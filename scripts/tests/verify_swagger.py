import requests
import time

URLS = [
    ("Intelligence Service", "http://localhost:8092/docs"),
    ("Java API Service", "http://localhost:8090/swagger-ui/index.html"),
    ("Java Ingestion Service", "http://localhost:8081/swagger-ui/index.html")
]

print("🔍 Verifying Swagger UI Accessibility...")
print("=======================================")

for name, url in URLS:
    try:
        resp = requests.get(url, timeout=5)
        if resp.status_code == 200:
            print(f"✅ {name}: ACCESSIBLE ({url})")
        else:
            print(f"❌ {name}: RETURNED {resp.status_code} ({url})")
    except Exception as e:
        print(f"❌ {name}: FAILED - {e}")

print("\nDone.")
