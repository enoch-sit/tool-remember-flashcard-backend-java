# Java Guide for Non-Java Programmers

This guide is designed to help you understand the basics of Java, specifically in the context of our Flashcard Remembering Application backend.

## Introduction to Java

Java is a general-purpose, class-based, object-oriented programming language that's designed to have as few implementation dependencies as possible. It's intended to let application developers "write once, run anywhere" (WORA), meaning compiled Java code can run on any platform with a Java Virtual Machine (JVM) without recompilation.

## Core Java Concepts Used in Our Application

### 1. Classes and Objects

Java is an object-oriented language, and everything in Java is associated with classes and objects.

- **Class**: A blueprint or template for creating objects.
- **Object**: An instance of a class that has states (fields) and behaviors (methods).

Example from our codebase:

```java
// Class definition
public class Card {
    // Fields (states)
    private Long id;
    private String front;
    private String back;
    
    // Methods (behaviors)
    public String getFront() {
        return front;
    }
}

// Creating an object
Card flashcard = new Card();
```

### 2. Annotations

In our Spring Boot application, you'll see annotations (starting with `@`) frequently. These are metadata that provide data about a program.

Common annotations in our codebase:

- `@RestController`: Marks a class as a controller where every method returns a domain object.
- `@Entity`: Indicates that the class is a JPA entity (mapped to a database table).
- `@Autowired`: Spring's dependency injection annotation.

Example:

```java
@Entity
public class User {
    // Class content
}

@RestController
public class DeckController {
    @Autowired
    private DeckRepository deckRepository;
}
```

### 3. Interfaces

Interfaces in Java define a contract that implementing classes must follow. Spring Data JPA repositories are good examples of interfaces in our codebase.

Example:

```java
public interface DeckRepository extends JpaRepository<Deck, Long> {
    List<Deck> findByUser(User user);
}
```

### 4. Java Collections

Java provides several data structures through its Collections Framework:

- `List`: Ordered collection (e.g., ArrayList, LinkedList)
- `Set`: Unordered collection with no duplicates
- `Map`: Key-value pairs collection

Example from our codebase:

```java
List<Card> cards = cardRepository.findByDeck(deck);
Map<String, Object> response = new HashMap<>();
Set<Role> roles = new HashSet<>();
```

## Understanding Spring Boot Concepts

Our application uses Spring Boot, which is a framework that makes it easier to develop Java applications.

### 1. MVC Architecture

Our application follows the Model-View-Controller pattern:

- **Models**: Classes like `User`, `Deck`, and `Card` represent data
- **Controllers**: Classes like `DeckController` handle HTTP requests
- **Repositories**: Interfaces like `DeckRepository` access data from the database

### 2. Dependency Injection

Spring handles the creation and management of objects through its dependency injection framework. You'll see this with `@Autowired` annotations.

### 3. REST API

Our controllers expose RESTful endpoints that follow common conventions:

- GET: Retrieve resources
- POST: Create resources
- PUT/PATCH: Update resources
- DELETE: Remove resources

## Common Java Syntax to Know

### Variable Declarations

```java
String username = "user1";  // String
int count = 5;              // Integer
boolean isActive = true;    // Boolean
List<Card> cards = new ArrayList<>();  // Generic collection
```

### Conditional Statements

```java
if (user.isEnabled()) {
    // do something
} else if (user.isVerified()) {
    // do something else
} else {
    // default action
}
```

### Loops

```java
// For each loop
for (Card card : deck.getCards()) {
    // process each card
}

// Traditional for loop
for (int i = 0; i < cards.size(); i++) {
    Card card = cards.get(i);
    // process card
}

// Java 8+ Stream API
deck.getCards().forEach(card -> {
    // process each card
});
```

### Method Definitions

```java
public Card getCardById(Long id) {
    return cardRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Card not found"));
}
```

## Working with Our Codebase

### Project Structure

- `src/main/java`: Contains Java source code
- `src/main/resources`: Contains configuration files
- `src/test`: Contains test code
- `pom.xml`: Maven configuration file

### Key Packages

- `com.flashcardapp.models`: Contains data models
- `com.flashcardapp.repositories`: Contains data access interfaces
- `com.flashcardapp.controllers`: Contains REST API endpoints
- `com.flashcardapp.security`: Contains authentication and authorization code

### Running the Application

1. Ensure Java 11 or higher is installed
2. Build with Maven: `mvn clean install`
3. Run the application: `mvn spring-boot:run`

### Common Development Tasks

#### Adding a New Entity

1. Create a new class in the `models` package with `@Entity` annotation
2. Define fields with appropriate JPA annotations
3. Create a repository interface in the `repositories` package
4. Create a controller in the `controllers` package

#### Modifying an Existing API

1. Locate the appropriate controller class
2. Update the method for the corresponding HTTP operation
3. Test the API using a tool like Postman

## Debugging Tips

1. Use log statements with SLF4J:

   ```java
   private static final Logger logger = LoggerFactory.getLogger(YourClass.class);
   logger.info("Processing request with id: {}", id);
   ```

2. Spring Boot includes dev tools for automatic restart:

   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-devtools</artifactId>
       <scope>runtime</scope>
       <optional>true</optional>
   </dependency>
   ```

3. H2 console is available at `/h2-console` for database inspection

## Additional Resources

- [Official Java Documentation](https://docs.oracle.com/javase/tutorial/)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Baeldung Spring Tutorials](https://www.baeldung.com/spring-tutorial)
- [Spring Data JPA Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
