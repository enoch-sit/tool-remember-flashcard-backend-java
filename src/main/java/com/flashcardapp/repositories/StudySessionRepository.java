package com.flashcardapp.repositories;

import com.flashcardapp.models.StudySession;
import com.flashcardapp.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    Optional<StudySession> findBySessionId(String sessionId);

    Page<StudySession> findByUser(User user, Pageable pageable);

    @Query("SELECT s FROM StudySession s WHERE s.user = :user AND s.startedAt >= :startDate ORDER BY s.startedAt DESC")
    Page<StudySession> findUserSessionsByDateRange(@Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            Pageable pageable);

    @Query("SELECT COUNT(s) FROM StudySession s WHERE s.user = :user AND s.completedAt IS NOT NULL AND s.completedAt >= :startDate")
    long countCompletedSessionsSince(@Param("user") User user, @Param("startDate") LocalDateTime startDate);
}