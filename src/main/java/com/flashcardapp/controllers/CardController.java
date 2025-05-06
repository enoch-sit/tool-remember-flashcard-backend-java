package com.flashcardapp.controllers;

import com.flashcardapp.models.Card;
import com.flashcardapp.models.Deck;
import com.flashcardapp.models.User;
import com.flashcardapp.payload.response.MessageResponse;
import com.flashcardapp.repositories.CardRepository;
import com.flashcardapp.repositories.DeckRepository;
import com.flashcardapp.repositories.UserRepository;
import com.flashcardapp.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class CardController {

        @Autowired
        private CardRepository cardRepository;

        @Autowired
        private DeckRepository deckRepository;

        @Autowired
        private UserRepository userRepository;

        @GetMapping("/decks/{deckId}/cards")
        @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
        public ResponseEntity<?> getAllCardsByDeck(
                        @PathVariable Long deckId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(defaultValue = "id,asc") String[] sort) {

                UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                                .getPrincipal();
                User user = userRepository.findById(userDetails.getId())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Deck deck = deckRepository.findByIdAndUser(deckId, user)
                                .orElseThrow(() -> new RuntimeException(
                                                "Deck not found or you don't have access to this deck"));

                String sortField = sort[0];
                String sortDirection = sort.length > 1 ? sort[1] : "asc";
                Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC
                                : Sort.Direction.ASC;
                Sort sortBy = Sort.by(direction, sortField);
                Pageable pageable = PageRequest.of(page, size, sortBy);

                Page<Card> cards = cardRepository.findByDeck(deck, pageable);

                Map<String, Object> response = new HashMap<>();
                response.put("cards", cards.getContent());
                response.put("currentPage", cards.getNumber());
                response.put("totalItems", cards.getTotalElements());
                response.put("totalPages", cards.getTotalPages());

                return ResponseEntity.ok(response);
        }

        /**
         * Get cards in a deck with simplified response to avoid chunked encoding issues
         */
        @GetMapping("/decks/{deckId}/cards/simple")
        @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
        public ResponseEntity<?> getAllCardsByDeckSimplified(
                        @PathVariable Long deckId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {

                try {
                        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                                        .getAuthentication()
                                        .getPrincipal();
                        User user = userRepository.findById(userDetails.getId())
                                        .orElseThrow(() -> new RuntimeException("User not found"));

                        Deck deck = deckRepository.findByIdAndUser(deckId, user)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Deck not found or you don't have access to this deck"));

                        Pageable pageable = PageRequest.of(page, size);
                        Page<Card> cards = cardRepository.findByDeck(deck, pageable);

                        // Create a simplified response with just essential card data
                        List<Map<String, Object>> simplifiedCards = cards.getContent().stream()
                                        .map(card -> {
                                                Map<String, Object> simplifiedCard = new HashMap<>();
                                                simplifiedCard.put("id", card.getId());
                                                simplifiedCard.put("front", card.getFront());
                                                simplifiedCard.put("back", card.getBack());
                                                simplifiedCard.put("createdAt", card.getCreatedAt());
                                                return simplifiedCard;
                                        })
                                        .collect(Collectors.toList());

                        Map<String, Object> response = new HashMap<>();
                        response.put("cards", simplifiedCards);
                        response.put("currentPage", cards.getNumber());
                        response.put("totalItems", cards.getTotalElements());
                        response.put("totalPages", cards.getTotalPages());

                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        Map<String, String> errorResponse = new HashMap<>();
                        errorResponse.put("message", "Error retrieving cards: " + e.getMessage());
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                }
        }

        @GetMapping("/decks/{deckId}/cards/{id}")
        @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
        public ResponseEntity<?> getCardById(@PathVariable Long deckId, @PathVariable Long id) {
                UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                                .getPrincipal();
                User user = userRepository.findById(userDetails.getId())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                deckRepository.findByIdAndUser(deckId, user)
                                .orElseThrow(() -> new RuntimeException(
                                                "Deck not found or you don't have access to this deck"));

                Card card = cardRepository.findByIdAndDeckId(id, deckId)
                                .orElseThrow(() -> new RuntimeException("Card not found"));

                return ResponseEntity.ok(card);
        }

        /**
         * Get card details with simplified response to avoid chunked encoding issues
         */
        @GetMapping("/decks/{deckId}/cards/{id}/simple")
        @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
        public ResponseEntity<?> getCardByIdSimplified(@PathVariable Long deckId, @PathVariable Long id) {
                try {
                        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                                        .getAuthentication()
                                        .getPrincipal();
                        User user = userRepository.findById(userDetails.getId())
                                        .orElseThrow(() -> new RuntimeException("User not found"));

                        deckRepository.findByIdAndUser(deckId, user)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Deck not found or you don't have access to this deck"));

                        Card card = cardRepository.findByIdAndDeckId(id, deckId)
                                        .orElseThrow(() -> new RuntimeException("Card not found"));

                        // Create a simplified response with just essential card data
                        Map<String, Object> simplifiedCard = new HashMap<>();
                        simplifiedCard.put("id", card.getId());
                        simplifiedCard.put("front", card.getFront());
                        simplifiedCard.put("back", card.getBack());
                        simplifiedCard.put("notes", card.getNotes());
                        simplifiedCard.put("createdAt", card.getCreatedAt());
                        simplifiedCard.put("updatedAt", card.getUpdatedAt());

                        return ResponseEntity.ok(simplifiedCard);
                } catch (Exception e) {
                        Map<String, String> errorResponse = new HashMap<>();
                        errorResponse.put("message", "Error retrieving card: " + e.getMessage());
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                }
        }

        @GetMapping("/decks/{deckId}/review-cards")
        @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
        public ResponseEntity<?> getCardsForReview(
                        @PathVariable Long deckId,
                        @RequestParam(defaultValue = "10") int limit) {

                UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                                .getPrincipal();
                User user = userRepository.findById(userDetails.getId())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Deck deck = deckRepository.findByIdAndUser(deckId, user)
                                .orElseThrow(() -> new RuntimeException(
                                                "Deck not found or you don't have access to this deck"));

                LocalDateTime now = LocalDateTime.now();

                Pageable pageable = PageRequest.of(0, limit);
                List<Card> cards = cardRepository.findCardsForReview(deck, now, pageable);
                Long totalCards = cardRepository.countCardsForReview(deck, now);

                Map<String, Object> response = new HashMap<>();
                response.put("cards", cards);
                response.put("totalDue", totalCards);

                return ResponseEntity.ok(response);
        }

        @PostMapping("/decks/{deckId}/cards")
        @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
        public ResponseEntity<?> createCard(@PathVariable Long deckId, @Valid @RequestBody Card card) {
                UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                                .getPrincipal();
                User user = userRepository.findById(userDetails.getId())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Deck deck = deckRepository.findByIdAndUser(deckId, user)
                                .orElseThrow(() -> new RuntimeException(
                                                "Deck not found or you don't have access to this deck"));

                card.setDeck(deck);
                Card savedCard = cardRepository.save(card);

                return ResponseEntity.status(HttpStatus.CREATED).body(savedCard);
        }

        /**
         * Alternative card creation endpoint that returns a lightweight response
         * to avoid chunked encoding issues with some clients
         */
        @PostMapping("/decks/{deckId}/cards/simple")
        @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
        public ResponseEntity<?> createCardSimple(@PathVariable Long deckId, @Valid @RequestBody Card card) {
                try {
                        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                                        .getAuthentication()
                                        .getPrincipal();
                        User user = userRepository.findById(userDetails.getId())
                                        .orElseThrow(() -> new RuntimeException("User not found"));

                        Deck deck = deckRepository.findByIdAndUser(deckId, user)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Deck not found or you don't have access to this deck"));

                        card.setDeck(deck);
                        Card savedCard = cardRepository.save(card);

                        // Create a simplified response with just the essential fields
                        Map<String, Object> response = new HashMap<>();
                        response.put("id", savedCard.getId());
                        response.put("front", savedCard.getFront());
                        response.put("back", savedCard.getBack());
                        response.put("createdAt", savedCard.getCreatedAt());

                        // Return a simple response to avoid chunked encoding issues
                        return ResponseEntity.status(HttpStatus.CREATED).body(response);
                } catch (Exception e) {
                        // Add better error handling
                        Map<String, String> errorResponse = new HashMap<>();
                        errorResponse.put("message", "Error creating card: " + e.getMessage());
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                }
        }

        @PutMapping("/decks/{deckId}/cards/{id}")
        @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
        public ResponseEntity<?> updateCard(@PathVariable Long deckId, @PathVariable Long id,
                        @Valid @RequestBody Card cardDetails) {
                UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                                .getPrincipal();
                User user = userRepository.findById(userDetails.getId())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                deckRepository.findByIdAndUser(deckId, user)
                                .orElseThrow(() -> new RuntimeException(
                                                "Deck not found or you don't have access to this deck"));

                Card card = cardRepository.findByIdAndDeckId(id, deckId)
                                .orElseThrow(() -> new RuntimeException("Card not found"));

                card.setFront(cardDetails.getFront());
                card.setBack(cardDetails.getBack());
                card.setNotes(cardDetails.getNotes());
                card.setUpdatedAt(LocalDateTime.now());

                Card updatedCard = cardRepository.save(card);

                return ResponseEntity.ok(updatedCard);
        }

        @DeleteMapping("/decks/{deckId}/cards/{id}")
        @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
        public ResponseEntity<?> deleteCard(@PathVariable Long deckId, @PathVariable Long id) {
                UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                                .getPrincipal();
                User user = userRepository.findById(userDetails.getId())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                deckRepository.findByIdAndUser(deckId, user)
                                .orElseThrow(() -> new RuntimeException(
                                                "Deck not found or you don't have access to this deck"));

                Card card = cardRepository.findByIdAndDeckId(id, deckId)
                                .orElseThrow(() -> new RuntimeException("Card not found"));

                cardRepository.delete(card);

                return ResponseEntity.ok(new MessageResponse("Card deleted successfully"));
        }
}