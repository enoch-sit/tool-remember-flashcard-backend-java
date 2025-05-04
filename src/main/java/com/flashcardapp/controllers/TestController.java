package com.flashcardapp.controllers;

import com.flashcardapp.payload.response.MessageResponse;
import com.flashcardapp.services.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for testing purposes only.
 * Should be disabled in production.
 */
@RestController
@RequestMapping("/api/test")
public class TestController {
    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private EmailService emailService;

    /**
     * Test endpoint to verify email service is working
     * 
     * @param email The email address to send the test email to
     * @return A response indicating whether the email was sent successfully
     */
    @GetMapping("/send-test-email")
    public ResponseEntity<?> sendTestEmail(@RequestParam String email) {
        logger.info("Received request to send test email to: {}", email);

        try {
            // Generate a test token
            String testToken = "test-token-" + System.currentTimeMillis();

            // Try to send a test email
            logger.info("Attempting to send verification test email to: {}", email);
            emailService.sendVerificationEmail(email, testToken);
            logger.info("Test verification email sent successfully to: {}", email);

            // Also test password reset email
            logger.info("Attempting to send password reset test email to: {}", email);
            emailService.sendPasswordResetEmail(email, testToken);
            logger.info("Test password reset email sent successfully to: {}", email);

            return ResponseEntity.ok(new MessageResponse("Test emails sent successfully to: " + email));
        } catch (Exception e) {
            logger.error("Failed to send test email: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new MessageResponse("Failed to send test email: " + e.getMessage()));
        }
    }
}