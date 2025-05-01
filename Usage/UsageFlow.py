#!/usr/bin/env python3
import requests
import json
import time
import sys
import os
from datetime import datetime, timedelta
import getpass


class FlashcardAPITester:
    def __init__(self):
        self.base_url = "http://localhost:3000"
        self.access_token = None
        self.refresh_token = None
        self.user_data = None
        self.current_deck_id = None
        self.current_card_ids = []
        self.current_session_id = None

    def set_base_url(self, url):
        self.base_url = url.rstrip("/")
        print(f"Base URL set to: {self.base_url}")

    def clear_terminal(self):
        os.system("cls" if os.name == "nt" else "clear")

    def print_header(self, text):
        print("\n" + "=" * 80)
        print(f" {text} ".center(80, "="))
        print("=" * 80)

    def print_response(self, response):
        print(f"\nStatus Code: {response.status_code}")
        try:
            json_response = response.json()
            print("Response:")
            print(json.dumps(json_response, indent=2))
            return json_response
        except:
            print(f"Raw Response: {response.text}")
            return None

    def get_headers(self):
        headers = {"Content-Type": "application/json"}
        if self.access_token:
            headers["Authorization"] = f"Bearer {self.access_token}"
        return headers

    def refresh_auth_token(self):
        if not self.refresh_token:
            print("No refresh token available. Please log in first.")
            return False

        print("Refreshing access token...")
        response = requests.post(
            f"{self.base_url}/api/auth/refresh",
            headers={"Content-Type": "application/json"},
            json={"refreshToken": self.refresh_token},
        )

        if response.status_code == 200:
            data = response.json()
            self.access_token = data.get("accessToken")
            print("Access token refreshed successfully!")
            return True
        else:
            print("Failed to refresh access token. Please log in again.")
            self.access_token = None
            self.refresh_token = None
            return False

    def check_api_health(self):
        self.print_header("Check API Health")
        response = requests.get(f"{self.base_url}/health")
        self.print_response(response)

    # Flow 1: User Registration & Verification
    def register_user(self):
        self.print_header("User Registration")
        username = input("Enter username: ")
        email = input("Enter email: ")
        password = getpass.getpass("Enter password: ")

        response = requests.post(
            f"{self.base_url}/api/auth/signup",
            headers={"Content-Type": "application/json"},
            json={"username": username, "email": email, "password": password},
        )

        result = self.print_response(response)
        if response.status_code == 201:
            print(
                "\nUser registration successful! Check your email for verification code."
            )
            self.verify_email()

    def verify_email(self):
        self.print_header("Email Verification")
        token = input("Enter the verification token from email: ")

        response = requests.post(
            f"{self.base_url}/api/auth/verify-email",
            headers={"Content-Type": "application/json"},
            json={"token": token},
        )

        self.print_response(response)
        if response.status_code == 200:
            print("\nEmail verification successful! You can now log in.")

    # Flow 2: Login & Authentication
    def login(self):
        self.print_header("Login")
        email = input("Enter email: ")
        password = getpass.getpass("Enter password: ")

        response = requests.post(
            f"{self.base_url}/api/auth/login",
            headers={"Content-Type": "application/json"},
            json={"email": email, "password": password},
        )

        result = self.print_response(response)
        if response.status_code == 200:
            self.access_token = result.get("accessToken")
            self.refresh_token = result.get("refreshToken")
            self.user_data = result.get("user")
            print("\nLogin successful!")

    def logout(self):
        self.print_header("Logout")
        if not self.refresh_token:
            print("You are not logged in.")
            return

        response = requests.post(
            f"{self.base_url}/api/auth/logout",
            headers={"Content-Type": "application/json"},
            json={"refreshToken": self.refresh_token},
        )

        self.print_response(response)
        if response.status_code == 200:
            self.access_token = None
            self.refresh_token = None
            self.user_data = None
            print("\nLogout successful!")

    def logout_all_devices(self):
        self.print_header("Logout from All Devices")
        if not self.access_token:
            print("You are not logged in.")
            return

        response = requests.post(
            f"{self.base_url}/api/auth/logout-all", headers=self.get_headers()
        )

        self.print_response(response)
        if response.status_code == 200:
            self.access_token = None
            self.refresh_token = None
            self.user_data = None
            print("\nLogged out from all devices!")

    # Flow 3: Password Recovery
    def forgot_password(self):
        self.print_header("Forgot Password")
        email = input("Enter your email: ")

        response = requests.post(
            f"{self.base_url}/api/auth/forgot-password",
            headers={"Content-Type": "application/json"},
            json={"email": email},
        )

        self.print_response(response)
        if response.status_code == 200:
            print("\nIf the email exists, a password reset link has been sent.")
            self.reset_password()

    def reset_password(self):
        self.print_header("Reset Password")
        token = input("Enter the password reset token from email: ")
        new_password = getpass.getpass("Enter new password: ")

        response = requests.post(
            f"{self.base_url}/api/auth/reset-password",
            headers={"Content-Type": "application/json"},
            json={"token": token, "newPassword": new_password},
        )

        self.print_response(response)

    # Flow 4: Deck Creation & Management
    def get_all_decks(self):
        self.print_header("Get All Decks")
        if not self.access_token:
            print("You must be logged in to perform this action.")
            return

        response = requests.get(
            f"{self.base_url}/api/decks", headers=self.get_headers()
        )

        result = self.print_response(response)
        if result and isinstance(result, list) and len(result) > 0:
            print("\nAvailable Decks:")
            for i, deck in enumerate(result):
                print(f"{i+1}. {deck.get('name')} (ID: {deck.get('id')})")

            choice = input(
                "\nSelect a deck number to set as current (or press Enter to skip): "
            )
            if choice.isdigit() and 1 <= int(choice) <= len(result):
                self.current_deck_id = result[int(choice) - 1].get("id")
                print(f"Current deck set to: {result[int(choice)-1].get('name')}")

    def create_deck(self):
        self.print_header("Create New Deck")
        if not self.access_token:
            print("You must be logged in to perform this action.")
            return

        name = input("Enter deck name: ")
        description = input("Enter deck description: ")
        tags = input("Enter tags (comma-separated): ")

        payload = {"name": name, "description": description}

        if tags.strip():
            payload["tags"] = [tag.strip() for tag in tags.split(",")]

        response = requests.post(
            f"{self.base_url}/api/decks", headers=self.get_headers(), json=payload
        )

        result = self.print_response(response)
        if response.status_code == 201 and result:
            self.current_deck_id = result.get("id")
            print(f"\nDeck created successfully! Current deck set to: {name}")

    def get_deck_details(self):
        self.print_header("Get Deck Details")
        if not self.access_token:
            print("You must be logged in to perform this action.")
            return

        if not self.current_deck_id:
            print("No deck selected. Please get all decks first and select one.")
            return

        response = requests.get(
            f"{self.base_url}/api/decks/{self.current_deck_id}",
            headers=self.get_headers(),
        )

        self.print_response(response)

    def update_deck(self):
        self.print_header("Update Deck")
        if not self.access_token:
            print("You must be logged in to perform this action.")
            return

        if not self.current_deck_id:
            print("No deck selected. Please get all decks first and select one.")
            return

        name = input("Enter new deck name (press Enter to keep current): ")
        description = input(
            "Enter new deck description (press Enter to keep current): "
        )
        tags = input("Enter new tags (comma-separated, press Enter to keep current): ")

        payload = {}
        if name.strip():
            payload["name"] = name
        if description.strip():
            payload["description"] = description
        if tags.strip():
            payload["tags"] = [tag.strip() for tag in tags.split(",")]

        if not payload:
            print("No changes to make.")
            return

        response = requests.put(
            f"{self.base_url}/api/decks/{self.current_deck_id}",
            headers=self.get_headers(),
            json=payload,
        )

        self.print_response(response)

    def delete_deck(self):
        self.print_header("Delete Deck")
        if not self.access_token:
            print("You must be logged in to perform this action.")
            return

        if not self.current_deck_id:
            print("No deck selected. Please get all decks first and select one.")
            return

        confirm = input(f"Are you sure you want to delete the current deck? (y/N): ")
        if confirm.lower() != "y":
            print("Deletion cancelled.")
            return

        response = requests.delete(
            f"{self.base_url}/api/decks/{self.current_deck_id}",
            headers=self.get_headers(),
        )

        self.print_response(response)
        if response.status_code == 200:
            print("\nDeck deleted successfully!")
            self.current_deck_id = None
            self.current_card_ids = []

    # Flow 5: Card Creation & Management
    def get_cards_in_deck(self):
        self.print_header("Get Cards in Deck")
        if not self.access_token:
            print("You must be logged in to perform this action.")
            return

        if not self.current_deck_id:
            print("No deck selected. Please get all decks first and select one.")
            return

        response = requests.get(
            f"{self.base_url}/api/decks/{self.current_deck_id}/cards",
            headers=self.get_headers(),
        )

        result = self.print_response(response)

        # Extract card IDs for later use
        self.current_card_ids = []
        if result and "content" in result and len(result["content"]) > 0:
            print("\nCards in this deck:")
            for i, card in enumerate(result["content"]):
                self.current_card_ids.append(card.get("id"))
                print(
                    f"{i+1}. Front: {card.get('front')} | Back: {card.get('back')} (ID: {card.get('id')})"
                )
        else:
            print("No cards found in this deck.")

    def create_card(self):
        self.print_header("Create New Card")
        if not self.access_token:
            print("You must be logged in to perform this action.")
            return

        if not self.current_deck_id:
            print("No deck selected. Please get all decks first and select one.")
            return

        front = input("Enter card front (question): ")
        back = input("Enter card back (answer): ")
        notes = input("Enter notes (optional): ")
        tags = input("Enter tags (comma-separated, optional): ")

        payload = {"front": front, "back": back}

        if notes.strip():
            payload["notes"] = notes
        if tags.strip():
            payload["tags"] = [tag.strip() for tag in tags.split(",")]

        response = requests.post(
            f"{self.base_url}/api/decks/{self.current_deck_id}/cards",
            headers=self.get_headers(),
            json=payload,
        )

        result = self.print_response(response)
        if response.status_code == 201 and result:
            card_id = result.get("id")
            if card_id:
                self.current_card_ids.append(card_id)
                print(f"\nCard created successfully with ID: {card_id}")

    def get_card_details(self):
        self.print_header("Get Card Details")
        if not self.access_token:
            print("You must be logged in to perform this action.")
            return

        if not self.current_card_ids:
            print("No cards available. Please get cards in the deck first.")
            return

        print("Available cards:")
        for i, card_id in enumerate(self.current_card_ids):
            print(f"{i+1}. Card ID: {card_id}")

        choice = input("\nSelect a card number: ")
        if (
            not choice.isdigit()
            or int(choice) < 1
            or int(choice) > len(self.current_card_ids)
        ):
            print("Invalid selection.")
            return

        card_id = self.current_card_ids[int(choice) - 1]
        response = requests.get(
            f"{self.base_url}/api/decks/{self.current_deck_id}/cards/{card_id}",
            headers=self.get_headers(),
        )

        self.print_response(response)

    def update_card(self):
        self.print_header("Update Card")
        if not self.access_token:
            print("You must be logged in to perform this action.")
            return

        if not self.current_card_ids:
            print("No cards available. Please get cards in the deck first.")
            return

        print("Available cards:")
        for i, card_id in enumerate(self.current_card_ids):
            print(f"{i+1}. Card ID: {card_id}")

        choice = input("\nSelect a card number: ")
        if (
            not choice.isdigit()
            or int(choice) < 1
            or int(choice) > len(self.current_card_ids)
        ):
            print("Invalid selection.")
            return

        card_id = self.current_card_ids[int(choice) - 1]
        front = input("Enter new card front (press Enter to keep current): ")
        back = input("Enter new card back (press Enter to keep current): ")
        notes = input("Enter new notes (press Enter to keep current): ")
        tags = input("Enter new tags (comma-separated, press Enter to keep current): ")

        payload = {}
        if front.strip():
            payload["front"] = front
        if back.strip():
            payload["back"] = back
        if notes.strip():
            payload["notes"] = notes
        if tags.strip():
            payload["tags"] = [tag.strip() for tag in tags.split(",")]

        if not payload:
            print("No changes to make.")
            return

        response = requests.put(
            f"{self.base_url}/api/decks/{self.current_deck_id}/cards/{card_id}",
            headers=self.get_headers(),
            json=payload,
        )

        self.print_response(response)

    def delete_card(self):
        self.print_header("Delete Card")
        if not self.access_token:
            print("You must be logged in to perform this action.")
            return

        if not self.current_card_ids:
            print("No cards available. Please get cards in the deck first.")
            return

        print("Available cards:")
        for i, card_id in enumerate(self.current_card_ids):
            print(f"{i+1}. Card ID: {card_id}")

        choice = input("\nSelect a card number to delete: ")
        if (
            not choice.isdigit()
            or int(choice) < 1
            or int(choice) > len(self.current_card_ids)
        ):
            print("Invalid selection.")
            return

        card_index = int(choice) - 1
        card_id = self.current_card_ids[card_index]

        confirm = input(f"Are you sure you want to delete card {card_id}? (y/N): ")
        if confirm.lower() != "y":
            print("Deletion cancelled.")
            return

        response = requests.delete(
            f"{self.base_url}/api/decks/{self.current_deck_id}/cards/{card_id}",
            headers=self.get_headers(),
        )

        self.print_response(response)
        if response.status_code == 200:
            print(f"\nCard {card_id} deleted successfully!")
            self.current_card_ids.pop(card_index)

    # Flow 6: Study Session
    def get_cards_for_review(self):
        self.print_header("Get Cards Due for Review")
        if not self.access_token:
            print("You must be logged in to perform this action.")
            return

        if not self.current_deck_id:
            print("No deck selected. Please get all decks first and select one.")
            return

        limit = input("Enter maximum number of cards to review (default: 10): ")
        if not limit.isdigit():
            limit = "10"

        response = requests.get(
            f"{self.base_url}/api/decks/{self.current_deck_id}/review-cards?limit={limit}",
            headers=self.get_headers(),
        )

        result = self.print_response(response)
        return result

    def start_study_session(self):
        self.print_header("Start Study Session")
        if not self.access_token:
            print("You must be logged in to perform this action.")
            return

        if not self.current_deck_id:
            print("No deck selected. Please get all decks first and select one.")
            return

        response = requests.post(
            f"{self.base_url}/api/decks/{self.current_deck_id}/study-sessions",
            headers=self.get_headers(),
        )

        result = self.print_response(response)
        if response.status_code == 201 and result:
            self.current_session_id = result.get("sessionId")
            print(f"\nStudy session started with ID: {self.current_session_id}")

            # Get cards due for review
            self.review_cards()

    def review_cards(self):
        if not self.current_session_id:
            print("No active study session. Please start a study session first.")
            return

        # Get cards due for review
        cards = self.get_cards_for_review()
        if not cards or "cards" not in cards or not cards["cards"]:
            print("No cards available for review.")
            return

        review_cards = cards["cards"]
        card_count = len(review_cards)
        reviewed_count = 0

        print(f"\nBeginning review of {card_count} cards...\n")

        for card in review_cards:
            card_id = card.get("id")
            self.print_header(f"Reviewing Card ({reviewed_count + 1}/{card_count})")

            print(f"Question: {card.get('front')}")
            input("\nPress Enter to show answer...")

            print(f"\nAnswer: {card.get('back')}")
            if card.get("notes"):
                print(f"Notes: {card.get('notes')}")

            while True:
                rating = input(
                    "\nRate your recall (0-5, where 0=incorrect, 5=perfect): "
                )
                if rating.isdigit() and 0 <= int(rating) <= 5:
                    break
                print("Please enter a number between 0 and 5.")

            start_time = time.time()
            time_spent_ms = int((time.time() - start_time) * 1000)

            response = requests.post(
                f"{self.base_url}/api/study-sessions/{self.current_session_id}/reviews",
                headers=self.get_headers(),
                json={
                    "cardId": card_id,
                    "result": int(rating),
                    "timeSpentMs": time_spent_ms,
                },
            )

            if response.status_code != 201:
                print("Failed to submit review:")
                self.print_response(response)

            reviewed_count += 1

        self.complete_study_session()

    def complete_study_session(self):
        self.print_header("Complete Study Session")
        if not self.access_token:
            print("You must be logged in to perform this action.")
            return

        if not self.current_session_id:
            print("No active study session. Please start a study session first.")
            return

        response = requests.put(
            f"{self.base_url}/api/study-sessions/{self.current_session_id}/complete",
            headers=self.get_headers(),
            json={"timeSpentMs": 900000},  # Example: 15 minutes in milliseconds
        )

        self.print_response(response)
        if response.status_code == 200:
            print("\nStudy session completed successfully!")
            self.current_session_id = None

    # Flow 7: Performance Tracking
    def get_study_sessions(self):
        self.print_header("Get Study Sessions")
        if not self.access_token:
            print("You must be logged in to perform this action.")
            return

        response = requests.get(
            f"{self.base_url}/api/study-sessions", headers=self.get_headers()
        )

        result = self.print_response(response)
        if result and "content" in result and len(result["content"]) > 0:
            print("\nStudy sessions:")
            for i, session in enumerate(result["content"]):
                print(f"{i+1}. Deck: {session.get('deckName')}")
                print(f"   Started: {session.get('startedAt')}")
                print(f"   Cards studied: {session.get('cardsStudied')}")
                print(f"   Correct: {session.get('cardsCorrect')}")
                print(
                    f"   Time spent: {session.get('timeSpentMs')/1000/60:.1f} minutes"
                )
                print(f"   ID: {session.get('id')}")
                print("")

            choice = input(
                "Select a session number for details (or press Enter to skip): "
            )
            if choice.isdigit() and 1 <= int(choice) <= len(result["content"]):
                session_id = result["content"][int(choice) - 1].get("id")
                self.get_study_session_details(session_id)

    def get_study_session_details(self, session_id=None):
        self.print_header("Get Study Session Details")
        if not self.access_token:
            print("You must be logged in to perform this action.")
            return

        if not session_id:
            session_id = input("Enter session ID: ")

        response = requests.get(
            f"{self.base_url}/api/study-sessions/{session_id}",
            headers=self.get_headers(),
        )

        self.print_response(response)

    def get_study_activity(self):
        self.print_header("Get Study Activity Statistics")
        if not self.access_token:
            print("You must be logged in to perform this action.")
            return

        days = input("Enter number of days to analyze (default: 7): ")
        if not days.isdigit():
            days = "7"

        response = requests.get(
            f"{self.base_url}/api/stats/study-activity?days={days}",
            headers=self.get_headers(),
        )

        self.print_response(response)

    # Flow 8: Card Review History
    def get_card_review_history(self):
        self.print_header("Get Card Review History")
        if not self.access_token:
            print("You must be logged in to perform this action.")
            return

        if not self.current_card_ids:
            print("No cards available. Please get cards in the deck first.")
            return

        print("Available cards:")
        for i, card_id in enumerate(self.current_card_ids):
            print(f"{i+1}. Card ID: {card_id}")

        choice = input("\nSelect a card number: ")
        if (
            not choice.isdigit()
            or int(choice) < 1
            or int(choice) > len(self.current_card_ids)
        ):
            print("Invalid selection.")
            return

        card_id = self.current_card_ids[int(choice) - 1]
        response = requests.get(
            f"{self.base_url}/api/cards/{card_id}/reviews", headers=self.get_headers()
        )

        self.print_response(response)

    # Flow 10: Deck Browsing & Search
    def search_decks(self):
        self.print_header("Search Decks")
        if not self.access_token:
            print("You must be logged in to perform this action.")
            return

        query = input("Enter search term: ")
        if not query.strip():
            print("Search term cannot be empty.")
            return

        response = requests.get(
            f"{self.base_url}/api/decks?search={query}", headers=self.get_headers()
        )

        result = self.print_response(response)
        if result and isinstance(result, list) and len(result) > 0:
            print("\nSearch results:")
            for i, deck in enumerate(result):
                print(f"{i+1}. {deck.get('name')} (ID: {deck.get('id')})")

            choice = input(
                "\nSelect a deck number to set as current (or press Enter to skip): "
            )
            if choice.isdigit() and 1 <= int(choice) <= len(result):
                self.current_deck_id = result[int(choice) - 1].get("id")
                print(f"Current deck set to: {result[int(choice)-1].get('name')}")

    def run_flow(self, flow_id):
        flows = {
            1: self.flow_user_registration_verification,
            2: self.flow_login_authentication,
            3: self.flow_password_recovery,
            4: self.flow_deck_creation_management,
            5: self.flow_card_creation_management,
            6: self.flow_study_session,
            7: self.flow_performance_tracking,
            8: self.flow_card_review_history,
            9: self.flow_multiple_device_management,
            10: self.flow_deck_browsing_search,
            11: self.flow_spaced_repetition_learning,
            12: self.flow_study_session_analysis,
        }

        if flow_id in flows:
            flows[flow_id]()
        else:
            print(f"Invalid flow ID: {flow_id}")

    # Flow implementations
    def flow_user_registration_verification(self):
        self.print_header("Flow 1: User Registration & Verification")
        print("This flow demonstrates user registration and email verification.")
        print("Steps:")
        print("1. Register a new user")
        print("2. Verify the user's email with the verification token")

        choice = input("\nSelect step (1-2) or press Enter to go through entire flow: ")

        if not choice.strip() or choice == "1":
            self.register_user()
        if not choice.strip() or choice == "2":
            self.verify_email()

    def flow_login_authentication(self):
        self.print_header("Flow 2: Login & Authentication")
        print("This flow demonstrates user login and authentication token management.")
        print("Steps:")
        print("1. Login with credentials")
        print("2. Refresh authentication token")
        print("3. Logout")

        choice = input("\nSelect step (1-3) or press Enter to go through entire flow: ")

        if not choice.strip() or choice == "1":
            self.login()
        if not choice.strip() or choice == "2":
            self.refresh_auth_token()
        if not choice.strip() or choice == "3":
            self.logout()

    def flow_password_recovery(self):
        self.print_header("Flow 3: Password Recovery")
        print("This flow demonstrates the password recovery process.")
        print("Steps:")
        print("1. Request password reset")
        print("2. Reset password with token")

        choice = input("\nSelect step (1-2) or press Enter to go through entire flow: ")

        if not choice.strip() or choice == "1":
            self.forgot_password()
        if not choice.strip() or choice == "2":
            self.reset_password()

    def flow_deck_creation_management(self):
        self.print_header("Flow 4: Deck Creation & Management")
        print("This flow demonstrates creating and managing flashcard decks.")
        print("Steps:")
        print("1. View all decks")
        print("2. Create a new deck")
        print("3. View deck details")
        print("4. Update deck information")
        print("5. Delete deck")

        choice = input("\nSelect step (1-5) or press Enter to go through entire flow: ")

        if not choice.strip() or choice == "1":
            self.get_all_decks()
        if not choice.strip() or choice == "2":
            self.create_deck()
        if not choice.strip() or choice == "3":
            self.get_deck_details()
        if not choice.strip() or choice == "4":
            self.update_deck()
        if not choice.strip() or choice == "5":
            self.delete_deck()

    def flow_card_creation_management(self):
        self.print_header("Flow 5: Card Creation & Management")
        print("This flow demonstrates creating and managing flashcards within a deck.")
        print("Steps:")
        print("1. View all cards in a deck")
        print("2. Create a new card")
        print("3. View card details")
        print("4. Update card information")
        print("5. Delete card")

        choice = input("\nSelect step (1-5) or press Enter to go through entire flow: ")

        if not choice.strip() or choice == "1":
            self.get_cards_in_deck()
        if not choice.strip() or choice == "2":
            self.create_card()
        if not choice.strip() or choice == "3":
            self.get_card_details()
        if not choice.strip() or choice == "4":
            self.update_card()
        if not choice.strip() or choice == "5":
            self.delete_card()

    def flow_study_session(self):
        self.print_header("Flow 6: Study Session")
        print("This flow demonstrates conducting a study session with flashcards.")
        print("Steps:")
        print("1. Get cards due for review")
        print("2. Start a study session")
        print("3. Submit card reviews")
        print("4. Complete the study session")

        choice = input("\nSelect step (1-4) or press Enter to go through entire flow: ")

        if not choice.strip() or choice == "1":
            self.get_cards_for_review()
        if not choice.strip() or choice == "2":
            self.start_study_session()
        # Steps 3 and 4 are handled automatically within start_study_session

    def flow_performance_tracking(self):
        self.print_header("Flow 7: Performance Tracking")
        print("This flow demonstrates tracking study performance and statistics.")
        print("Steps:")
        print("1. View all study sessions")
        print("2. View study activity statistics")

        choice = input("\nSelect step (1-2) or press Enter to go through entire flow: ")

        if not choice.strip() or choice == "1":
            self.get_study_sessions()
        if not choice.strip() or choice == "2":
            self.get_study_activity()

    def flow_card_review_history(self):
        self.print_header("Flow 8: Card Review History")
        print("This flow demonstrates viewing the review history for specific cards.")
        print("Steps:")
        print("1. Select a card and view its review history")

        self.get_card_review_history()

    def flow_multiple_device_management(self):
        self.print_header("Flow 9: Multiple Device Management")
        print("This flow demonstrates managing sessions across devices.")
        print("Steps:")
        print("1. Logout from current device")
        print("2. Logout from all devices")

        choice = input("\nSelect step (1-2) or press Enter to go through entire flow: ")

        if not choice.strip() or choice == "1":
            self.logout()
        if not choice.strip() or choice == "2":
            self.logout_all_devices()

    def flow_deck_browsing_search(self):
        self.print_header("Flow 10: Deck Browsing & Search")
        print("This flow demonstrates browsing and searching for decks.")
        print("Steps:")
        print("1. View all decks")
        print("2. Search for decks by keyword")

        choice = input("\nSelect step (1-2) or press Enter to go through entire flow: ")

        if not choice.strip() or choice == "1":
            self.get_all_decks()
        if not choice.strip() or choice == "2":
            self.search_decks()

    def flow_spaced_repetition_learning(self):
        self.print_header("Flow 11: Spaced Repetition Learning")
        print("This flow demonstrates the spaced repetition learning algorithm.")
        print("Steps:")
        print("1. Get cards due for review based on spaced repetition algorithm")
        print("2. Start a study session")
        print("3. Complete reviews and see next review dates")

        choice = input("\nSelect step (1-3) or press Enter to go through entire flow: ")

        if not choice.strip() or choice == "1":
            self.get_cards_for_review()
        if not choice.strip() or choice == "2" or not choice.strip():
            self.start_study_session()
        # Step 3 is handled automatically within start_study_session

    def flow_study_session_analysis(self):
        self.print_header("Flow 12: Study Session Analysis")
        print("This flow demonstrates analyzing details of past study sessions.")
        print("Steps:")
        print("1. View all study sessions")
        print("2. Select a session to view detailed analysis")

        self.get_study_sessions()
        # Step 2 is handled within get_study_sessions if user selects a session


def main():
    tester = FlashcardAPITester()

    # Welcome message
    tester.clear_terminal()
    tester.print_header("FLASHCARD API TESTER")
    print("\nWelcome to the Flashcard API Tester!")
    print(
        "This tool helps you test the different API flows for the Flashcard application."
    )

    # Set base URL
    default_url = "http://localhost:3000"
    url = input(f"Enter API base URL (default: {default_url}): ")
    if not url:
        url = default_url
    tester.set_base_url(url)

    # Check API health
    print("\nChecking API health...")
    tester.check_api_health()

    while True:
        tester.print_header("MAIN MENU")
        print("Available Flows to Test:")
        print("1. User Registration & Verification")
        print("2. Login & Authentication")
        print("3. Password Recovery")
        print("4. Deck Creation & Management")
        print("5. Card Creation & Management")
        print("6. Study Session")
        print("7. Performance Tracking")
        print("8. Card Review History")
        print("9. Multiple Device Management")
        print("10. Deck Browsing & Search")
        print("11. Spaced Repetition Learning")
        print("12. Study Session Analysis")
        print("\nUtilities:")
        print("h. Check API Health")
        print("q. Quit")

        choice = input("\nSelect an option: ")

        if choice == "q":
            print("Exiting...")
            break
        elif choice == "h":
            tester.check_api_health()
        elif choice.isdigit() and 1 <= int(choice) <= 12:
            tester.run_flow(int(choice))
        else:
            print("Invalid choice. Please try again.")

        input("\nPress Enter to continue...")
        tester.clear_terminal()


if __name__ == "__main__":
    main()
