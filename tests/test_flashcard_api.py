#!/usr/bin/env python3
import requests
import json
import time
import os
import getpass
from datetime import datetime
import webbrowser
import random
import string
import argparse

# Parse command-line arguments
parser = argparse.ArgumentParser(description="Test the Flashcard API")
parser.add_argument(
    "--port", type=int, default=3000, help="API server port (default: 3000)"
)
parser.add_argument(
    "--mailhog-port", type=int, default=8025, help="MailHog web UI port (default: 8025)"
)
args = parser.parse_args()

# Configuration
BASE_URL = f"http://localhost:{args.port}"
MAILHOG_URL = f"http://localhost:{args.mailhog_port}"  # MailHog web interface
AUTH_TOKEN = None
REFRESH_TOKEN = None
USER_EMAIL = None
USER_PASSWORD = None
USER_ID = None
TEST_DECK_ID = None
TEST_CARD_ID = None
TEST_SESSION_ID = None


# Color codes for terminal output
class Colors:
    HEADER = "\033[95m"
    BLUE = "\033[94m"
    GREEN = "\033[92m"
    YELLOW = "\033[93m"
    RED = "\033[91m"
    ENDC = "\033[0m"
    BOLD = "\033[1m"
    UNDERLINE = "\033[4m"


def print_header(text):
    """Print a formatted header"""
    print(f"\n{Colors.HEADER}{Colors.BOLD}{'='*80}\n{text}\n{'='*80}{Colors.ENDC}")


def print_info(text):
    """Print info text"""
    print(f"{Colors.BLUE}[INFO] {text}{Colors.ENDC}")


def print_success(text):
    """Print success text"""
    print(f"{Colors.GREEN}[SUCCESS] {text}{Colors.ENDC}")


def print_warning(text):
    """Print warning text"""
    print(f"{Colors.YELLOW}[WARNING] {text}{Colors.ENDC}")


def print_error(text):
    """Print error text"""
    print(f"{Colors.RED}[ERROR] {text}{Colors.ENDC}")


def print_json(data):
    """Print formatted JSON data"""
    print(json.dumps(data, indent=2))


def generate_random_string(length=8):
    """Generate a random string of fixed length"""
    letters = string.ascii_lowercase
    return "".join(random.choice(letters) for i in range(length))


def save_auth_tokens(response_data):
    """Save authentication tokens"""
    global AUTH_TOKEN, REFRESH_TOKEN
    AUTH_TOKEN = response_data.get("accessToken")
    REFRESH_TOKEN = response_data.get("refreshToken")
    print_info("Authentication tokens saved")


def get_auth_header():
    """Get authorization header"""
    global AUTH_TOKEN
    if not AUTH_TOKEN:
        print_error("No authentication token available. Please log in first.")
        return None
    return {"Authorization": f"Bearer {AUTH_TOKEN}"}


def handle_response(response, success_msg=None):
    """Handle API response with proper output"""
    try:
        data = response.json()
    except:
        data = {"text": response.text}

    if 200 <= response.status_code < 300:
        if success_msg:
            print_success(success_msg)
        print_info(f"Status: {response.status_code}")
        print_json(data)
        return data
    else:
        print_error(f"Request failed with status: {response.status_code}")
        print_json(data)
        return None


def open_mailhog():
    """Open MailHog in the default web browser"""
    print_info(f"Opening MailHog at {MAILHOG_URL}")
    try:
        webbrowser.open(MAILHOG_URL)
        print_info("Please check the verification email in MailHog")
    except:
        print_warning(
            f"Failed to open browser. Please manually navigate to {MAILHOG_URL}"
        )


def test_health_check():
    """Test the health check endpoint"""
    print_header("Testing Health Check Endpoint")
    response = requests.get(f"{BASE_URL}/health")
    return handle_response(response, "Health check successful")


def test_register_user():
    """Test user registration"""
    global USER_EMAIL, USER_PASSWORD, USER_ID

    print_header("Testing User Registration")

    # Generate random username and email for testing
    random_suffix = generate_random_string()
    username = f"testuser_{random_suffix}"
    USER_EMAIL = f"testuser_{random_suffix}@example.com"
    USER_PASSWORD = f"Password123!{random_suffix}"

    print_info(f"Registering user: {username} with email: {USER_EMAIL}")

    data = {"username": username, "email": USER_EMAIL, "password": USER_PASSWORD}

    response = requests.post(f"{BASE_URL}/api/auth/signup", json=data)
    result = handle_response(response, "User registration request sent")

    if result:
        USER_ID = result.get("userId")

        print_info("\nA verification email has been sent to your inbox.")
        input(
            f"{Colors.YELLOW}Press Enter to open MailHog to check the verification email...{Colors.ENDC}"
        )
        open_mailhog()

        print_info("Look for the verification token in the email sent to your address")
        print_info("The token might be in the form of a URL parameter or a code")

    return result


def test_verify_email():
    """Test email verification"""
    print_header("Testing Email Verification")

    token = input("Enter the verification token from the email: ").strip()

    data = {"token": token}

    response = requests.post(f"{BASE_URL}/api/auth/verify-email", json=data)
    return handle_response(response, "Email verification request sent")


def test_login():
    """Test user login"""
    global USER_EMAIL, USER_PASSWORD

    print_header("Testing User Login")

    if not USER_EMAIL or not USER_PASSWORD:
        USER_EMAIL = input("Enter your email: ").strip()
        USER_PASSWORD = getpass.getpass("Enter your password: ")

    data = {"email": USER_EMAIL, "password": USER_PASSWORD}

    response = requests.post(f"{BASE_URL}/api/auth/login", json=data)
    result = handle_response(response, "Login request sent")

    if result:
        save_auth_tokens(result)

    return result


def test_refresh_token():
    """Test token refresh"""
    global REFRESH_TOKEN

    print_header("Testing Refresh Token")

    if not REFRESH_TOKEN:
        print_error("No refresh token available. Please log in first.")
        return None

    data = {"refreshToken": REFRESH_TOKEN}

    response = requests.post(f"{BASE_URL}/api/auth/refresh", json=data)
    result = handle_response(response, "Token refresh request sent")

    if result:
        AUTH_TOKEN = result.get("accessToken")
        print_info("Access token updated")

    return result


def test_create_deck():
    """Test creating a new deck"""
    global TEST_DECK_ID

    print_header("Testing Create Deck")

    auth_header = get_auth_header()
    if not auth_header:
        return None

    deck_name = f"Test Deck {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}"

    data = {
        "name": deck_name,
        "description": "A test deck created by the API test script",
    }

    response = requests.post(f"{BASE_URL}/api/decks", json=data, headers=auth_header)
    result = handle_response(response, "Create deck request sent")

    if result:
        TEST_DECK_ID = result.get("id")
        print_info(f"Created deck with ID: {TEST_DECK_ID}")

    return result


def test_get_decks():
    """Test getting all decks"""
    print_header("Testing Get All Decks")

    auth_header = get_auth_header()
    if not auth_header:
        return None

    response = requests.get(f"{BASE_URL}/api/decks", headers=auth_header)
    return handle_response(response, "Get all decks request sent")


def test_create_card():
    """Test creating a new card"""
    global TEST_DECK_ID, TEST_CARD_ID

    print_header("Testing Create Card")

    if not TEST_DECK_ID:
        print_error("No test deck ID available. Please create a deck first.")
        return None

    auth_header = get_auth_header()
    if not auth_header:
        return None

    data = {
        "front": f"Test Front {datetime.now().strftime('%H:%M:%S')}",
        "back": f"Test Back {datetime.now().strftime('%H:%M:%S')}",
        "notes": "A test card created by the API test script",
    }

    response = requests.post(
        f"{BASE_URL}/api/decks/{TEST_DECK_ID}/cards", json=data, headers=auth_header
    )
    result = handle_response(response, "Create card request sent")

    if result:
        TEST_CARD_ID = result.get("id")
        print_info(f"Created card with ID: {TEST_CARD_ID}")

    return result


def test_get_cards():
    """Test getting cards in a deck"""
    global TEST_DECK_ID

    print_header("Testing Get Cards in Deck")

    if not TEST_DECK_ID:
        print_error("No test deck ID available. Please create a deck first.")
        return None

    auth_header = get_auth_header()
    if not auth_header:
        return None

    response = requests.get(
        f"{BASE_URL}/api/decks/{TEST_DECK_ID}/cards", headers=auth_header
    )
    return handle_response(response, "Get cards request sent")


def test_start_study_session():
    """Test starting a study session"""
    global TEST_DECK_ID, TEST_SESSION_ID

    print_header("Testing Start Study Session")

    if not TEST_DECK_ID:
        print_error("No test deck ID available. Please create a deck first.")
        return None

    auth_header = get_auth_header()
    if not auth_header:
        return None

    response = requests.post(
        f"{BASE_URL}/api/decks/{TEST_DECK_ID}/study-sessions", headers=auth_header
    )
    result = handle_response(response, "Start study session request sent")

    if result:
        TEST_SESSION_ID = result.get("sessionId")
        print_info(f"Started study session with ID: {TEST_SESSION_ID}")

    return result


def test_complete_study_session():
    """Test completing a study session"""
    global TEST_SESSION_ID

    print_header("Testing Complete Study Session")

    if not TEST_SESSION_ID:
        print_error("No test session ID available. Please start a study session first.")
        return None

    auth_header = get_auth_header()
    if not auth_header:
        return None

    data = {
        "cardsReviewed": 2,
        "correctResponses": 1,
        "incorrectResponses": 1,
        "totalTimeSeconds": 60,
    }

    response = requests.put(
        f"{BASE_URL}/api/study-sessions/{TEST_SESSION_ID}/complete",
        json=data,
        headers=auth_header,
    )
    return handle_response(response, "Complete study session request sent")


def test_forget_password():
    """Test password reset request"""
    print_header("Testing Password Reset Request")

    email = input("Enter the email address to reset password for: ").strip()

    data = {"email": email}

    response = requests.post(f"{BASE_URL}/api/auth/forgot-password", json=data)
    result = handle_response(response, "Password reset request sent")

    if result:
        print_info("\nA password reset email should have been sent.")
        input(
            f"{Colors.YELLOW}Press Enter to open MailHog to check the password reset email...{Colors.ENDC}"
        )
        open_mailhog()

        print_info("Look for the password reset token in the email")

    return result


def test_reset_password():
    """Test password reset"""
    print_header("Testing Password Reset")

    token = input("Enter the password reset token from the email: ").strip()
    new_password = getpass.getpass("Enter new password: ")

    data = {"token": token, "newPassword": new_password}

    response = requests.post(f"{BASE_URL}/api/auth/reset-password", json=data)
    return handle_response(response, "Password reset request sent")


def test_logout():
    """Test user logout"""
    global REFRESH_TOKEN

    print_header("Testing User Logout")

    if not REFRESH_TOKEN:
        print_error("No refresh token available. Please log in first.")
        return None

    data = {"refreshToken": REFRESH_TOKEN}

    response = requests.post(f"{BASE_URL}/api/auth/logout", json=data)
    result = handle_response(response, "Logout request sent")

    if result:
        AUTH_TOKEN = None
        REFRESH_TOKEN = None
        print_info("Logged out successfully, tokens cleared")

    return result


def main_menu():
    """Display the main menu for API testing"""
    while True:
        print_header("Flashcard API Test Menu")

        print(f"{Colors.BOLD}Authentication Tests:{Colors.ENDC}")
        print("1.  Test Health Check")
        print("2.  Register a New User")
        print("3.  Verify Email")
        print("4.  Login")
        print("5.  Refresh Token")
        print("6.  Request Password Reset")
        print("7.  Reset Password")
        print("8.  Logout")

        print(f"\n{Colors.BOLD}Deck Management:{Colors.ENDC}")
        print("9.  Get All Decks")
        print("10. Create New Deck")
        print("11. Get Deck Details")

        print(f"\n{Colors.BOLD}Card Management:{Colors.ENDC}")
        print("12. Create New Card")
        print("13. Get Cards in Deck")
        print("14. Get Card Details")

        print(f"\n{Colors.BOLD}Study Session Management:{Colors.ENDC}")
        print("15. Start Study Session")
        print("16. Complete Study Session")
        print("17. Submit Card Review")

        print(f"\n{Colors.BOLD}System:{Colors.ENDC}")
        print("0.  Exit")

        choice = input("\nEnter your choice (0-17): ")

        try:
            choice = int(choice)

            if choice == 0:
                print_info("Exiting the test script")
                break

            # Authentication Tests
            elif choice == 1:
                test_health_check()
            elif choice == 2:
                test_register_user()
            elif choice == 3:
                test_verify_email()
            elif choice == 4:
                test_login()
            elif choice == 5:
                test_refresh_token()
            elif choice == 6:
                test_forget_password()
            elif choice == 7:
                test_reset_password()
            elif choice == 8:
                test_logout()

            # Deck Management
            elif choice == 9:
                test_get_decks()
            elif choice == 10:
                test_create_deck()
            elif choice == 11:
                if TEST_DECK_ID:
                    print_header("Testing Get Deck Details")
                    auth_header = get_auth_header()
                    if auth_header:
                        response = requests.get(
                            f"{BASE_URL}/api/decks/{TEST_DECK_ID}", headers=auth_header
                        )
                        handle_response(response, "Get deck details request sent")
                else:
                    print_error(
                        "No test deck ID available. Please create a deck first."
                    )

            # Card Management
            elif choice == 12:
                test_create_card()
            elif choice == 13:
                test_get_cards()
            elif choice == 14:
                if TEST_CARD_ID and TEST_DECK_ID:
                    print_header("Testing Get Card Details")
                    auth_header = get_auth_header()
                    if auth_header:
                        response = requests.get(
                            f"{BASE_URL}/api/decks/{TEST_DECK_ID}/cards/{TEST_CARD_ID}",
                            headers=auth_header,
                        )
                        handle_response(response, "Get card details request sent")
                else:
                    print_error(
                        "No test card ID available. Please create a card first."
                    )

            # Study Session Management
            elif choice == 15:
                test_start_study_session()
            elif choice == 16:
                test_complete_study_session()
            elif choice == 17:
                if TEST_CARD_ID and TEST_SESSION_ID:
                    print_header("Testing Submit Card Review")
                    auth_header = get_auth_header()
                    if auth_header:
                        data = {
                            "card": {"id": TEST_CARD_ID},
                            "result": 4,
                            "timeSpentSeconds": 5,
                        }
                        response = requests.post(
                            f"{BASE_URL}/api/study-sessions/{TEST_SESSION_ID}/reviews",
                            json=data,
                            headers=auth_header,
                        )
                        handle_response(response, "Submit card review request sent")
                else:
                    print_error(
                        "Need both a test card ID and session ID. Please create them first."
                    )

            else:
                print_warning("Invalid choice. Please try again.")

        except ValueError:
            print_error("Please enter a number.")

        # Pause before returning to the menu
        input(f"\n{Colors.YELLOW}Press Enter to return to the menu...{Colors.ENDC}")


if __name__ == "__main__":
    print_header("Flashcard API Test Script")
    print_info(f"Testing API at {BASE_URL}")
    print_info(f"MailHog is expected at {MAILHOG_URL}")

    try:
        # Test if the API is up and running
        health = test_health_check()
        if health:
            main_menu()
        else:
            print_error(
                f"Failed to connect to the API at {BASE_URL}. Make sure the server is running."
            )
    except requests.exceptions.ConnectionError:
        print_error(
            f"Failed to connect to the API at {BASE_URL}. Make sure the server is running."
        )
    except KeyboardInterrupt:
        print_info("\nExiting the test script")
