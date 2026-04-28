from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from apis.flashcards import router as flashcards_router
from apis.knowledge import router as knowledge_router
from apis.chat import router as chat_router

app = FastAPI(title="UdHocTap AI Service", version="1.5.0")

# --- Middleware ---
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- Include Routers ---
app.include_router(flashcards_router)
app.include_router(knowledge_router)
app.include_router(chat_router)

@app.get("/")
def read_root():
    return {
        "status": "online",
        "model": "gemini-3.1-flash-lite-preview",
        "features": [
            "Native PDF Support", 
            "Structured Output", 
            "Difficulty Levels", 
            "RAG Knowledge Base"
        ],
        "message": "Backend refactored and ready for scaling."
    }
