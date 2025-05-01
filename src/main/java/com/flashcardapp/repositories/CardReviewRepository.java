package com.flashcardapp.repositories;

import com.flashcardapp.models.Card;
import com.flashcardapp.models.CardReview;
import com.flashcardapp.models.StudySession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CardReviewRepository extends JpaRepository<CardReview, Long> {
    List<CardReview> findByCard(Card card);

    List<CardReview> findByStudySession(StudySession studySession);

    @Query("SELECT cr FROM CardReview cr WHERE cr.card = :card ORDER BY cr.reviewedAt DESC")
    List<CardReview> findRecentReviews(@Param("card") Card card, Pageable pageable);

    @Query("SELECT COUNT(cr) FROM CardReview cr WHERE cr.card = :card AND cr.result > 0")
    Long countCorrectReviews(@Param("card") Card card);

    @Query("SELECT COUNT(cr) FROM CardReview cr WHERE cr.card = :card AND cr.result = 0")
    Long countIncorrectReviews(@Param("card") Card card);

    @Query("SELECT AVG(cr.timeSpentSeconds) FROM CardReview cr WHERE cr.card = :card")
    Double getAverageTimeSpent(@Param("card") Card card);

    @Query("SELECT cr FROM CardReview cr WHERE cr.card.deck.user.id = :userId AND cr.reviewedAt >= :startDate")
    List<CardReview> findUserReviewsInPeriod(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);
}
