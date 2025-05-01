package com.flashcardapp.controllers;

import com.flashcardapp.models.Card;
import com.flashcardapp.models.CardReview;
import com.flashcardapp.models.StudySession;
import com.flashcardapp.models.User;
import com.flashcardapp.payload.response.MessageResponse;
import com.flashcardapp.repositories.CardRepository;
import com.flashcardapp.repositories.CardReviewRepository;
import com.flashcardapp.repositories.StudySessionRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class CardReviewController {

    @Autowired
    private CardReviewRepository cardReviewRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private StudySessionRepository studySessionRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/study-sessions/{sessionId}/reviews")
    @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
    public ResponseEntity<?> submitCardReview(
            @PathVariable String sessionId,
            @Valid @RequestBody CardReview reviewDetails) {

        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        StudySession studySession = studySessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Study session not found"));

        // Verify ownership
        if (!studySession.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("You don't have access to this study session"));
        }

        Card card = cardRepository.findById(reviewDetails.getCard().getId())
                .orElseThrow(() -> new RuntimeException("Card not found"));

        // Verify that the card belongs to the deck being studied
        if (!card.getDeck().getId().equals(studySession.getDeck().getId())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Card does not belong to the deck being studied"));
        }

        // Create the review record
        CardReview cardReview = new CardReview();
        cardReview.setCard(card);
        cardReview.setStudySession(studySession);
        cardReview.setResult(reviewDetails.getResult());
        cardReview.setTimeSpentSeconds(reviewDetails.getTimeSpentSeconds());
        cardReview.setPreviousDifficulty(card.getDifficulty());

        // Calculate new difficulty based on review result (0 = incorrect, 1-5 for
        // correct with varying difficulty)
        int newDifficulty;
        if (reviewDetails.getResult() == 0) {
            // Incorrect answer increases difficulty
            newDifficulty = Math.min(5, card.getDifficulty() + 2);
        } else {
            // Correct answer decreases difficulty depending on rating (1-5)
            // Lower rating = higher difficulty
            newDifficulty = Math.max(0, card.getDifficulty() - (reviewDetails.getResult() - 3));
        }
        cardReview.setNewDifficulty(newDifficulty);

        // Calculate next review date based on the difficulty
        LocalDateTime nextReview;
        switch (newDifficulty) {
            case 0:
                nextReview = LocalDateTime.now().plusHours(6); // Easiest: review after 6 hours
                break;
            case 1:
                nextReview = LocalDateTime.now().plusDays(1); // Review after 1 day
                break;
            case 2:
                nextReview = LocalDateTime.now().plusDays(3); // Review after 3 days
                break;
            case 3:
                nextReview = LocalDateTime.now().plusDays(7); // Review after 1 week
                break;
            case 4:
                nextReview = LocalDateTime.now().plusDays(14); // Review after 2 weeks
                break;
            case 5:
                nextReview = LocalDateTime.now().plusDays(30); // Hardest: review after 1 month
                break;
            default:
                nextReview = LocalDateTime.now().plusDays(1); // Default: review after 1 day
        }
        cardReview.setNextReviewDate(nextReview);

        // Save the review
        CardReview savedReview = cardReviewRepository.save(cardReview);

        // Update the card's difficulty and next review date
        card.setDifficulty(newDifficulty);
        card.setNextReviewDate(nextReview);
        card.setReviewCount(card.getReviewCount() + 1);
        cardRepository.save(card);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedReview);
    }

    @GetMapping("/cards/{cardId}/reviews")
    @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getCardReviewHistory(@PathVariable Long cardId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        // Verify ownership by checking if the card's deck belongs to the current user
        if (!card.getDeck().getUser().getId().equals(userDetails.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("You don't have access to this card"));
        }

        List<CardReview> reviews = cardReviewRepository.findByCard(card);

        // Get statistics for the card
        Long correctCount = cardReviewRepository.countCorrectReviews(card);
        Long incorrectCount = cardReviewRepository.countIncorrectReviews(card);
        Double averageTime = cardReviewRepository.getAverageTimeSpent(card);

        Map<String, Object> response = new HashMap<>();
        response.put("cardId", cardId);
        response.put("reviews", reviews);
        response.put("statistics", Map.of(
                "totalReviews", reviews.size(),
                "correctCount", correctCount,
                "incorrectCount", incorrectCount,
                "averageTimeSeconds", averageTime != null ? averageTime : 0,
                "successRate", reviews.size() > 0 ? (double) correctCount / reviews.size() * 100 : 0));

        return ResponseEntity.ok(response);
    }
}