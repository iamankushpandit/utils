import requests
import json
import sys

# Color codes
GREEN = "\033[92m"
RED = "\033[91m"
RESET = "\033[0m"

QUERY_URL = "http://localhost:8092/query"

def test_debug(question):
    print(f"Testing Question: '{question}'")
    try:
        resp = requests.post(QUERY_URL, json={"question": question}, timeout=60)
        if resp.status_code == 200:
            data = resp.json()
            print(f"{GREEN}HTTP 200 OK{RESET}")
            # Pretty print the JSON to verify debug_meta
            print(json.dumps(data, indent=2))
        else:
            print(f"{RED}Error {resp.status_code}{RESET}: {resp.text}")
    except Exception as e:
        print(f"{RED}Exception{RESET}: {e}")

if __name__ == "__main__":
    # Test a query that requires joining Region table
    test_debug("Show me the bill amount for Dallas.")
