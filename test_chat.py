import asyncio
import json
import websockets

async def test_chat():
    uri = "ws://localhost:8001/api/v1/chat"

    async with websockets.connect(uri) as websocket:
        # Send message with RAG enabled
        message = {
            "message": "What is machine learning?",
            "enable_rag": True,
            "kb_name": "Test ML",
            "session_id": None
        }

        print("Sending:", json.dumps(message, indent=2))
        await websocket.send(json.dumps(message))

        sources_found = False
        response_text = ""

        # Receive messages
        while True:
            try:
                data = await asyncio.wait_for(websocket.recv(), timeout=5.0)
                msg = json.loads(data)
                msg_type = msg.get("type")

                print(f"\nReceived ({msg_type}):")

                if msg_type == "session":
                    print(f"  Session ID: {msg.get('session_id')}")

                elif msg_type == "status":
                    print(f"  Stage: {msg.get('stage')}, Message: {msg.get('message')}")

                elif msg_type == "stream":
                    response_text += msg.get("content", "")
                    print(f"  Content chunk: {msg.get('content', '')[:50]}...")

                elif msg_type == "sources":
                    sources_found = True
                    rag_sources = msg.get("rag", [])
                    print(f"  RAG Sources: {len(rag_sources)}")
                    for src in rag_sources:
                        print(f"    - {src.get('title', 'unknown')} (score: {src.get('score', 'N/A')})")
                        print(f"      Content: {src.get('content', '')[:100]}...")

                elif msg_type == "result":
                    print(f"  Final response length: {len(msg.get('content', ''))}")

                elif msg_type == "complete":
                    print("  Chat completed")
                    break

            except asyncio.TimeoutError:
                print("Timeout waiting for message")
                break

        if sources_found:
            print("\n✓ SUCCESS: Sources were returned!")
        else:
            print("\n✗ FAILED: No sources found in response")

        print(f"\nResponse length: {len(response_text)} chars")

if __name__ == "__main__":
    asyncio.run(test_chat())
