import os
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from google import genai
from google.genai import types
from dotenv import load_dotenv

# Load environment variables from .env
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ENV_PATH = os.path.join(BASE_DIR, ".env")
load_dotenv(ENV_PATH)

# Configure Gemini API
API_KEY = os.getenv("GEMINI_API_KEY")

# Diagnostic print (Safe to keep during dev)
if not API_KEY:
    print(f"Error: Could not find GEMINI_API_KEY. Checked path: {ENV_PATH}")
    raise RuntimeError(f"GEMINI_API_KEY is not set. Please check {ENV_PATH}")

# Initialize Gemini Client
client = genai.Client(api_key=API_KEY)

app = FastAPI(title="UdHocTap AI Service", version="1.2.0")

# CORS config
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class Flashcard(BaseModel):
    front: str
    back: str

class GenerateResponse(BaseModel):
    flashcards: list[Flashcard]

async def generate_flashcards_with_gemini(file_bytes: bytes, mime_type: str) -> list[Flashcard]:
    prompt = """
    You are an expert tutor. Create flashcards from the provided document.
    Extract the most important concepts and definitions.
    Output MUST be a list of objects with 'front' (question/concept) and 'back' (answer/definition) fields.
    Analyze the document carefully, including any diagrams or tables if present.
    """

    try:
        # Construct content based on file type
        # Native PDF support: We send the bytes directly as a Part
        content_parts = [prompt]
        
        if mime_type == "text/plain":
            content_parts.append(file_bytes.decode('utf-8'))
        else:
            # For PDF and other media supported by Gemini
            content_parts.append(
                types.Part.from_bytes(data=file_bytes, mime_type=mime_type)
            )

        response = client.models.generate_content(
            model='gemini-3.1-flash-lite-preview',
            contents=content_parts,
            config={
                'response_mime_type': 'application/json',
                'response_schema': list[Flashcard],
            }
        )
        
        return response.parsed
    except Exception as e:
        raise Exception(f"AI Generation failed: {str(e)}")

@app.post("/api/v1/generate-flashcards", response_model=GenerateResponse)
async def generate_flashcards(file: UploadFile = File(...)):
    # Supported mime types for this endpoint
    ALLOWED_TYPES = {
        "application/pdf": "application/pdf",
        "text/plain": "text/plain"
    }
    
    # Fallback check by extension if content_type is generic
    mime_type = file.content_type
    if mime_type not in ALLOWED_TYPES:
        if file.filename.endswith('.pdf'):
            mime_type = "application/pdf"
        elif file.filename.endswith('.txt'):
            mime_type = "text/plain"
        else:
            raise HTTPException(status_code=400, detail="Only PDF and TXT files are supported")
        
    content = await file.read()
    
    if not content:
        raise HTTPException(status_code=400, detail="File is empty")
            
    try:
        flashcards = await generate_flashcards_with_gemini(content, mime_type)
        return GenerateResponse(flashcards=flashcards)
        
    except Exception as e:
        print(f"Error handling request: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/")
def read_root():
    return {
        "status": "online", 
        "model": "gemini-3.1-flash-lite-preview",
        "features": ["Native PDF Support", "Structured Output"]
    }
