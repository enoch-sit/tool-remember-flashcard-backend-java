# JWT and Testing Best Practices in Java Spring Boot Applications

This guide explains common issues and best practices with JWT implementation and testing in Spring Boot applications. It's based on real-world fixes made to our Flashcard Application backend.

## Common JWT Issues and How to Avoid Them

### 1. Base64 Encoding JWT Secret Keys

#### The Problem We Fixed

We encountered `Illegal base64 character: '_'` errors when testing our JWT token functionality. This happened because the JWT library expects the secret key to be Base64-encoded, but our test was using a plain string with underscores.

#### How to Avoid It

When working with libraries like JJWT (Java JWT), ensure your secret keys are properly Base64-encoded:

```java
// INCORRECT - Using raw string with special characters
@Value("${jwt.secret}")
private String jwtSecret;

// CORRECT - Base64 encode the secret key before use
private Key key() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
}
```

For testing, ensure you're using a properly encoded secret key:

```java
// INCORRECT
ReflectionTestUtils.setField(jwtUtils, "jwtSecret", 
    "TEST_JWT_SECRET_KEY_WITH_UNDERSCORES"); // Will fail with Base64 error

// CORRECT
String secretKey = Base64.getEncoder().encodeToString(
    "TEST_JWT_SECRET_KEY_WITH_UNDERSCORES".getBytes());
ReflectionTestUtils.setField(jwtUtils, "jwtSecret", secretKey);
```

### 2. Secret Key Length in Production

While not directly related to our fixes, it's important to note that JWT requires secret keys of sufficient length for different algorithms.

```java
// For HMAC-SHA algorithms, make sure your key has enough bits
// - HS256 requires at least 256 bits (32 bytes)
// - HS384 requires at least 384 bits (48 bytes)
// - HS512 requires at least 512 bits (64 bytes)

// Example of generating a secure random key for production
SecureRandom random = new SecureRandom();
byte[] keyBytes = new byte[64]; // For HS512
random.nextBytes(keyBytes);
String base64Key = Base64.getEncoder().encodeToString(keyBytes);

// Store this in your application.properties/application.yml
```

## Testing Best Practices

### 1. Mockito Unnecessary Stubbing Exception

#### The Problem We Fixed

We encountered `UnnecessaryStubbingException` errors in tests where we were setting up mocks that weren't used in all test methods.

#### How to Avoid It

Use Mockito's `lenient()` method when setting up mocks that might not be used by all test methods in a test class:

```java
// INCORRECT - Will cause UnnecessaryStubbingException if not used in all tests
when(authentication.getPrincipal()).thenReturn(userDetails);

// CORRECT - Lenient stubbing won't cause exceptions if unused
lenient().when(authentication.getPrincipal()).thenReturn(userDetails);
```

This is especially useful in `@BeforeEach` setup methods where not all stubs will be used by every test method.

### 2. Integration Testing Delete Operations

#### The Problem We Fixed

We had an issue in our integration test where after deleting a resource, the test was trying to verify the deletion by making another API call. This was causing a `NestedServletException` because the controller was throwing a `RuntimeException` when the resource wasn't found.

#### Better Approaches

1. **Directly verify in the database** (preferred for integration tests):

   ```java
   // After deleting via API
   mockMvc.perform(delete("/api/decks/" + deckId)
           .header("Authorization", "Bearer " + accessToken))
           .andExpect(status().isOk());
   
   // Verify using the repository directly
   assert !deckRepository.existsById(deckId) : "Deck should have been deleted";
   ```

2. **If you need to test the API response for a missing resource**:

   ```java
   // Use andExpect() with result matcher
   mockMvc.perform(get("/api/decks/" + deckId))
       .andExpect(result -> {
           assertEquals(404, result.getResponse().getStatus());
           // Or check for specific error message
       });
   ```

### 3. Transaction Management in Tests

For tests that modify data and then immediately check for those modifications:

```java
@Test
@Transactional // Makes test run in a transaction
void testDeleteAndVerify() {
    // Test code
}

// OR

@Autowired
private TestEntityManager entityManager;

@Test
void testWithFlush() {
    repository.delete(entity);
    entityManager.flush(); // Force changes to be applied
    // Verify deletion
}
```

## General Testing Tips for Spring Boot Applications

1. **Use appropriate test slices**:
   - `@WebMvcTest` for controller layer only
   - `@DataJpaTest` for repository layer only
   - `@SpringBootTest` for full integration tests

2. **Clean state between tests**:

   ```java
   @BeforeEach
   void setUp() {
       // Clear relevant tables
       repository.deleteAll();
   }
   ```

3. **Mock external systems** in unit tests, but use test containers or in-memory alternatives for integration tests.

4. **Use meaningful test method names** that describe the scenario and expected outcome:

   ```java
   void deleteDeck_ShouldDeleteDeck_AndReturnSuccessMessage()
   ```

5. **Set up separate test properties** in `src/test/resources/application.properties` for test-specific configurations.

## JWT Security Best Practices

1. **Store secrets securely**: Never commit secrets to source control. Use environment variables or secure vaults.

2. **Token expiration**: Set appropriate expiration times for tokens. Access tokens should be short-lived.

3. **Token validation**: Always validate tokens properly:
   - Check signature
   - Verify the token hasn't expired
   - Validate any custom claims

4. **HTTPS**: Always use HTTPS in production to protect tokens in transit.

5. **CSRF protection**: If your clients use cookies, ensure CSRF protection is implemented.

By following these best practices, you'll avoid common pitfalls in JWT implementation and testing in your Spring Boot applications.
