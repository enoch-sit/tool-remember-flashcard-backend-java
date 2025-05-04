package com.flashcardapp.services;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender emailSender;

    @Value("${spring.mail.properties.mail.from:noreply@flashcardapp.com}")
    private String fromEmail;

    @Value("${app.email.verification-url:http://localhost:3000/api/auth/verify-email?token=}")
    private String verificationBaseUrl;

    /**
     * Send verification email to the user
     * 
     * @param to    recipient email address
     * @param token verification token
     */
    public void sendVerificationEmail(String to, String token) {
        logger.info("Starting to send verification email to: {} with token: {}", to, token);

        // Log mail configuration
        logger.info("Mail configuration: fromEmail={}, verificationBaseUrl={}",
                fromEmail, verificationBaseUrl);

        try {
            logger.info("Creating MIME message");
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            logger.info("Setting email headers");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Verify Your Email Address");

            String verificationLink = verificationBaseUrl + token;
            String htmlContent = "<h1>Welcome to Flashcard App</h1>" +
                    "<p>Thank you for registering with us. Please click the link below to verify your email address:</p>"
                    +
                    "<p><a href=\"" + verificationLink + "\">Verify My Email</a></p>" +
                    "<p>Or copy and paste this token: " + token + "</p>" +
                    "<p>This link will expire in 24 hours.</p>";

            logger.info("Setting email content");
            helper.setText(htmlContent, true);

            logger.info("About to send verification email to: {}", to);
            emailSender.send(message);
            logger.info("Verification email sent successfully to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send verification email to {}: {}", to, e.getMessage());
            logger.error("Stack trace:", e);
            throw new RuntimeException("Failed to send verification email", e);
        } catch (Exception e) {
            logger.error("Unexpected error while sending verification email to {}: {}", to, e.getMessage());
            logger.error("Stack trace:", e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    /**
     * Send password reset email to the user
     * 
     * @param to    recipient email address
     * @param token reset token
     */
    public void sendPasswordResetEmail(String to, String token) {
        logger.info("Starting to send password reset email to: {} with token: {}", to, token);

        try {
            logger.info("Creating MIME message");
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            logger.info("Setting email headers");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Reset Your Password");

            String resetLink = "http://localhost:3000/reset-password?token=" + token;
            String htmlContent = "<h1>Password Reset Request</h1>" +
                    "<p>You requested to reset your password. Click the link below to create a new password:</p>" +
                    "<p><a href=\"" + resetLink + "\">Reset My Password</a></p>" +
                    "<p>Or copy and paste this token: " + token + "</p>" +
                    "<p>This link will expire in 24 hours. If you did not request a password reset, please ignore this email.</p>";

            logger.info("Setting email content");
            helper.setText(htmlContent, true);

            logger.info("About to send password reset email to: {}", to);
            emailSender.send(message);
            logger.info("Password reset email sent successfully to: {}", to);
        } catch (MessagingException e) {
            logger.error("Failed to send password reset email to {}: {}", to, e.getMessage());
            logger.error("Stack trace:", e);
            throw new RuntimeException("Failed to send password reset email", e);
        } catch (Exception e) {
            logger.error("Unexpected error while sending password reset email to {}: {}", to, e.getMessage());
            logger.error("Stack trace:", e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}