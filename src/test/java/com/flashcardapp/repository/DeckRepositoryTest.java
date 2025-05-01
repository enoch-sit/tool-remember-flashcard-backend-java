package com.flashcardapp.repository;

import com.flashcardapp.models.Deck;
import com.flashcardapp.models.ERole;
import com.flashcardapp.models.Role;
import com.flashcardapp.models.User;
import com.flashcardapp.repositories.DeckRepository;
import com.flashcardapp.repositories.RoleRepository;
import com.flashcardapp.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class DeckRepositoryTest {

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create role
        Role userRole = new Role(null, ERole.ROLE_USER);
        roleRepository.save(userRole);

        // Create test user
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password(new BCryptPasswordEncoder().encode("password"))
                .enabled(true)
                .emailVerified(true)
                .roles(roles)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(testUser);

        // Clear decks
        deckRepository.deleteAll();
    }

    @Test
    void findByUser_ShouldReturnUserDecks() {
        // Arrange
        Deck deck1 = new Deck();
        deck1.setName("Test Deck 1");
        deck1.setDescription("Test Description 1");
        deck1.setUser(testUser);
        deckRepository.save(deck1);

        Deck deck2 = new Deck();
        deck2.setName("Test Deck 2");
        deck2.setDescription("Test Description 2");
        deck2.setUser(testUser);
        deckRepository.save(deck2);

        // Create another user with their own deck
        User otherUser = User.builder()
                .username("otheruser")
                .email("other@example.com")
                .password("password")
                .enabled(true)
                .build();
        userRepository.save(otherUser);

        Deck otherDeck = new Deck();
        otherDeck.setName("Other Deck");
        otherDeck.setUser(otherUser);
        deckRepository.save(otherDeck);

        // Act
        List<Deck> foundDecks = deckRepository.findByUser(testUser);

        // Assert
        assertEquals(2, foundDecks.size());
        assertTrue(foundDecks.stream().anyMatch(d -> d.getName().equals("Test Deck 1")));
        assertTrue(foundDecks.stream().anyMatch(d -> d.getName().equals("Test Deck 2")));
        assertFalse(foundDecks.stream().anyMatch(d -> d.getName().equals("Other Deck")));
    }

    @Test
    void findByIdAndUser_ShouldReturnDeckWhenUserOwnsIt() {
        // Arrange
        Deck deck = new Deck();
        deck.setName("Test Deck");
        deck.setDescription("Test Description");
        deck.setUser(testUser);
        Deck savedDeck = deckRepository.save(deck);

        // Act
        Optional<Deck> found = deckRepository.findByIdAndUser(savedDeck.getId(), testUser);

        // Assert
        assertTrue(found.isPresent());
        assertEquals("Test Deck", found.get().getName());
    }

    @Test
    void findByIdAndUser_ShouldReturnEmptyWhenUserDoesNotOwnDeck() {
        // Arrange
        Deck deck = new Deck();
        deck.setName("Test Deck");
        deck.setDescription("Test Description");
        deck.setUser(testUser);
        Deck savedDeck = deckRepository.save(deck);

        User otherUser = User.builder()
                .username("otheruser")
                .email("other@example.com")
                .password("password")
                .enabled(true)
                .build();
        userRepository.save(otherUser);

        // Act
        Optional<Deck> found = deckRepository.findByIdAndUser(savedDeck.getId(), otherUser);

        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    void existsByIdAndUser_ShouldReturnTrueWhenUserOwnsDeck() {
        // Arrange
        Deck deck = new Deck();
        deck.setName("Test Deck");
        deck.setDescription("Test Description");
        deck.setUser(testUser);
        Deck savedDeck = deckRepository.save(deck);

        // Act
        boolean exists = deckRepository.existsByIdAndUser(savedDeck.getId(), testUser);

        // Assert
        assertTrue(exists);
    }

    @Test
    void existsByIdAndUser_ShouldReturnFalseWhenUserDoesNotOwnDeck() {
        // Arrange
        Deck deck = new Deck();
        deck.setName("Test Deck");
        deck.setDescription("Test Description");
        deck.setUser(testUser);
        Deck savedDeck = deckRepository.save(deck);

        User otherUser = User.builder()
                .username("otheruser")
                .email("other@example.com")
                .password("password")
                .enabled(true)
                .build();
        userRepository.save(otherUser);

        // Act
        boolean exists = deckRepository.existsByIdAndUser(savedDeck.getId(), otherUser);

        // Assert
        assertFalse(exists);
    }
}