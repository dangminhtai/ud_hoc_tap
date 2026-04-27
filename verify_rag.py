import requests
import json

# Test RAG directly via REST API (simpler than WebSocket)
url = "http://localhost:8001/api/v1/chat/test-rag"
params = {
    "kb_name": "AI",
    "query": "What is machine learning"
}

try:
    response = requests.get(url, params=params, timeout=10)
    result = response.json()

    print("RAG Test Result:")
    print(json.dumps(result, indent=2))

    if result.get("status") == "ok":
        sources = result.get("sources", {}).get("rag", [])
        if sources:
            print(f"\n✓ SUCCESS: Found {len(sources)} sources!")
            for src in sources:
                print(f"  - {src.get('title', 'unknown')}")
        else:
            print("\n✗ No sources found")
    else:
        print(f"\n✗ Error: {result.get('error', 'Unknown error')}")

except Exception as e:
    print(f"Error: {e}")
    print("Endpoint might not exist, using WebSocket fallback...")
