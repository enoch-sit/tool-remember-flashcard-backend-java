# Possible API Flows in the Flashcard Application

Below is a table of the main user flows possible with this API stack:

| Flow ID | Flow Name | Description | Key Endpoints Used |
|---------|-----------|-------------|-------------------|
| 1 | User Registration & Verification | User creates account and verifies email | `/api/auth/signup`, `/api/auth/verify-email` |
| 2 | Login & Authentication | User logs in and maintains authentication | `/api/auth/login`, `/api/auth/refresh` |
| 3 | Password Recovery | User recovers forgotten password | `/api/auth/forgot-password`, `/api/auth/reset-password` |
| 4 | Deck Creation & Management | User creates and manages flashcard decks | `/api/decks`, `/api/decks/{deckId}` |
| 5 | Card Creation & Management | User creates and manages cards within decks | `/api/decks/{deckId}/cards`, `/api/decks/{deckId}/cards/{cardId}` |
| 6 | Study Session | User conducts a study session with review | `/api/decks/{deckId}/review-cards`, `/api/decks/{deckId}/study-sessions`, `/api/study-sessions/{sessionId}/reviews` |
| 7 | Performance Tracking | User reviews their study statistics | `/api/study-sessions`, `/api/stats/study-activity` |
| 8 | Card Review History | User views history of specific card reviews | `/api/cards/{cardId}/reviews` |
| 9 | Multiple Device Management | User manages sessions across devices | `/api/auth/logout`, `/api/auth/logout-all` |
| 10 | Deck Browsing & Search | User browses and searches their decks | `/api/decks` with search parameters |
| 11 | Spaced Repetition Learning | User learns with spaced repetition algorithm | `/api/decks/{deckId}/review-cards`, `/api/study-sessions/{sessionId}/reviews` |
| 12 | Study Session Analysis | User analyzes details of past study sessions | `/api/study-sessions/{sessionId}` |

These flows represent the major user journeys enabled by the API stack, from account management to the core learning features utilizing spaced repetition algorithms.
