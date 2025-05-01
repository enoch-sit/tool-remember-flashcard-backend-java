package com.flashcardapp.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "study_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudySession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    @OneToMany(mappedBy = "studySession", cascade = CascadeType.ALL)
    private List<CardReview> cardReviews = new ArrayList<>();

    private Integer cardsReviewed;

    private Integer correctResponses;

    private Integer incorrectResponses;

    private Integer totalTimeSeconds;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        sessionId = UUID.randomUUID().toString();
        startedAt = LocalDateTime.now();
        cardsReviewed = 0;
        correctResponses = 0;
        incorrectResponses = 0;
        totalTimeSeconds = 0;
    }
}