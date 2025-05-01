package com.flashcardapp.controllers;

import com.flashcardapp.models.Deck;
import com.flashcardapp.models.StudySession;
import com.flashcardapp.models.User;
import com.flashcardapp.payload.response.MessageResponse;
import com.flashcardapp.repositories.DeckRepository;
import com.flashcardapp.repositories.StudySessionRepository;
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
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class StudySessionController {

    @Autowired
    private StudySessionRepository studySessionRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/study-sessions")
    @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getAllStudySessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startedAt,desc") String[] sort) {

        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String sortField = sort[0];
        String sortDirection = sort.length > 1 ? sort[1] : "desc";
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sortBy = Sort.by(direction, sortField);
        Pageable pageable = PageRequest.of(page, size, sortBy);

        Page<StudySession> sessions = studySessionRepository.findByUser(user, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("sessions", sessions.getContent());
        response.put("currentPage", sessions.getNumber());
        response.put("totalItems", sessions.getTotalElements());
        response.put("totalPages", sessions.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/study-sessions/{sessionId}")
    @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getStudySession(@PathVariable String sessionId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();

        StudySession studySession = studySessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Study session not found"));

        // Verify ownership
        if (!studySession.getUser().getId().equals(userDetails.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse("You don't have access to this study session"));
        }

        return ResponseEntity.ok(studySession);
    }

    @PostMapping("/decks/{deckId}/study-sessions")
    @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
    public ResponseEntity<?> startStudySession(@PathVariable Long deckId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Deck deck = deckRepository.findByIdAndUser(deckId, user)
                .orElseThrow(() -> new RuntimeException("Deck not found or you don't have access to this deck"));

        // Create a new study session
        StudySession studySession = StudySession.builder()
                .user(user)
                .deck(deck)
                .build();

        // Update deck's last studied time
        deck.setLastStudied(LocalDateTime.now());
        deckRepository.save(deck);

        StudySession savedSession = studySessionRepository.save(studySession);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedSession);
    }

    @PutMapping("/study-sessions/{sessionId}/complete")
    @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
    public ResponseEntity<?> completeStudySession(
            @PathVariable String sessionId,
            @Valid @RequestBody StudySession sessionDetails) {

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

        // Update session details
        studySession.setCardsReviewed(sessionDetails.getCardsReviewed());
        studySession.setCorrectResponses(sessionDetails.getCorrectResponses());
        studySession.setIncorrectResponses(sessionDetails.getIncorrectResponses());
        studySession.setTotalTimeSeconds(sessionDetails.getTotalTimeSeconds());
        studySession.setCompletedAt(LocalDateTime.now());

        StudySession updatedSession = studySessionRepository.save(studySession);

        return ResponseEntity.ok(updatedSession);
    }

    @GetMapping("/stats/study-activity")
    @PreAuthorize("hasRole('USER') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getStudyActivity(
            @RequestParam(defaultValue = "7") int days) {

        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        // Get user's recent study activity
        long completedSessions = studySessionRepository.countCompletedSessionsSince(user, startDate);

        Map<String, Object> stats = new HashMap<>();
        stats.put("period", days + " days");
        stats.put("completedSessions", completedSessions);

        // TODO: Add more detailed statistics if needed

        return ResponseEntity.ok(stats);
    }
}