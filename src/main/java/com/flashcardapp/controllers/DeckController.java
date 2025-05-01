package com.flashcardapp.controllers;

import com.flashcardapp.models.Deck;
import com.flashcardapp.models.User;
import com.flashcardapp.payload.response.MessageResponse;
import com.flashcardapp.repositories.DeckRepository;
import com.flashcardapp.repositories.UserRepository;
import com.flashcardapp.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/decks")
public class DeckController {

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
    public ResponseEntity<List<Deck>> getAllDecks() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Deck> decks = deckRepository.findByUser(user);

        return ResponseEntity.ok(decks);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getDeckById(@PathVariable Long id) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();

        Deck deck = deckRepository.findByIdAndUser(id,
                userRepository.findById(userDetails.getId()).orElseThrow(() -> new RuntimeException("User not found")))
                .orElseThrow(() -> new RuntimeException("Deck not found"));

        return ResponseEntity.ok(deck);
    }

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
    public ResponseEntity<?> createDeck(@Valid @RequestBody Deck deck) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        deck.setUser(user);
        deck.setCreatedAt(LocalDateTime.now());
        deck.setUpdatedAt(LocalDateTime.now());

        Deck savedDeck = deckRepository.save(deck);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedDeck);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
    public ResponseEntity<?> updateDeck(@PathVariable Long id, @Valid @RequestBody Deck deckDetails) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Deck deck = deckRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Deck not found or you don't have access to this deck"));

        deck.setName(deckDetails.getName());
        deck.setDescription(deckDetails.getDescription());
        deck.setUpdatedAt(LocalDateTime.now());

        Deck updatedDeck = deckRepository.save(deck);

        return ResponseEntity.ok(updatedDeck);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteDeck(@PathVariable Long id) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!deckRepository.existsByIdAndUser(id, user)) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Deck not found or you don't have access to this deck"));
        }

        deckRepository.deleteById(id);

        return ResponseEntity.ok(new MessageResponse("Deck deleted successfully"));
    }
}