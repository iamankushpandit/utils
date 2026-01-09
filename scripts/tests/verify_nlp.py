import requests
import time
import json
import sys

BASE_URL = "http://localhost:8092"
QUERY_URL = f"{BASE_URL}/query"

# Color codes for terminal output
GREEN = "\033[92m"
RED = "\033[91m"
YELLOW = "\033[93m"
RESET = "\033[0m"

def check_health():
    print("Checking service health...")
    for i in range(5):
        try:
            resp = requests.get(f"{BASE_URL}/health", timeout=5)
            if resp.status_code == 200:
                print(f"{GREEN}✅ Service is UP!{RESET}")
                return True
        except:
            pass
        time.sleep(1)
    print(f"{RED}❌ Service down.{RESET}")
    return False

def run_test_suite():
    results = {"pass": 0, "fail": 0, "gen_ai": 0}
    
    questions = [
        # --- EIA / PRICE QUESTIONS (25) ---
        "What is the latest electricity retail price in Texas?", 
        "Show me the electricity retail price for California.",
        "How much does electricity cost in New York right now?",
        "What was the price of electricity in Florida in 2023?",
        "Compare electricity prices across all states.",
        "Which state has the highest electricity rate?",
        "Which state has the lowest electricity price this year?",
        "What is the average retail price for the US?",
        "Show me the trend of electricity prices in Washington over the last year.",
        "Give me the price per kWh in Massachusetts.",
        "Is electricity cheaper in Texas or Louisiana?",
        "List the retail electricity price for each state.",
        "What is the cost of electricity in 06?", 
        "Show me the retail price in AL.",
        "Electricity rates for IL.",
        "What is the price in Pennsylvania?",
        "Rates in Ohio vs Michigan.",
        "Did electricity prices go up in 2024?",
        "Show me the price history for Arizona.",
        "What is the max price recorded for Hawaii?",
        "What is the min price recorded for North Dakota?",
        "Get electricity price for Georgia.",
        "Cost of kwh in Virginia.",
        "Latest electricity rate in New Jersey.",
        "Comparison of rates in New England states.",

        # --- ACS / BILL QUESTIONS (25) ---
        "What is the average monthly electricity bill across states?",
        "Show me the monthly bill in Texas.",
        "How much is the average electric bill in California?",
        "Which county has the highest monthly electricity bill?",
        "Which county has the lowest monthly bill?",
        "What is the monthly bill for New York City?",
        "Comparison of monthly bills in Florida counties.",
        "Average bill in 12057.",
        "Show me the bill amount for Dallas.",
        "Trends in monthly electricity bills.",
        "Which place has the highest electricity cost?",
        "What is the average bill in Los Angeles?",
        "Monthly electric costs for Chicago.",
        "Bill amount in Miami.",
        "Is the bill higher in San Francisco or Seattle?",
        "How many counties have data?", 
        "Show me the lowest bill recorded.",
        "Average monthly cost in 48201.",
        "Bill amount in Harris County.",
        "Cost to live in Travis County regarding electricity.",
        "Map of monthly electricity bills.",
        "Show me the bill for all counties in Texas.",
        "What is the variance in monthly bills?",
        "Drastic increase in bills in 2023?",
        "Did bills decrease anywhere?",

        # --- FORECAST / PREDICTION QUESTIONS (New) ---
        "Forecast the electricity price for Texas in 2026.",
        "Predict the cost of electricity in California next year.",
        "What will the bill be in Florida in 6 months?",
        "Trend prediction for New York electricity rates.",
        "Project the future energy cost for 48201."
    ]

    print(f"\n🚀 Starting NLP Verification Suite ({len(questions)} Questions)...\n")

    for idx, q in enumerate(questions):
        print(f"[{idx+1}/{len(questions)}] Q: {q}")
        try:
            start_t = time.time()
            resp = requests.post(QUERY_URL, json={"question": q}, timeout=60) # Generative AI needs time
            duration = time.time() - start_t
            
            if resp.status_code == 200:
                data = resp.json()
                answer = data.get("answer", "")
                sources = data.get("sources", [])
                viz = data.get("visualization")
                debug_meta = data.get("debug_meta")
                
                # Validation Logic
                is_gen_ai = "GEN_AI_SQL" in sources
                has_sources = len(sources) > 0
                has_viz = viz is not None
                
                # Heuristic: Valid if sources found OR GenAI worked OR meaningful text response (not "I don't know")
                is_valid = has_sources or is_gen_ai or ("data" in answer.lower() and "sorry" not in answer.lower())

                if is_valid:
                    tag = f"{GREEN}PASS{RESET}"
                    if is_gen_ai: 
                        tag = f"{YELLOW}PASS (GenAI){RESET}"
                        results["gen_ai"] += 1
                    else:
                        results["pass"] += 1
                        
                    print(f"   => {tag} ({duration:.1f}s)")
                    if has_viz: print(f"      [Viz Detected: {viz['type']}]")
                    if debug_meta:
                        print(f"      [GenAI SQL]: {debug_meta.get('generated_sql')}")
                        # print(f"      [Raw Result]: {debug_meta.get('sql_result')[:3]}") # First 3 rows
                    # print(f"      A: {answer[:100]}...")
                else:
                    results["fail"] += 1
                    print(f"   => {RED}FAIL (No Data/Ambiguous){RESET}")
                    if debug_meta:
                        print(f"      [SQL Attempt]: {debug_meta.get('generated_sql')}")
                        print(f"      [Raw Result]: {debug_meta.get('sql_result')}")
                    # print(f"      A: {answer}")

            else:
                results["fail"] += 1
                print(f"   => {RED}ERROR {resp.status_code}{RESET}")

        except Exception as e:
            results["fail"] += 1
            print(f"   => {RED}EXCEPTION: {e}{RESET}")
        
        # print("-" * 40)

    print("\n" + "="*30)
    print("📊 TEST SUMMARY")
    print("="*30)
    print(f"Total:   {len(questions)}")
    print(f"Pass:    {results['pass'] + results['gen_ai']}")
    print(f" - Rules: {results['pass']}")
    print(f" - GenAI: {results['gen_ai']}")
    print(f"Fail:    {results['fail']}")
    print("="*30)

if __name__ == "__main__":
    if check_health():
        run_test_suite()
