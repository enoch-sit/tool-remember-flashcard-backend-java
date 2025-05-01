package com.flashcardapp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashcardapp.models.Deck;
import com.flashcardapp.models.ERole;
import com.flashcardapp.models.Role;
import com.flashcardapp.models.User;
import com.flashcardapp.repositories.DeckRepository;
import com.flashcardapp.repositories.RoleRepository;
import com.flashcardapp.repositories.UserRepository;
import com.flashcardapp.security.jwt.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class DeckControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private DeckRepository deckRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private JwtUtils jwtUtils;

        @Autowired
        private AuthenticationManager authenticationManager;

        private String accessToken;
        private User testUser;

        @BeforeEach
        void setUp() {
                // Clear database
                deckRepository.deleteAll();
                userRepository.deleteAll();

                // Set JWT secret via reflection to ensure it's a valid base64 string
                String secretKey = Base64.getEncoder().encodeToString(
                                "TEST_JWT_SECRET_KEY_THAT_IS_SUFFICIENTLY_LONG_FOR_TESTING_PURPOSES_ONLY".getBytes());
                ReflectionTestUtils.setField(jwtUtils, "jwtSecret", secretKey);

                // Create role if not exists
                Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                                .orElseGet(() -> roleRepository.save(new Role(null, ERole.ROLE_USER)));

                // Create test user
                Set<Role> roles = new HashSet<>();
                roles.add(userRole);

                testUser = User.builder()
                                .username("testuser")
                                .email("test@example.com")
                                .password(passwordEncoder.encode("password"))
                                .enabled(true)
                                .emailVerified(true)
                                .roles(roles)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                userRepository.save(testUser);

                // Generate JWT token
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken("testuser", "password"));
                accessToken = jwtUtils.generateAccessToken(authentication);
        }

        @Test
        void getAllDecks_ShouldReturnUserDecks() throws Exception {
                // Arrange
                Deck deck1 = new Deck();
                deck1.setName("Test Deck 1");
                deck1.setDescription("Test Description 1");
                deck1.setUser(testUser);
                deckRepository.save(deck1);

                // Act & Assert
                mockMvc.perform(get("/api/decks")
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].name", is("Test Deck 1")));
        }

        @Test
        void createDeck_ShouldReturnCreatedDeck() throws Exception {
                // Arrange
                Deck newDeck = new Deck();
                newDeck.setName("New Test Deck");
                newDeck.setDescription("New Description");

                // Act
                ResultActions result = mockMvc.perform(post("/api/decks")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newDeck)));

                // Assert
                result.andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name", is("New Test Deck")))
                                .andExpect(jsonPath("$.description", is("New Description")));
        }

        @Test
        void getDeckById_ShouldReturnDeck() throws Exception {
                // Arrange
                Deck deck = new Deck();
                deck.setName("Test Deck");
                deck.setDescription("Test Description");
                deck.setUser(testUser);
                Deck savedDeck = deckRepository.save(deck);

                // Act & Assert
                mockMvc.perform(get("/api/decks/" + savedDeck.getId())
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name", is("Test Deck")));
        }

        @Test
        void updateDeck_ShouldUpdateDeck() throws Exception {
                // Arrange
                Deck deck = new Deck();
                deck.setName("Original Name");
                deck.setDescription("Original Description");
                deck.setUser(testUser);
                Deck savedDeck = deckRepository.save(deck);

                Deck updatedDeck = new Deck();
                updatedDeck.setName("Updated Name");
                updatedDeck.setDescription("Updated Description");

                // Act & Assert
                mockMvc.perform(put("/api/decks/" + savedDeck.getId())
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updatedDeck)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name", is("Updated Name")))
                                .andExpect(jsonPath("$.description", is("Updated Description")));
        }

        @Test
        void deleteDeck_ShouldDeleteDeck() throws Exception {
                // Arrange
                Deck deck = new Deck();
                deck.setName("Test Deck to Delete");
                deck.setDescription("Test Description");
                deck.setUser(testUser);
                Deck savedDeck = deckRepository.save(deck);
                Long deckId = savedDeck.getId();

                // Act & Assert - Delete the deck
                mockMvc.perform(delete("/api/decks/" + deckId)
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message", is("Deck deleted successfully")));

                // Verify the deck is deleted by checking the repository directly
                // This is more reliable than trying to make another API call
                assert !deckRepository.existsById(deckId) : "Deck should have been deleted";
        }
}