import requests
import json
import time

GREEN = "\033[92m"
RED = "\033[91m"
RESET = "\033[0m"
QUERY_URL = "http://localhost:8092/query"

def test_query(question):
    print(f"Testing Question: '{question}'")
    try:
        start = time.time()
        resp = requests.post(QUERY_URL, json={"question": question}, timeout=60)
        dur = time.time() - start
        
        if resp.status_code == 200:
            data = resp.json()
            ans = data.get("answer", "")
            debug = data.get("debug_meta", {})
            sql = debug.get("generated_sql", "")
            
            print(f"Time: {dur:.1f}s")
            # Verify it found data
            if "71" in ans or "38" in ans or "6.2" in ans or (debug.get("sql_result") and len(debug.get("sql_result")) > 0):
                print(f"{GREEN}PASS{RESET}: Data retrieved.")
                print(f"Answer: {ans[:150]}...")
            else:
                 print(f"{RED}FAIL{RESET}: No correct data found.")
                 print(f"Answer: {ans}")
            
            if sql:
                print(f"Generated SQL: {sql}")
        else:
            print(f"{RED}Error {resp.status_code}{RESET}: {resp.text}")
    except Exception as e:
        print(f"{RED}Exception{RESET}: {e}")

if __name__ == "__main__":
    # Test specific lookup
    test_query("What is the current temperature in Johnson County?")
    # Test specific metric
    test_query("Show me the weather stress index for Johnson County.")
