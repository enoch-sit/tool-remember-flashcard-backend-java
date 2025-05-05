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
import sys
import re
import socket

# Parse command-line arguments
parser = argparse.ArgumentParser(description="Test the Flashcard API")
parser.add_argument(
    "--port", type=int, default=3000, help="API server port (default: 3000)"
)
parser.add_argument(
    "--mailhog-port", type=int, default=8025, help="MailHog web UI port (default: 8025)"
)
parser.add_argument(
    "--mailhog-host",
    type=str,
    default="localhost",
    help="MailHog host (default: localhost, use 'mailhog' if inside Docker network)",
)
parser.add_argument(
    "--wait-email",
    type=int,
    default=5,
    help="Time to wait for emails to arrive in seconds (default: 5)",
)
parser.add_argument(
    "--auto", action="store_true", help="Run in automated mode with minimal user input"
)
parser.add_argument(
    "--auto-verify",
    action="store_true",
    help="Automatically extract verification token from MailHog (requires --auto)",
)
parser.add_argument(
    "--delay",
    type=int,
    default=2,
    help="Delay between tests in seconds when in auto mode (default: 2)",
)
parser.add_argument(
    "--token-tries",
    type=int,
    default=5,
    help="Number of attempts to extract tokens from emails (default: 5)",
)
args = parser.parse_args()

# Configuration
BASE_URL = f"http://localhost:{args.port}"
MAILHOG_URL = f"http://{args.mailhog_host}:{args.mailhog_port}"  # MailHog web interface
MAILHOG_API_URL = f"http://{args.mailhog_host}:{args.mailhog_port}/api/v2/messages"
AUTH_TOKEN = None
REFRESH_TOKEN = None
USER_EMAIL = None
USER_PASSWORD = None
USER_ID = None
USER_USERNAME = None
TEST_DECK_ID = None
TEST_CARD_ID = None
TEST_SESSION_ID = None


# Attempt to detect if we're running inside Docker
def is_in_docker():
    """Check if we're running inside Docker"""
    try:
        with open("/proc/1/cgroup", "r") as f:
            return any("docker" in line for line in f)
    except:
        return False


# If inside Docker, adjust URLs
if is_in_docker():
    MAILHOG_URL = "http://mailhog:8025"
    MAILHOG_API_URL = "http://mailhog:8025/api/v2/messages"
    print("Running inside Docker container, using internal Docker network URLs")


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
    if not args.auto:
        print_info(f"Opening MailHog at {MAILHOG_URL}")
        try:
            webbrowser.open(MAILHOG_URL)
            print_info("Please check the verification email in MailHog")
        except:
            print_warning(
                f"Failed to open browser. Please manually navigate to {MAILHOG_URL}"
            )


def extract_token_from_mailhog(email, token_type="verification"):
    """Extract a token from MailHog for the given email and token type"""
    print_info(f"Waiting for email to arrive in MailHog for {email}...")
    # Wait longer for emails to arrive
    time.sleep(args.wait_email)

    max_attempts = args.token_tries
    for attempt in range(max_attempts):
        try:
            print_info(
                f"Attempt {attempt+1}/{max_attempts} to fetch emails from MailHog"
            )
            # Try connecting to multiple potential MailHog addresses
            urls_to_try = [
                MAILHOG_API_URL,  # Try default URL first
                f"http://localhost:{args.mailhog_port}/api/v2/messages",  # Try localhost explicitly
                f"http://mailhog:{args.mailhog_port}/api/v2/messages",  # Try Docker service name
            ]

            for i, url in enumerate(urls_to_try):
                try:
                    print_info(f"Trying to connect to MailHog at {url}")
                    response = requests.get(url, timeout=5)
                    if response.status_code == 200:
                        print_success(f"Successfully connected to MailHog at {url}")
                        break
                except requests.RequestException as e:
                    print_warning(f"Could not connect to {url}: {str(e)}")
                    if i == len(urls_to_try) - 1:
                        print_error("Failed to connect to any MailHog instance")
                        time.sleep(2)
                        continue

            if response.status_code != 200:
                print_error(f"Failed to fetch emails: {response.status_code}")
                time.sleep(2)
                continue

            data = response.json()
            if not data or "items" not in data:
                print_error("Invalid response format from MailHog API")
                print_json(data)  # Print the response for debugging
                time.sleep(2)
                continue

            if not data["items"]:
                print_warning(f"No emails found in MailHog (attempt {attempt+1})")
                time.sleep(2)
                continue

            print_info(f"Found {len(data['items'])} emails in MailHog")

            # Find the most recent email for the given address
            for item in data["items"]:
                to_addresses = item.get("To", [])
                recipients = []

                # Handle both string and object formats for recipients
                for addr in to_addresses:
                    if isinstance(addr, dict):
                        recipient = (
                            f"{addr.get('Mailbox', '')}@{addr.get('Domain', '')}"
                        )
                        recipients.append(recipient)
                    elif isinstance(addr, str):
                        recipients.append(addr)

                print_info(f"Checking email to: {', '.join(recipients)}")

                # Check if our target email is in the recipients list
                email_found = any(email.lower() in addr.lower() for addr in recipients)
                # Also check with relaxed matching (just username part)
                username = email.split("@")[0] if "@" in email else email
                relaxed_match = any(
                    username.lower() in addr.lower() for addr in recipients
                )

                if email_found or relaxed_match:
                    # Extract email body
                    body = ""
                    content = item.get("Content", {})

                    # Try to get HTML body first
                    if "Body" in content and isinstance(content["Body"], list):
                        for part in content["Body"]:
                            body = part.get("Body", "")
                            if body:
                                break
                    # Fallback to direct Body field
                    elif "Body" in content and isinstance(content["Body"], str):
                        body = content["Body"]

                    if "MIME" in content:
                        print_info(f"Email MIME type: {content['MIME']}")

                    # Print a small part of the body for debugging
                    if body:
                        print_info(f"Email body sample: {body[:100]}...")
                    else:
                        print_warning("Email body is empty")

                    # Look for token in body
                    if token_type == "verification":
                        # Try multiple regex patterns to find the token
                        patterns = [
                            r"verification.*?token[^\w]+([\w\-]+)",  # Standard format
                            r"verify.*?token[^\w]+([\w\-]+)",  # Alternative wording
                            r"activate.*?token[^\w]+([\w\-]+)",  # Alternative wording
                            r'href=[\'"].*?token=([\w\-]+)',  # URL with token parameter
                            r"token[^\w]+([\w\-]+)",  # Generic token mention
                            r"code[^\w]+([\w\-]+)",  # Code instead of token
                            r'[\'"][a-zA-Z0-9\-]{20,}[\'"]',  # Long string in quotes (likely token)
                            r'[\'"][a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}[\'"]',  # UUID pattern
                        ]

                        for pattern in patterns:
                            token_match = re.search(pattern, body, re.IGNORECASE)
                            if token_match:
                                token = (
                                    token_match.group(1)
                                    if "(" in pattern
                                    else token_match.group(0).strip("'\"")
                                )
                                print_success(
                                    f"Found verification token using pattern '{pattern}': {token}"
                                )
                                return token

                    elif token_type == "reset":
                        # Similar patterns for reset tokens
                        patterns = [
                            r"reset.*?token[^\w]+([\w\-]+)",
                            r"reset.*?password.*?token[^\w]+([\w\-]+)",
                            r'href=[\'"].*?token=([\w\-]+)',
                            r"password.*?token[^\w]+([\w\-]+)",
                            r'[\'"][a-zA-Z0-9\-]{20,}[\'"]',
                            r'[\'"][a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}[\'"]',
                        ]

                        for pattern in patterns:
                            token_match = re.search(pattern, body, re.IGNORECASE)
                            if token_match:
                                token = (
                                    token_match.group(1)
                                    if "(" in pattern
                                    else token_match.group(0).strip("'\"")
                                )
                                print_success(f"Found reset token: {token}")
                                return token

            print_warning(f"No {token_type} token found for {email} in this batch")
            # Wait before trying again
            time.sleep(2)

        except Exception as e:
            print_error(f"Error extracting token (attempt {attempt+1}): {str(e)}")
            time.sleep(2)

    print_error(
        f"Failed to extract {token_type} token for {email} after {max_attempts} attempts"
    )
    return None


def test_verify_email():
    """Test email verification"""
    global USER_EMAIL

    print_header("Testing Email Verification")

    if args.auto and args.auto_verify and USER_EMAIL:
        print_info(
            f"Attempting to automatically extract verification token for {USER_EMAIL}"
        )
        token = extract_token_from_mailhog(USER_EMAIL, "verification")
        if not token:
            print_error("Could not automatically extract verification token")
            if not args.auto:
                token = input("Enter the verification token from the email: ").strip()
            else:
                # In full auto mode, let's try one more approach - open MailHog in browser
                # and give the user a chance to check manually
                print_warning(
                    "Auto extraction failed. Check MailHog manually at http://localhost:8025"
                )
                open_mailhog()
                if not args.auto:
                    token = input(
                        "Enter the verification token from the email: "
                    ).strip()
                else:
                    return None
        else:
            print_success(f"Found verification token: {token}")
    else:
        open_mailhog()
        token = input("Enter the verification token from the email: ").strip()

    data = {"token": token}

    response = requests.post(f"{BASE_URL}/api/auth/verify-email", json=data)
    return handle_response(response, "Email verification request sent")


def check_server_status():
    """Check if the server is running by pinging the health endpoint"""
    try:
        response = requests.get(f"{BASE_URL}/health", timeout=5)
        return 200 <= response.status_code < 300
    except:
        return False


def wait_for_server(max_attempts=10, delay=3):
    """Wait for the server to become available"""
    print_info(f"Checking if server is available at {BASE_URL}...")
    for attempt in range(max_attempts):
        if check_server_status():
            print_success("Server is running and ready for tests!")
            return True
        else:
            print_warning(
                f"Server not ready, waiting {delay} seconds... (attempt {attempt+1}/{max_attempts})"
            )
            time.sleep(delay)

    print_error(f"Server did not become available after {max_attempts} attempts")
    return False


def test_health_check():
    """Test the health check endpoint"""
    print_header("Testing Health Check Endpoint")
    response = requests.get(f"{BASE_URL}/health")
    return handle_response(response, "Health check successful")


def test_register_user():
    """Test user registration"""
    global USER_EMAIL, USER_PASSWORD, USER_ID, USER_USERNAME

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
        USER_ID = result.get("userId")

        if args.auto:
            print_info("\nA verification email has been sent.")
            if args.auto_verify:
                time.sleep(1)  # Wait for email to arrive
            else:
                open_mailhog()
        else:
            print_info("\nA verification email has been sent to your inbox.")
            input(
                f"{Colors.YELLOW}Press Enter to open MailHog to check the verification email...{Colors.ENDC}"
            )
            open_mailhog()

            print_info("Look for the verification token in the email")

    return result


def test_login():
    """Test user login"""
    global USER_EMAIL, USER_PASSWORD, USER_USERNAME

    print_header("Testing User Login")

    if not USER_USERNAME or not USER_PASSWORD:
        if args.auto:
            print_error("No user credentials available for automated login")
            return None
        USER_USERNAME = input("Enter your username: ").strip()
        USER_PASSWORD = getpass.getpass("Enter your password: ")

    data = {"username": USER_USERNAME, "password": USER_PASSWORD}

    response = requests.post(f"{BASE_URL}/api/auth/login", json=data)
    result = handle_response(response, "Login request sent")

    if result:
        save_auth_tokens(result)

    return result


def test_refresh_token():
    """Test token refresh"""
    global REFRESH_TOKEN, AUTH_TOKEN

    print_header("Testing Refresh Token")

    if not REFRESH_TOKEN:
        print_error("No refresh token available. Please log in first.")
        return None

    data = {"refreshToken": REFRESH_TOKEN}

    # Try with different timeout values to see if it helps with connection issues
    try:
        response = requests.post(f"{BASE_URL}/api/auth/refresh", json=data, timeout=30)
        result = handle_response(response, "Token refresh request sent")

        if result:
            AUTH_TOKEN = result.get("accessToken")
            print_info("Access token updated")

        return result
    except (requests.exceptions.ConnectionError, requests.exceptions.Timeout) as e:
        print_error(f"Connection error during token refresh: {str(e)}")
        return None
    except Exception as e:
        print_error(f"Unexpected error during token refresh: {str(e)}")
        return None


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


def test_get_deck_details():
    """Test get details for a specific deck"""
    global TEST_DECK_ID

    print_header("Testing Get Deck Details")

    if not TEST_DECK_ID:
        print_error("No test deck ID available. Please create a deck first.")
        return None

    auth_header = get_auth_header()
    if not auth_header:
        return None

    response = requests.get(f"{BASE_URL}/api/decks/{TEST_DECK_ID}", headers=auth_header)
    return handle_response(response, "Get deck details request sent")


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

    # First try the new simplified endpoint that avoids chunked encoding issues
    try:
        print_info("Attempting to use simplified card creation endpoint first")
        response = requests.post(
            f"{BASE_URL}/api/decks/{TEST_DECK_ID}/cards/simple",
            json=data,
            headers=auth_header,
            timeout=30,
        )
        result = handle_response(
            response, "Create card request sent (simplified endpoint)"
        )

        if result:
            TEST_CARD_ID = result.get("id")
            print_info(f"Created card with ID: {TEST_CARD_ID}")
            print_success("Successfully used simplified endpoint")
            return result

    except Exception as e:
        print_warning(
            f"Simplified endpoint failed: {str(e)}. Falling back to standard endpoint."
        )

    # Fall back to the original endpoint with retry logic if the simplified endpoint fails
    print_info("Using standard endpoint with retry logic")

    # Add retry logic for handling connection errors
    max_retries = 3
    retry_delay = 2
    for attempt in range(max_retries):
        try:
            response = requests.post(
                f"{BASE_URL}/api/decks/{TEST_DECK_ID}/cards",
                json=data,
                headers=auth_header,
                timeout=30,  # Add a timeout to prevent indefinite hanging
            )
            result = handle_response(response, "Create card request sent")

            if result:
                TEST_CARD_ID = result.get("id")
                print_info(f"Created card with ID: {TEST_CARD_ID}")
                return result

        except (
            requests.exceptions.ChunkedEncodingError,
            requests.exceptions.ConnectionError,
            requests.exceptions.Timeout,
        ) as e:
            if attempt < max_retries - 1:
                print_warning(f"Connection error on attempt {attempt + 1}: {str(e)}")
                print_info(f"Retrying in {retry_delay} seconds...")
                time.sleep(retry_delay)
                retry_delay *= 2  # Exponential backoff
            else:
                print_error(
                    f"Failed to create card after {max_retries} attempts: {str(e)}"
                )
                return None


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

    # First try the new simplified endpoint that avoids chunked encoding issues
    try:
        print_info("Attempting to use simplified endpoint for retrieving cards")
        response = requests.get(
            f"{BASE_URL}/api/decks/{TEST_DECK_ID}/cards/simple",
            headers=auth_header,
            timeout=30,
        )
        result = handle_response(
            response, "Get cards request sent (simplified endpoint)"
        )
        if result:
            print_success("Successfully used simplified endpoint")
            return result
    except Exception as e:
        print_warning(
            f"Simplified endpoint failed: {str(e)}. Falling back to standard endpoint."
        )

    # Fall back to the original endpoint with retry logic
    print_info("Using standard endpoint with retry logic")

    # Add retry logic for handling connection errors
    max_retries = 3
    retry_delay = 2
    for attempt in range(max_retries):
        try:
            response = requests.get(
                f"{BASE_URL}/api/decks/{TEST_DECK_ID}/cards",
                headers=auth_header,
                timeout=30,
            )
            return handle_response(response, "Get cards request sent")
        except (
            requests.exceptions.ChunkedEncodingError,
            requests.exceptions.ConnectionError,
            requests.exceptions.Timeout,
        ) as e:
            if attempt < max_retries - 1:
                print_warning(f"Connection error on attempt {attempt + 1}: {str(e)}")
                print_info(f"Retrying in {retry_delay} seconds...")
                time.sleep(retry_delay)
                retry_delay *= 2  # Exponential backoff
            else:
                print_error(
                    f"Failed to get cards after {max_retries} attempts: {str(e)}"
                )
                return None


def test_get_card_details():
    """Test get details for a specific card"""
    global TEST_DECK_ID, TEST_CARD_ID

    print_header("Testing Get Card Details")

    if not TEST_CARD_ID or not TEST_DECK_ID:
        print_error("Card ID or Deck ID not available. Please create them first.")
        return None

    auth_header = get_auth_header()
    if not auth_header:
        return None

    # First try the simplified endpoint that avoids chunked encoding issues
    try:
        print_info("Attempting to use simplified card details endpoint")
        response = requests.get(
            f"{BASE_URL}/api/decks/{TEST_DECK_ID}/cards/{TEST_CARD_ID}/simple",
            headers=auth_header,
            timeout=30,
        )
        result = handle_response(
            response, "Get card details request sent (simplified endpoint)"
        )
        if result:
            print_success("Successfully used simplified endpoint")
            return result
    except Exception as e:
        print_warning(
            f"Simplified endpoint failed: {str(e)}. Falling back to standard endpoint."
        )

    # Fall back to the original endpoint with retry logic
    print_info("Using standard endpoint with retry logic")

    # Add retry logic for handling connection errors
    max_retries = 3
    retry_delay = 2
    for attempt in range(max_retries):
        try:
            response = requests.get(
                f"{BASE_URL}/api/decks/{TEST_DECK_ID}/cards/{TEST_CARD_ID}",
                headers=auth_header,
                timeout=30,
            )
            return handle_response(response, "Get card details request sent")
        except (
            requests.exceptions.ChunkedEncodingError,
            requests.exceptions.ConnectionError,
            requests.exceptions.Timeout,
        ) as e:
            if attempt < max_retries - 1:
                print_warning(f"Connection error on attempt {attempt + 1}: {str(e)}")
                print_info(f"Retrying in {retry_delay} seconds...")
                time.sleep(retry_delay)
                retry_delay *= 2  # Exponential backoff
            else:
                print_error(
                    f"Failed to get card details after {max_retries} attempts: {str(e)}"
                )
                return None


# Main execution block
if __name__ == "__main__":
    print_header("Flashcard API Testing Tool")
    print_info(f"Using API at {BASE_URL}")
    print_info(f"Using MailHog at {MAILHOG_URL}")

    # Wait for server to be ready
    if not wait_for_server():
        print_error("Server not available. Exiting.")
        sys.exit(1)

    # Run basic health check
    if not test_health_check():
        print_error(
            "Health check failed. Server might be running but not working correctly."
        )
        if not args.auto:
            input("Press Enter to continue anyway or Ctrl+C to exit...")

    # Authentication flow
    test_register_user()
    test_verify_email()
    test_login()
    test_refresh_token()

    # Deck and card operations
    test_create_deck()
    test_get_decks()
    test_get_deck_details()
    test_create_card()
    test_get_cards()
    test_get_card_details()

    print_header("Testing Complete")
    print_success("All tests have been executed")
