package com.flashcardapp.unit;

import com.flashcardapp.security.jwt.JwtUtils;
import com.flashcardapp.security.services.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtUtilsTest {

    @InjectMocks
    private JwtUtils jwtUtils;

    @Mock
    private Authentication authentication;

    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        // Set JWT secret and expiration via reflection
        // Use a valid base64-encoded string for the secret key
        String secretKey = Base64.getEncoder().encodeToString(
                "TEST_JWT_SECRET_KEY_THAT_IS_SUFFICIENTLY_LONG_FOR_TESTING_PURPOSES_ONLY".getBytes());
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", secretKey);
        ReflectionTestUtils.setField(jwtUtils, "jwtAccessExpirationMs", 60000);
        ReflectionTestUtils.setField(jwtUtils, "jwtRefreshExpirationMs", 300000);

        // Create UserDetails
        userDetails = new UserDetailsImpl(
                1L,
                "testuser",
                "test@example.com",
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        // Use lenient stubbing to avoid UnnecessaryStubbingException for tests that
        // don't use this mock
        lenient().when(authentication.getPrincipal()).thenReturn(userDetails);
    }

    @Test
    void generateAccessToken_ShouldCreateValidToken() {
        // Act
        String token = jwtUtils.generateAccessToken(authentication);

        // Assert
        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals("testuser", jwtUtils.getUserNameFromJwtToken(token));
    }

    @Test
    void generateRefreshToken_ShouldCreateValidToken() {
        // Act
        String token = jwtUtils.generateRefreshToken(authentication);

        // Assert
        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals("testuser", jwtUtils.getUserNameFromJwtToken(token));
    }

    @Test
    void validateJwtToken_WithInvalidToken_ShouldReturnFalse() {
        // Act & Assert
        assertFalse(jwtUtils.validateJwtToken("invalid.token.string"));
    }

    @Test
    void validateJwtToken_WithNullOrEmptyToken_ShouldReturnFalse() {
        // Act & Assert
        assertFalse(jwtUtils.validateJwtToken(null));
        assertFalse(jwtUtils.validateJwtToken(""));
    }
}