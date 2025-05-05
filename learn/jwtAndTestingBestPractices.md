# JWT and Testing Best Practices for Spring Boot Applications

This guide provides an overview of JWT (JSON Web Tokens) implementation and testing best practices for Spring Boot applications, specifically targeting our flashcard application.

## JSON Web Tokens (JWT) Overview

```mermaid
graph TD
    subgraph "JWT Token Structure"
    A[JWT Token] --> B[Header]
    A --> C[Payload/Claims]
    A --> D[Signature]
    end
    
    B --> B1["Algorithm (alg)"]
    B --> B2["Token Type (typ)"]
    
    C --> C1["Subject (sub)"]
    C --> C2["Issued At (iat)"]
    C --> C3["Expiration (exp)"]
    C --> C4["Custom Claims"]
    
    D --> D1["HMAC/RSA Signature"]
    
    style A fill:#f9f,stroke:#333,stroke-width:2px
    style B fill:#bbf,stroke:#333
    style C fill:#bbf,stroke:#333
    style D fill:#bbf,stroke:#333
```

JWT is a compact, self-contained means of securely transmitting information between parties as a JSON object. Our application uses JWTs for:

1. **Authentication**: Verifying user identity
2. **Authorization**: Determining what resources a user can access
3. **Information Exchange**: Securely transferring data between services

A JWT consists of three parts, separated by dots:

- **Header**: Identifies the algorithm used
- **Payload**: Contains claims (user data)
- **Signature**: Verifies the token hasn't been altered

Example token:

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

## JWT Authentication Flow in Our Application

```mermaid
sequenceDiagram
    participant User as User
    participant Client as Frontend Client
    participant Auth as Authentication Controller
    participant TokenProvider as JWT Token Provider 
    participant UserDetails as User Details Service
    participant DB as Database
    
    User->>Client: Enter credentials
    Client->>Auth: POST /api/auth/signin
    Auth->>UserDetails: loadUserByUsername()
    UserDetails->>DB: Query user data
    DB-->>UserDetails: User details
    UserDetails-->>Auth: UserDetails object
    Auth->>Auth: AuthenticationManager validates password
    Auth->>TokenProvider: Generate JWT
    TokenProvider-->>Auth: JWT Token
    Auth-->>Client: Return JWT + user info
    Client->>Client: Store JWT in localStorage
    
    Note over Client,Auth: Subsequent Authenticated Requests
    
    Client->>Auth: Request with Authorization header
    Auth->>TokenProvider: Validate JWT
    TokenProvider-->>Auth: Valid/Invalid + User details
    Auth->>Auth: Set SecurityContext
    Auth-->>Client: Protected resource
```

### Authentication Process

1. The client sends credentials (username/password) to `/api/auth/signin`
2. Server validates credentials
3. Server generates JWT with user identity and permissions
4. Client receives JWT and stores it
5. Client includes JWT in Authorization header for subsequent requests
6. Server validates JWT signature and extracts user details for each request

### JWT Implementation in Our Application

Our implementation uses Spring Security with JWT:

```java
// TokenProvider.java - Creates and validates tokens
public String generateToken(Authentication authentication) {
    UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
    
    return Jwts.builder()
        .setSubject(userPrincipal.getUsername())
        .setIssuedAt(new Date())
        .setExpiration(new Date(new Date().getTime() + jwtExpirationMs))
        .signWith(SignatureAlgorithm.HS512, jwtSecret)
        .compact();
}
```

```mermaid
graph TD
    A[JWT Filter Chain] -->|"Extract token from header"| B{Token exists?}
    B -->|No| Z[Continue filter chain]
    B -->|Yes| C{Validate token}
    C -->|Invalid| Z
    C -->|Valid| D[Extract username]
    D --> E[Load UserDetails]
    E --> F[Create Authentication object]
    F --> G[Set SecurityContext]
    G --> Z
    
    style A fill:#bbf,stroke:#333
    style C fill:#f9f,stroke:#333,stroke-width:2px
    style G fill:#9f9,stroke:#333
```

### Security Configuration

```java
// WebSecurityConfig.java
@Override
protected void configure(HttpSecurity http) throws Exception {
    http.cors().and().csrf().disable()
        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
        .authorizeRequests()
            .antMatchers("/api/auth/**").permitAll()
            .anyRequest().authenticated();
    
    http.addFilterBefore(jwtAuthenticationFilter(), 
            UsernamePasswordAuthenticationFilter.class);
}
```

```mermaid
graph TD
    subgraph "Spring Security Filter Chain"
    A[HTTP Request] --> B[CorsFilter]
    B --> C[JwtAuthenticationFilter]
    C --> D[UsernamePasswordAuthenticationFilter]
    D --> E[Other Security Filters]
    E --> F[Controller Endpoint]
    end
    
    subgraph "Authentication Decision Flow"
    C -->|"Extract JWT"| G{Valid JWT?}
    G -->|"Yes"| H[Set Authentication]
    G -->|"No/Missing"| I{Public URL?}
    I -->|"Yes"| F
    I -->|"No"| J[Return 401 Unauthorized]
    H --> F
    end
    
    style C fill:#f9f,stroke:#333,stroke-width:2px
    style F fill:#9f9,stroke:#333
    style J fill:#f99,stroke:#333
```

## JWT Best Practices

```mermaid
graph TD
    A[JWT Best Practices] --> B[Security]
    A --> C[Implementation]
    A --> D[Token Management]
    
    B --> B1[Use HTTPS]
    B --> B2[Strong Secret Keys]
    B --> B3[Short Expiration Times]
    
    C --> C1[Validate All Claims]
    C --> C2[Include Only Necessary Data]
    C --> C3[Handle Exceptions Properly]
    
    D --> D1[Token Revocation Strategy]
    D --> D2[Token Refresh Mechanism]
    D --> D3[Secure Storage on Client]
    
    style A fill:#f9f,stroke:#333,stroke-width:2px
    style B fill:#bbf,stroke:#333
    style C fill:#bbf,stroke:#333
    style D fill:#bbf,stroke:#333
```

1. **Use Strong Secret Keys**: Our application uses a secure, randomly generated key stored in application.properties

2. **Short Token Expiration**: We set reasonable expiration times (24 hours default)

   ```
   app.jwtExpirationMs=86400000
   ```

3. **Include Minimal Data**: Our tokens contain only user ID and roles, not sensitive data

4. **Use HTTPS**: All production deployments must use HTTPS

5. **Proper Error Handling**: Our AuthEntryPointJwt handles unauthorized requests gracefully

6. **Secure Token Storage**: Frontend applications should store tokens in secure storage

## Testing JWT Authentication

```mermaid
graph TD
    A[Testing Approaches] --> B[Unit Tests]
    A --> C[Integration Tests]
    A --> D[End-to-End Tests]
    
    B --> B1[JwtTokenProvider]
    B --> B2[UserDetailsService]
    B --> B3[AuthenticationFilters]
    
    C --> C1[Authentication Flow]
    C --> C2[Authorization Rules]
    C --> C3[Token Validation]
    
    D --> D1[Complete User Journeys]
    D --> D2[Edge Cases]
    D --> D3[Performance Tests]
    
    style A fill:#f9f,stroke:#333,stroke-width:2px
    style B fill:#bbf,stroke:#333
    style C fill:#bbf,stroke:#333
    style D fill:#bbf,stroke:#333
```

### Unit Testing JWT Components

For testing JWT token generation and validation:

```java
@Test
public void testGenerateToken() {
    // Setup UserDetails
    UserDetailsImpl userDetails = new UserDetailsImpl(
        1L, "test@example.com", "test", "password", authorities);
    
    Authentication authentication = new UsernamePasswordAuthenticationToken(
        userDetails, null, userDetails.getAuthorities());
    
    String token = jwtUtils.generateToken(authentication);
    assertNotNull(token);
    
    // Validate token
    boolean isValid = jwtUtils.validateToken(token);
    assertTrue(isValid);
    
    // Check username extraction
    String username = jwtUtils.getUsernameFromToken(token);
    assertEquals("test@example.com", username);
}
```

```mermaid
sequenceDiagram
    participant Test
    participant JwtUtils
    participant Jwts
    
    Test->>JwtUtils: generateToken(authentication)
    JwtUtils->>Jwts: builder().setSubject()...
    Jwts-->>JwtUtils: token
    JwtUtils-->>Test: JWT token
    
    Test->>JwtUtils: validateToken(token)
    JwtUtils->>Jwts: parser().setSigningKey()...
    Jwts-->>JwtUtils: valid/exception
    JwtUtils-->>Test: boolean result
    
    Test->>JwtUtils: getUsernameFromToken(token)
    JwtUtils->>Jwts: parser().setSigningKey().parseClaimsJws()
    Jwts-->>JwtUtils: Claims
    JwtUtils-->>Test: username
```

### Integration Testing Authentication

For testing complete authentication flows:

```java
@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testAuthenticationFlow() throws Exception {
        // 1. Register user
        mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"test@example.com\",\"password\":\"password\"}"))
            .andExpect(status().isOk());
            
        // 2. Login and get token
        MvcResult result = mockMvc.perform(post("/api/auth/signin")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"test@example.com\",\"password\":\"password\"}"))
            .andExpect(status().isOk())
            .andReturn();
            
        String response = result.getResponse().getContentAsString();
        String token = // Extract token from JSON response
        
        // 3. Access protected endpoint
        mockMvc.perform(get("/api/decks")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }
}
```

```mermaid
sequenceDiagram
    participant Test as Integration Test
    participant MockMvc
    participant AuthController
    participant JwtUtils
    participant UserService
    
    Test->>MockMvc: POST /api/auth/signup
    MockMvc->>AuthController: registerUser()
    AuthController->>UserService: createUser()
    UserService-->>AuthController: User created
    AuthController-->>MockMvc: 200 OK
    MockMvc-->>Test: Result
    
    Test->>MockMvc: POST /api/auth/signin
    MockMvc->>AuthController: authenticateUser()
    AuthController->>JwtUtils: generateToken()
    JwtUtils-->>AuthController: JWT Token
    AuthController-->>MockMvc: 200 OK + Token
    MockMvc-->>Test: Result with Token
    
    Test->>MockMvc: GET /api/decks with Token
    MockMvc->>AuthController: getDecks()
    AuthController-->>MockMvc: Deck Data
    MockMvc-->>Test: Result
```

### End-to-End Testing

We use Python-based automated tests that:

1. Register a new user
2. Verify the email (if required)
3. Sign in and get the token
4. Test protected endpoints with the token

```python
# test_flashcard_api.py (simplified example)
def test_auth_flow():
    # Register user
    register_data = {"username": "test@example.com", "password": "password123"}
    response = requests.post(f"{API_URL}/api/auth/signup", json=register_data)
    assert response.status_code == 200
    
    # Login to get token
    login_response = requests.post(f"{API_URL}/api/auth/signin", json=register_data)
    assert login_response.status_code == 200
    
    token = login_response.json()["accessToken"]
    headers = {"Authorization": f"Bearer {token}"}
    
    # Test protected endpoint
    decks_response = requests.get(f"{API_URL}/api/decks", headers=headers)
    assert decks_response.status_code == 200
```

```mermaid
flowchart TD
    A[Start Test] --> B[Register User]
    B --> C{Registration Successful?}
    C -->|No| D[Test Failed]
    C -->|Yes| E[Login User]
    E --> F{Login Successful?}
    F -->|No| D
    F -->|Yes| G[Extract JWT Token]
    G --> H[Test Protected Endpoints]
    H --> I{All Tests Passed?}
    I -->|No| D
    I -->|Yes| J[Test Successful]
    
    style A fill:#bbf,stroke:#333
    style D fill:#f99,stroke:#333
    style G fill:#f9f,stroke:#333,stroke-width:2px
    style J fill:#9f9,stroke:#333
```

## Testing Best Practices for Spring Boot Applications

```mermaid
graph TD
    A[Testing Best Practices] --> B[Test Types]
    A --> C[Test Structure]
    A --> D[Test Data]
    
    B --> B1[Unit Tests]
    B --> B2[Integration Tests]
    B --> B3[Performance Tests]
    
    C --> C1[Arrange-Act-Assert]
    C --> C2[Given-When-Then]
    C --> C3[Test Class per Component]
    
    D --> D1[Test Data Builders]
    D --> D2[Use Test Profiles]
    D --> D3[Clean Test Data]
    
    style A fill:#f9f,stroke:#333,stroke-width:2px
    style B fill:#bbf,stroke:#333
    style C fill:#bbf,stroke:#333
    style D fill:#bbf,stroke:#333
```

1. **Use Appropriate Test Types**:
   - **Unit Tests**: Test individual components in isolation (use Mockito for dependencies)
   - **Integration Tests**: Test how components work together (@SpringBootTest)
   - **End-to-End Tests**: Test complete flows (external test scripts)

2. **Follow AAA Pattern**:
   - **Arrange**: Set up test data and conditions
   - **Act**: Call the method/functionality being tested
   - **Assert**: Verify the results

3. **Use Meaningful Test Names**:

   ```java
   @Test
   public void should_ReturnTrue_When_TokenIsValid()
   ```

4. **Test Happy Path and Edge Cases**:
   - Test successful scenarios
   - Test failure cases (invalid tokens, expired tokens)
   - Test boundary conditions (token just about to expire)

5. **Use Spring's Testing Tools**:
   - @SpringBootTest for larger integration tests
   - @WebMvcTest for controller-layer tests
   - @DataJpaTest for repository tests
   - @MockBean to mock dependencies

6. **Secure Test Credentials**:
   - Never commit actual secrets in test resources
   - Use test-specific application.properties
   - Use environment variables or Spring profiles for sensitive data

```mermaid
flowchart TD
    A[Spring Boot Test Annotations] --> B["@SpringBootTest"]
    A --> C["@WebMvcTest"]
    A --> D["@DataJpaTest"]
    A --> E["@MockBean"]
    A --> F["@AutoConfigureMockMvc"]
    
    B -->|"Tests"| B1[Complete Application]
    C -->|"Tests"| C1[MVC Controllers]
    D -->|"Tests"| D1[JPA Repositories]
    E -->|"Creates"| E1[Mock Dependencies]
    F -->|"Provides"| F1[MockMvc for HTTP testing]
    
    style A fill:#f9f,stroke:#333,stroke-width:2px
    style B fill:#bbf,stroke:#333
    style C fill:#bbf,stroke:#333
    style D fill:#bbf,stroke:#333
    style E fill:#bbf,stroke:#333
    style F fill:#bbf,stroke:#333
```

## Special Considerations for JWT Testing

1. **Create Test Tokens**:

   ```java
   private String createTestToken(String username, List<String> roles) {
       // Generate test-only token
   }
   ```

2. **Mock JwtTokenProvider in Tests**:

   ```java
   @MockBean
   private JwtTokenProvider tokenProvider;
   
   @Before
   public void setup() {
       when(tokenProvider.validateToken(anyString())).thenReturn(true);
       when(tokenProvider.getUsernameFromToken(anyString())).thenReturn("testuser");
   }
   ```

3. **Test Token Expiration**:

   ```java
   @Test
   public void shouldRejectExpiredToken() {
       // Create expired token
       String expiredToken = createExpiredToken();
       
       assertFalse(jwtUtils.validateToken(expiredToken));
   }
   ```

```mermaid
sequenceDiagram
    participant Test
    participant JwtUtils
    participant Clock
    
    Note over Test,Clock: Testing Expiration
    
    Test->>Test: Create token with past expiration
    Test->>JwtUtils: validateToken(expiredToken)
    JwtUtils->>JwtUtils: Parse and check expiration
    JwtUtils-->>Test: false (invalid)
    
    Note over Test,Clock: Testing Clock Manipulation
    
    Test->>Clock: mockStatic(Clock.class)
    Test->>Clock: Set fixed clock in future
    Test->>JwtUtils: validateToken(token)
    JwtUtils->>Clock: now()
    Clock-->>JwtUtils: Future time
    JwtUtils-->>Test: false (expired)
```

## Security Testing

1. **Test for Common Vulnerabilities**:
   - Ensure endpoints properly enforce authentication
   - Test authorization (can users access only their own data?)
   - Check for token validation bypass vulnerabilities

2. **Use Security Scanning Tools**:
   - OWASP ZAP
   - SonarQube

```mermaid
flowchart TD
    A[Security Testing] --> B[Authentication Testing]
    A --> C[Authorization Testing]
    A --> D[Vulnerability Scanning]
    
    B --> B1[Valid Credentials Test]
    B --> B2[Invalid Credentials Test]
    B --> B3[Token Validation Test]
    
    C --> C1[Resource Access Tests]
    C --> C2[Role-Based Access Tests]
    C --> C3[Cross-User Access Tests]
    
    D --> D1[OWASP ZAP]
    D --> D2[SonarQube]
    D --> D3[Manual Penetration Testing]
    
    style A fill:#f9f,stroke:#333,stroke-width:2px
    style B fill:#bbf,stroke:#333
    style C fill:#bbf,stroke:#333
    style D fill:#bbf,stroke:#333
```

## Conclusion

A well-implemented JWT authentication system combined with comprehensive testing provides secure and reliable user authentication for our flashcard application. By following these best practices, we ensure:

1. Secure user authentication and authorization
2. Protection of user data
3. Identification of bugs and vulnerabilities early in development
4. Documentation of expected behavior through tests

These practices make our application more maintainable and ensure consistent quality as we add new features.
