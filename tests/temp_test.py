#!/usr/bin/env python3
import requests
import json
import time
import random
import string
from datetime import datetime


# Color codes for terminal output
class Colors:
    HEADER = "\033[95m"
    BLUE = "\033[94m"
    GREEN = "\033[92m"
    YELLOW = "\033[93m"
    RED = "\033[91m"
    ENDC = "\033[0m"
    BOLD = "\033[1m"


# Configuration
BASE_URL = "http://localhost:3000"
AUTH_TOKEN = None
REFRESH_TOKEN = None
USER_USERNAME = None
USER_PASSWORD = None
TEST_DECK_ID = None


def print_header(text):
    print(f"\n{Colors.HEADER}{Colors.BOLD}{'='*80}\n{text}\n{'='*80}{Colors.ENDC}")


def print_info(text):
    print(f"{Colors.BLUE}[INFO] {text}{Colors.ENDC}")


def print_success(text):
    print(f"{Colors.GREEN}[SUCCESS] {text}{Colors.ENDC}")


def print_warning(text):
    print(f"{Colors.YELLOW}[WARNING] {text}{Colors.ENDC}")


def print_error(text):
    print(f"{Colors.RED}[ERROR] {text}{Colors.ENDC}")


def print_json(data):
    print(json.dumps(data, indent=2))


def generate_random_string(length=8):
    letters = string.ascii_lowercase
    return "".join(random.choice(letters) for i in range(length))


def handle_response(response, success_msg=None):
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


def save_auth_tokens(response_data):
    global AUTH_TOKEN, REFRESH_TOKEN
    AUTH_TOKEN = response_data.get("accessToken")
    REFRESH_TOKEN = response_data.get("refreshToken")
    print_info("Authentication tokens saved")


def get_auth_header():
    global AUTH_TOKEN
    if not AUTH_TOKEN:
        print_error("No authentication token available. Please log in first.")
        return None
    return {"Authorization": f"Bearer {AUTH_TOKEN}"}


def test_health_check():
    print_header("Testing Health Check Endpoint")
    response = requests.get(f"{BASE_URL}/health")
    return handle_response(response, "Health check successful")


def test_register_user():
    global USER_USERNAME, USER_PASSWORD

    print_header("Testing User Registration")

    # Generate random username and email for testing
    random_suffix = generate_random_string()
    USER_USERNAME = f"testuser_{random_suffix}"
    USER_EMAIL = f"testuser_{random_suffix}@example.com"
    USER_PASSWORD = f"Password123!{random_suffix}"

    print_info(f"Registering user: {USER_USERNAME} with email: {USER_EMAIL}")

    data = {"username": USER_USERNAME, "email": USER_EMAIL, "password": USER_PASSWORD}

    response = requests.post(f"{BASE_URL}/api/auth/signup", json=data)
    result = handle_response(response, "User registration request sent")

    if result:
        print_info(f"Test username: {USER_USERNAME}")
        print_info(f"Test password: {USER_PASSWORD}")

    return result


def test_verify_email(token):
    print_header("Testing Email Verification")

    data = {"token": token}

    response = requests.post(f"{BASE_URL}/api/auth/verify-email", json=data)
    return handle_response(response, "Email verification request sent")


def test_login():
    global USER_USERNAME, USER_PASSWORD

    print_header("Testing User Login")

    if not USER_USERNAME:
        USER_USERNAME = input("Enter your username: ").strip()
        USER_PASSWORD = input("Enter your password: ")

    data = {"username": USER_USERNAME, "password": USER_PASSWORD}

    response = requests.post(f"{BASE_URL}/api/auth/login", json=data)
    result = handle_response(response, "Login request sent")

    if result:
        save_auth_tokens(result)

    return result


def test_refresh_token():
    global REFRESH_TOKEN

    print_header("Testing Refresh Token")

    if not REFRESH_TOKEN:
        REFRESH_TOKEN = input("Enter refresh token: ").strip()

    data = {"refreshToken": REFRESH_TOKEN}

    # Test with different timeout values to see if it's a timeout issue
    for timeout in [5, 10, 30]:
        print_info(f"Trying with {timeout} second timeout")
        try:
            response = requests.post(
                f"{BASE_URL}/api/auth/refresh", json=data, timeout=timeout
            )
            result = handle_response(response, "Token refresh request sent")

            if result:
                print_info(f"Success with {timeout}s timeout")
                return result
            else:
                print_warning(
                    f"Failed with {timeout}s timeout. Trying longer timeout..."
                )
        except requests.exceptions.Timeout:
            print_error(f"Request timed out after {timeout} seconds")
        except Exception as e:
            print_error(f"Error: {str(e)}")

    return None


def test_create_deck():
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


def test_create_card_with_different_configs():
    global TEST_DECK_ID

    print_header("Testing Create Card with Different Configurations")

    if not TEST_DECK_ID:
        print_info("No test deck ID available. Creating a deck first.")
        test_create_deck()
        if not TEST_DECK_ID:
            print_error("Failed to create a deck. Exiting test.")
            return None

    auth_header = get_auth_header()
    if not auth_header:
        return None

    print_info(f"Using deck ID: {TEST_DECK_ID}")

    data = {
        "front": f"Test Front {datetime.now().strftime('%H:%M:%S')}",
        "back": f"Test Back {datetime.now().strftime('%H:%M:%S')}",
        "notes": "A test card created by the API test script",
    }

    # First try the new simplified endpoint
    print_header("Testing with new simplified endpoint")
    try:
        print_info("Using the /simple endpoint which has lightweight responses")
        response = requests.post(
            f"{BASE_URL}/api/decks/{TEST_DECK_ID}/cards/simple",
            json=data,
            headers=auth_header,
            timeout=30,
        )
        result = handle_response(
            response, "Create card request sent to simplified endpoint"
        )

        if result:
            print_success("Success with simplified endpoint!")
            return result
    except Exception as e:
        print_error(f"Error with simplified endpoint: {str(e)}")

    # If the simplified endpoint doesn't work, try the original endpoint with different configurations
    print_info("Falling back to original endpoint with different configurations")

    # Test different request configurations
    configurations = [
        {"name": "Default Configuration", "options": {}},
        {"name": "With timeout", "options": {"timeout": 30}},
        {"name": "With stream=False", "options": {"stream": False}},
        {"name": "With stream=True", "options": {"stream": True}},
        {"name": "With both", "options": {"timeout": 30, "stream": False}},
    ]

    for config in configurations:
        print_header(f"Testing with {config['name']}")

        # Add retry logic for handling connection errors
        max_retries = 3
        retry_delay = 2
        success = False

        for attempt in range(max_retries):
            try:
                print_info(f"Attempt {attempt + 1}/{max_retries}")

                response = requests.post(
                    f"{BASE_URL}/api/decks/{TEST_DECK_ID}/cards",
                    json=data,
                    headers=auth_header,
                    **config["options"],
                )

                result = handle_response(response, "Create card request sent")

                if result:
                    print_success(
                        f"Success with {config['name']} on attempt {attempt + 1}"
                    )
                    success = True
                    break
                else:
                    print_error(
                        f"Failed with {config['name']} on attempt {attempt + 1}"
                    )

            except (
                requests.exceptions.ChunkedEncodingError,
                requests.exceptions.ConnectionError,
                requests.exceptions.Timeout,
            ) as e:
                print_warning(f"Connection error on attempt {attempt + 1}: {str(e)}")
                if attempt < max_retries - 1:
                    print_info(f"Retrying in {retry_delay} seconds...")
                    time.sleep(retry_delay)
                    retry_delay *= 2  # Exponential backoff
                else:
                    print_error(
                        f"Failed after {max_retries} attempts with {config['name']}"
                    )

            except Exception as e:
                print_error(f"Unexpected error: {str(e)}")
                break

        if success:
            print_success(f"{config['name']} was successful")
        else:
            print_error(f"{config['name']} failed")

        # Add delay between configurations to avoid overloading server
        time.sleep(2)


def run_focused_tests():
    print_header("Running Focused Tests for Problematic Endpoints")

    # Step 1: Check if server is up
    if not test_health_check():
        print_error("Server not available. Exiting.")
        return

    # Step 2: Register a new user or use existing
    use_existing = input("Use existing user? (y/n): ").strip().lower() == "y"

    if use_existing:
        USER_USERNAME = input("Enter existing username: ").strip()
        USER_PASSWORD = input("Enter existing password: ")
    else:
        test_register_user()
        token = input("Enter verification token from email: ").strip()
        test_verify_email(token)

    # Step 3: Login
    if not test_login():
        print_error("Failed to log in. Exiting.")
        return

    # Step 4: Test problematic endpoints
    while True:
        print_header("Problem Endpoint Test Menu")
        print("1. Test Token Refresh")
        print("2. Test Create Card with Different Configurations")
        print("3. Run Both Tests")
        print("0. Exit")

        choice = input("\nEnter your choice (0-3): ").strip()

        try:
            choice = int(choice)

            if choice == 0:
                break
            elif choice == 1:
                test_refresh_token()
            elif choice == 2:
                test_create_card_with_different_configs()
            elif choice == 3:
                test_refresh_token()
                test_create_card_with_different_configs()
            else:
                print_warning("Invalid choice. Please try again.")

        except ValueError:
            print_error("Please enter a number between 0 and 3.")

        input(f"\n{Colors.YELLOW}Press Enter to return to the menu...{Colors.ENDC}")


if __name__ == "__main__":
    print_header("Flashcard API Focused Test Script")
    print_info(f"Testing API at {BASE_URL}")
    print_info("This script specifically tests problematic endpoints:")
    print_info("1. Token Refresh (500 error)")
    print_info("2. Create Card (ChunkedEncodingError)")

    run_focused_tests()
