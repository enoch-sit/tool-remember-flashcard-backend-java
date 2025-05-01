package com.flashcardapp.repositories;

import com.flashcardapp.models.Card;
import com.flashcardapp.models.Deck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByDeck(Deck deck);

    Page<Card> findByDeck(Deck deck, Pageable pageable);

    Optional<Card> findByIdAndDeckId(Long id, Long deckId);

    @Query("SELECT c FROM Card c WHERE c.deck = :deck AND c.nextReviewDate <= :now ORDER BY c.nextReviewDate ASC")
    List<Card> findCardsForReview(@Param("deck") Deck deck, @Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Card c WHERE c.deck = :deck AND c.nextReviewDate <= :now")
    Long countCardsForReview(@Param("deck") Deck deck, @Param("now") LocalDateTime now);
}