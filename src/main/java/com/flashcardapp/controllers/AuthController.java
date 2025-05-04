package com.flashcardapp.controllers;

import com.flashcardapp.models.ERole;
import com.flashcardapp.models.Role;
import com.flashcardapp.models.User;
import com.flashcardapp.payload.request.LoginRequest;
import com.flashcardapp.payload.request.SignupRequest;
import com.flashcardapp.payload.request.VerifyEmailRequest;
import com.flashcardapp.payload.request.ResendVerificationRequest;
import com.flashcardapp.payload.request.RefreshTokenRequest;
import com.flashcardapp.payload.request.LogoutRequest;
import com.flashcardapp.payload.request.ForgotPasswordRequest;
import com.flashcardapp.payload.request.ResetPasswordRequest;
import com.flashcardapp.payload.response.JwtResponse;
import com.flashcardapp.payload.response.MessageResponse;
import com.flashcardapp.repositories.RoleRepository;
import com.flashcardapp.repositories.UserRepository;
import com.flashcardapp.security.jwt.JwtUtils;
import com.flashcardapp.security.services.UserDetailsImpl;
import com.flashcardapp.services.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    EmailService emailService;

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // Create new user account
        User user = User.builder()
                .username(signUpRequest.getUsername())
                .email(signUpRequest.getEmail())
                .password(encoder.encode(signUpRequest.getPassword()))
                .enabled(true)
                .emailVerified(false)
                .verificationToken(UUID.randomUUID().toString())
                .verificationTokenExpiry(LocalDateTime.now().plusDays(1))
                .build();

        Set<String> strRoles = signUpRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                        break;
                    case "supervisor":
                        Role modRole = roleRepository.findByName(ERole.ROLE_SUPERVISOR)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(modRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        // Send verification email
        try {
            emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getVerificationToken());
            logger.info("Verification email sent to: {}", savedUser.getEmail());
        } catch (Exception e) {
            logger.error("Failed to send verification email: {}", e.getMessage());
            // Continue execution even if email sending fails
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully. Verification email has been sent.");
        response.put("userId", savedUser.getId().toString());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Check if email is verified
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEmailVerified()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse(
                            "Error: Email not verified. Please check your email for verification link."));
        }

        String accessToken = jwtUtils.generateAccessToken(authentication);
        String refreshToken = jwtUtils.generateRefreshToken(authentication);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        // Update last login date
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", "Login successful");
        responseBody.put("accessToken", accessToken);
        responseBody.put("refreshToken", refreshToken);
        responseBody.put("user", Map.of(
                "id", userDetails.getId(),
                "username", userDetails.getUsername(),
                "email", userDetails.getEmail(),
                "isVerified", user.isEmailVerified(),
                "role", roles.get(0)));

        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        if (request.getToken() == null || request.getToken().isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Token is required"));
        }

        try {
            User user = userRepository.findByVerificationToken(request.getToken())
                    .orElseThrow(() -> new RuntimeException("Invalid verification token"));

            if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body(new MessageResponse("Token expired"));
            }

            user.setEmailVerified(true);
            user.setVerificationToken(null);
            user.setVerificationTokenExpiry(null);
            userRepository.save(user);

            return ResponseEntity.ok(new MessageResponse("Email verified successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Email verification failed"));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Email is required"));
        }

        try {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.isEmailVerified()) {
                return ResponseEntity.badRequest().body(new MessageResponse("Email is already verified"));
            }

            // Generate new verification token
            user.setVerificationToken(UUID.randomUUID().toString());
            user.setVerificationTokenExpiry(LocalDateTime.now().plusDays(1));
            userRepository.save(user);

            // Send new verification email
            try {
                emailService.sendVerificationEmail(user.getEmail(), user.getVerificationToken());
                logger.info("Verification email resent to: {}", user.getEmail());
            } catch (Exception e) {
                logger.error("Failed to resend verification email: {}", e.getMessage());
                // Continue execution even if email sending fails
            }

            return ResponseEntity.ok(new MessageResponse("Verification code resent"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to resend verification code"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate the refresh token
        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Refresh token is required"));
        }

        if (!jwtUtils.validateJwtToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Invalid refresh token"));
        }

        try {
            // Get username from refresh token
            String username = jwtUtils.getUserNameFromJwtToken(refreshToken);

            // Get user details
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, ""));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate new access token
            String newAccessToken = jwtUtils.generateAccessToken(authentication);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Token refreshed successfully");
            response.put("accessToken", newAccessToken);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Token refresh failed"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody LogoutRequest request) {
        // In a production environment, you would invalidate the refresh token in a
        // token store
        // For this implementation, we'll simply return success
        // TODO: Implement token invalidation logic with a token store

        return ResponseEntity.ok(new MessageResponse("Logout successful"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutFromAllDevices() {
        // This endpoint requires authentication, which should be handled by Spring
        // Security
        // Get the authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // In a production environment, you would invalidate all refresh tokens for this
        // user
        // TODO: Implement token invalidation logic with a token store

        return ResponseEntity.ok(new MessageResponse("Logged out from all devices"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        // Always return 200 OK regardless of whether the email exists
        // This prevents email enumeration attacks

        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            try {
                userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
                    // Generate reset token
                    user.setResetPasswordToken(UUID.randomUUID().toString());
                    user.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(24));
                    userRepository.save(user);

                    // Send password reset email
                    try {
                        emailService.sendPasswordResetEmail(user.getEmail(), user.getResetPasswordToken());
                        logger.info("Password reset email sent to: {}", user.getEmail());
                    } catch (Exception e) {
                        logger.error("Failed to send password reset email: {}", e.getMessage());
                        // Continue execution even if email sending fails
                    }
                });
            } catch (Exception e) {
                // Log the error but don't expose it to the user
                logger.error("Error in forgot password flow: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(new MessageResponse(
                "If your email exists in our system, you will receive a password reset link"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        if (request.getToken() == null || request.getToken().isEmpty() ||
                request.getNewPassword() == null || request.getNewPassword().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Token and new password are required"));
        }

        try {
            User user = userRepository.findByResetPasswordToken(request.getToken())
                    .orElseThrow(() -> new RuntimeException("Invalid reset token"));

            if (user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body(new MessageResponse("Token expired"));
            }

            user.setPassword(encoder.encode(request.getNewPassword()));
            user.setResetPasswordToken(null);
            user.setResetPasswordTokenExpiry(null);
            userRepository.save(user);

            return ResponseEntity.ok(new MessageResponse("Password reset successful"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Password reset failed"));
        }
    }
}