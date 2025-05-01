package com.flashcardapp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashcardapp.models.ERole;
import com.flashcardapp.models.Role;
import com.flashcardapp.models.User;
import com.flashcardapp.payload.request.LoginRequest;
import com.flashcardapp.payload.request.SignupRequest;
import com.flashcardapp.repositories.RoleRepository;
import com.flashcardapp.repositories.UserRepository;
import com.flashcardapp.security.jwt.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        // Clear user repository
        userRepository.deleteAll();

        // Set JWT secret via reflection to ensure it's a valid base64 string
        String secretKey = Base64.getEncoder().encodeToString(
                "TEST_JWT_SECRET_KEY_THAT_IS_SUFFICIENTLY_LONG_FOR_TESTING_PURPOSES_ONLY".getBytes());
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", secretKey);

        // Create roles if they don't exist
        if (!roleRepository.findByName(ERole.ROLE_USER).isPresent()) {
            roleRepository.save(new Role(null, ERole.ROLE_USER));
        }
        if (!roleRepository.findByName(ERole.ROLE_SUPERVISOR).isPresent()) {
            roleRepository.save(new Role(null, ERole.ROLE_SUPERVISOR));
        }
        if (!roleRepository.findByName(ERole.ROLE_ADMIN).isPresent()) {
            roleRepository.save(new Role(null, ERole.ROLE_ADMIN));
        }
    }

    @Test
    void signupUser_ShouldCreateUser() throws Exception {
        // Arrange
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("newuser");
        signupRequest.setEmail("newuser@example.com");
        signupRequest.setPassword("password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", containsString("User registered successfully")))
                .andExpect(jsonPath("$.userId").exists());

        // Verify user was created
        User createdUser = userRepository.findByUsername("newuser").orElse(null);
        assertNotNull(createdUser);
        assertEquals("newuser@example.com", createdUser.getEmail());
        assertTrue(passwordEncoder.matches("password123", createdUser.getPassword()));
        assertEquals(1, createdUser.getRoles().size());
        assertTrue(createdUser.getRoles().stream().anyMatch(role -> role.getName() == ERole.ROLE_USER));
    }

    @Test
    void signupUser_WithDuplicateUsername_ShouldFailWithBadRequest() throws Exception {
        // Arrange
        User existingUser = User.builder()
                .username("existinguser")
                .email("existing@example.com")
                .password(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(existingUser);

        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("existinguser"); // duplicate username
        signupRequest.setEmail("new@example.com");
        signupRequest.setPassword("password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Username is already taken")));
    }

    @Test
    void signupUser_WithDuplicateEmail_ShouldFailWithBadRequest() throws Exception {
        // Arrange
        User existingUser = User.builder()
                .username("existinguser")
                .email("existing@example.com")
                .password(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(existingUser);

        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("newuser");
        signupRequest.setEmail("existing@example.com"); // duplicate email
        signupRequest.setPassword("password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Email is already in use")));
    }

    @Test
    void loginUser_WithValidCredentials_ShouldReturnTokens() throws Exception {
        // Arrange
        Role userRole = roleRepository.findByName(ERole.ROLE_USER).orElseThrow();
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .enabled(true)
                .emailVerified(true)
                .roles(roles)
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Login successful")))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.user.username", is("testuser")))
                .andExpect(jsonPath("$.user.email", is("test@example.com")))
                .andReturn();
    }

    @Test
    void loginUser_WithInvalidCredentials_ShouldFailWithUnauthorized() throws Exception {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .enabled(true)
                .emailVerified(true)
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("wrongpassword");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginUser_WithUnverifiedEmail_ShouldFailWithUnauthorized() throws Exception {
        // Arrange
        Role userRole = roleRepository.findByName(ERole.ROLE_USER).orElseThrow();
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        User user = User.builder()
                .username("unverified")
                .email("unverified@example.com")
                .password(passwordEncoder.encode("password123"))
                .enabled(true)
                .emailVerified(false) // Unverified email
                .roles(roles)
                .verificationToken(UUID.randomUUID().toString())
                .verificationTokenExpiry(LocalDateTime.now().plusDays(1))
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("unverified");
        loginRequest.setPassword("password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Email not verified")));
    }

    @Test
    void verifyEmail_WithValidToken_ShouldVerifyUser() throws Exception {
        // Arrange
        String verificationToken = UUID.randomUUID().toString();
        User user = User.builder()
                .username("unverified")
                .email("unverified@example.com")
                .password(passwordEncoder.encode("password123"))
                .enabled(true)
                .emailVerified(false)
                .verificationToken(verificationToken)
                .verificationTokenExpiry(LocalDateTime.now().plusDays(1))
                .build();
        userRepository.save(user);

        // Act & Assert
        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + verificationToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Email verified successfully")));

        // Verify user's email is now verified
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(updatedUser.isEmailVerified());
        assertNull(updatedUser.getVerificationToken());
        assertNull(updatedUser.getVerificationTokenExpiry());
    }
}