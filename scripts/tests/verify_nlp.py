import requests
import time
import json

BASE_URL = "http://localhost:8092"

def check_health():
    print("Checking service health...")
    for i in range(10):
        try:
            resp = requests.get(f"{BASE_URL}/health", timeout=5)
            if resp.status_code == 200:
                print("✅ Service is UP!")
                return True
        except requests.exceptions.ConnectionError:
            pass
        print(f"Waiting for service... ({i+1}/10)")
        time.sleep(2)
    return False

def test_query(question, description):
    print(f"\n🔹 Testing: {description}")
    print(f"❓ Question: '{question}'")
    payload = {"question": question}
    try:
        resp = requests.post(f"{BASE_URL}/query", json=payload, timeout=30)
        if resp.status_code == 200:
            data = resp.json()
            answer = data.get("answer", "No answer found")
            print(f"💡 Answer: {answer}")
            return answer
        else:
            print(f"❌ Error {resp.status_code}: {resp.text}")
    except Exception as e:
        print(f"❌ Exception: {e}")

if __name__ == "__main__":
    if not check_health():
        print("❌ Service failed to start.")
        exit(1)

    # 1. Basic Fact Retrieval (Location: Text code)
    # Expect: Spacy might see TX as GPE OR custom logic handles it. 
    # With 'en_core_web_sm', 'Texas' is definitely a GPE. "TX" might be ORG or GPE depending on context.
    test_query("What is the electricity price in Texas?", "Fact Retrieval - State Name (Texas)")
    
    # 2. Fact Retrieval (Location: Full Name, different metric)
    test_query("What is the monthly bill in California?", "Fact Retrieval - State Name (California), Metric: Bill")

    # 3. New York (Multi-word state)
    test_query("Show me the energy rates for New York", "Fact Retrieval - Multi-word State (New York)")

    # 4. Sorting logic check
    test_query("Which state has the cheapest electricity?", "Fact Retrieval - Sorting (Cheapest)")

    # 5. Forecast Logic (Dynamic Location)
    test_query("Forecast the prices for Florida", "Forecast - State Name (Florida)")

    # 6. Bad Query (Guardrail)
    test_query("What is the weather like in Texas?", "Guardrail - Out of Scope")

    # 7. Semantic Metric Selection (No keyword overlap)
    # "How much does it cost to power my home?" -> Should map to Monthly Cost semantically
    test_query("How much does it cost to power my home in Ohio?", "Semantic Metric Matching (Vague query)")
