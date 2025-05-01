package com.flashcardapp.repositories;

import com.flashcardapp.models.Deck;
import com.flashcardapp.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeckRepository extends JpaRepository<Deck, Long> {
    List<Deck> findByUser(User user);

    Optional<Deck> findByIdAndUser(Long id, User user);

    boolean existsByIdAndUser(Long id, User user);
}