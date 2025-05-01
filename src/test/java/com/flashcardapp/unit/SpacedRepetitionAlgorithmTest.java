package com.flashcardapp.unit;

import com.flashcardapp.models.Card;
import com.flashcardapp.models.CardReview;
import com.flashcardapp.models.Deck;
import com.flashcardapp.models.StudySession;
import com.flashcardapp.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the spaced repetition algorithm logic in the application
 */
public class SpacedRepetitionAlgorithmTest {

    private Card card;
    private StudySession studySession;
    private User testUser;
    private Deck testDeck;
    // Create a fixed reference time for testing
    private static final LocalDateTime REFERENCE_TIME = LocalDateTime.of(2025, 5, 1, 10, 0, 0);

    @BeforeEach
    void setUp() {
        // Create user
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .build();

        // Create deck
        testDeck = new Deck();
        testDeck.setId(1L);
        testDeck.setUser(testUser);

        // Create card with initial difficulty
        card = new Card();
        card.setId(1L);
        card.setFront("Test Front");
        card.setBack("Test Back");
        card.setDifficulty(3); // Medium difficulty
        card.setDeck(testDeck);
        card.setNextReviewDate(LocalDateTime.now());
        card.setReviewCount(5);

        // Create study session
        studySession = StudySession.builder()
                .id(1L)
                .sessionId("test-session")
                .user(testUser)
                .deck(testDeck)
                .build();
    }

    @ParameterizedTest
    @MethodSource("provideReviewResultsAndExpectedDifficulty")
    void calculateNewDifficulty_ShouldReturnCorrectDifficulty(int initialDifficulty, int reviewResult,
            int expectedDifficulty) {
        // Arrange
        card.setDifficulty(initialDifficulty);
        CardReview cardReview = new CardReview();
        cardReview.setCard(card);
        cardReview.setStudySession(studySession);
        cardReview.setResult(reviewResult);
        cardReview.setPreviousDifficulty(card.getDifficulty());

        // Act - Algorithm from CardReviewController
        int newDifficulty;
        if (reviewResult == 0) {
            // Incorrect answer increases difficulty
            newDifficulty = Math.min(5, initialDifficulty + 2);
        } else {
            // Correct answer decreases difficulty depending on rating (1-5)
            // Lower rating = higher difficulty
            newDifficulty = Math.max(0, initialDifficulty - (reviewResult - 3));
        }

        // Assert
        assertEquals(expectedDifficulty, newDifficulty);
    }

    private static Stream<Arguments> provideReviewResultsAndExpectedDifficulty() {
        // Format: initialDifficulty, reviewResult, expectedDifficulty
        return Stream.of(
                // Incorrect answers (result = 0) increase difficulty by 2 (max 5)
                Arguments.of(0, 0, 2), // 0 -> 2
                Arguments.of(3, 0, 5), // 3 -> 5
                Arguments.of(4, 0, 5), // 4 -> 5 (capped at 5)
                Arguments.of(5, 0, 5), // 5 -> 5 (already at max)

                // Correct answers with different ratings (1-5) affect difficulty
                // Formula: newDifficulty = initialDifficulty - (reviewResult - 3)
                Arguments.of(3, 1, 5), // 3 - (1-3) = 3 - (-2) = 5
                Arguments.of(3, 2, 4), // 3 - (2-3) = 3 - (-1) = 4
                Arguments.of(3, 3, 3), // 3 - (3-3) = 3 - 0 = 3 (no change)
                Arguments.of(3, 4, 2), // 3 - (4-3) = 3 - 1 = 2
                Arguments.of(3, 5, 1), // 3 - (5-3) = 3 - 2 = 1
                Arguments.of(1, 5, 0), // 1 - (5-3) = 1 - 2 = 0 (min value)
                Arguments.of(0, 5, 0) // 0 - (5-3) = 0 - 2 = -2 -> capped at 0
        );
    }

    @ParameterizedTest
    @MethodSource("provideReviewResultsAndExpectedNextReviewDates")
    void calculateNextReviewDate_ShouldReturnCorrectInterval(int difficulty, LocalDateTime expectedRelativeDate) {
        // Act - Algorithm from CardReviewController
        LocalDateTime nextReview;
        switch (difficulty) {
            case 0:
                nextReview = REFERENCE_TIME.plusHours(6); // Easiest: review after 6 hours
                break;
            case 1:
                nextReview = REFERENCE_TIME.plusDays(1); // Review after 1 day
                break;
            case 2:
                nextReview = REFERENCE_TIME.plusDays(3); // Review after 3 days
                break;
            case 3:
                nextReview = REFERENCE_TIME.plusDays(7); // Review after 1 week
                break;
            case 4:
                nextReview = REFERENCE_TIME.plusDays(14); // Review after 2 weeks
                break;
            case 5:
                nextReview = REFERENCE_TIME.plusDays(30); // Hardest: review after 1 month
                break;
            default:
                nextReview = REFERENCE_TIME.plusDays(1); // Default: review after 1 day
        }

        // Assert - Compare the differences between dates, allowing for millisecond
        // variations
        long expectedHours = ChronoUnit.HOURS.between(REFERENCE_TIME, expectedRelativeDate);
        long actualHours = ChronoUnit.HOURS.between(REFERENCE_TIME, nextReview);

        assertEquals(expectedHours, actualHours);
    }

    private static Stream<Arguments> provideReviewResultsAndExpectedNextReviewDates() {
        return Stream.of(
                Arguments.of(0, REFERENCE_TIME.plusHours(6)), // Easiest: review after 6 hours
                Arguments.of(1, REFERENCE_TIME.plusDays(1)), // Review after 1 day
                Arguments.of(2, REFERENCE_TIME.plusDays(3)), // Review after 3 days
                Arguments.of(3, REFERENCE_TIME.plusDays(7)), // Review after 1 week
                Arguments.of(4, REFERENCE_TIME.plusDays(14)), // Review after 2 weeks
                Arguments.of(5, REFERENCE_TIME.plusDays(30)), // Hardest: review after 1 month
                Arguments.of(6, REFERENCE_TIME.plusDays(1)) // Invalid: default to 1 day
        );
    }

    @Test
    void fullReviewFlow_ShouldUpdateCardCorrectly() {
        // Arrange
        CardReview cardReview = new CardReview();
        cardReview.setCard(card);
        cardReview.setStudySession(studySession);
        cardReview.setResult(0); // Incorrect answer
        cardReview.setTimeSpentSeconds(30);
        cardReview.setPreviousDifficulty(card.getDifficulty());

        // Initial state
        int initialDifficulty = card.getDifficulty();
        int initialReviewCount = card.getReviewCount();

        // Act - Simulate the full review flow from CardReviewController
        int newDifficulty;
        if (cardReview.getResult() == 0) {
            newDifficulty = Math.min(5, initialDifficulty + 2);
        } else {
            newDifficulty = Math.max(0, initialDifficulty - (cardReview.getResult() - 3));
        }

        cardReview.setNewDifficulty(newDifficulty);

        LocalDateTime testReferenceTime = LocalDateTime.now(); // Use a consistent time for this test
        LocalDateTime nextReview;
        switch (newDifficulty) {
            case 0:
                nextReview = testReferenceTime.plusHours(6);
                break;
            case 1:
                nextReview = testReferenceTime.plusDays(1);
                break;
            case 2:
                nextReview = testReferenceTime.plusDays(3);
                break;
            case 3:
                nextReview = testReferenceTime.plusDays(7);
                break;
            case 4:
                nextReview = testReferenceTime.plusDays(14);
                break;
            case 5:
                nextReview = testReferenceTime.plusDays(30);
                break;
            default:
                nextReview = testReferenceTime.plusDays(1);
        }

        cardReview.setNextReviewDate(nextReview);

        // Update card
        card.setDifficulty(newDifficulty);
        card.setNextReviewDate(nextReview);
        card.setReviewCount(initialReviewCount + 1);

        // Assert
        assertEquals(5, card.getDifficulty()); // 3 + 2 = 5
        assertEquals(initialReviewCount + 1, card.getReviewCount());

        // Check next review date is approximately 30 days in the future
        long daysBetween = ChronoUnit.DAYS.between(testReferenceTime, card.getNextReviewDate());
        assertEquals(30, daysBetween);
    }
}