import os
import json
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import fitz  # PyMuPDF
import google.generativeai as genai
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Configure Gemini API
API_KEY = os.getenv("GEMINI_API_KEY")
if not API_KEY or API_KEY == "your_gemini_api_key_here":
    print("Warning: GEMINI_API_KEY is not set correctly in .env file.")

if API_KEY:
    genai.configure(api_key=API_KEY)

app = FastAPI(title="UdHocTap AI Service", version="1.0.0")

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

def extract_text_from_pdf(file_bytes: bytes) -> str:
    try:
        doc = fitz.open(stream=file_bytes, filetype="pdf")
        text = ""
        for page in doc:
            text += page.get_text()
        return text
    except Exception as e:
        raise Exception(f"Failed to extract text from PDF: {str(e)}")

def generate_flashcards_with_gemini(text: str) -> list[dict]:
    if not API_KEY or API_KEY == "your_gemini_api_key_here":
        raise Exception("API key is not configured")
        
    model = genai.GenerativeModel('gemini-1.5-flash')
    
    prompt = """
    You are an expert tutor. Create flashcards from the following text.
    Extract the most important concepts and definitions.
    Output MUST be a valid JSON array of objects, where each object has a 'front' (question/concept) and a 'back' (answer/definition) field.
    Keep the answers concise but informative. Do NOT output any markdown, ONLY valid JSON.
    Example: [{"front": "What is AI?", "back": "Artificial Intelligence."}]
    
    Text to analyze:
    """ + text[:30000] # Limit to avoid exceeding token limits if text is too long

    try:
        response = model.generate_content(prompt)
        content = response.text.strip()
        
        # Clean up potential markdown formatting in response
        if content.startswith("```json"):
            content = content[7:]
        if content.startswith("```"):
            content = content[3:]
        if content.endswith("```"):
            content = content[:-3]
            
        flashcards = json.loads(content.strip())
        return flashcards
    except Exception as e:
        raise Exception(f"AI Generation failed: {str(e)}")

@app.post("/api/v1/generate-flashcards", response_model=GenerateResponse)
async def generate_flashcards(file: UploadFile = File(...)):
    if not file.filename.endswith(('.pdf', '.txt')):
        raise HTTPException(status_code=400, detail="Only PDF and TXT files are supported")
        
    content = await file.read()
    
    try:
        if file.filename.endswith('.pdf'):
            text = extract_text_from_pdf(content)
        else:
            text = content.decode('utf-8')
            
        if not text.strip():
            raise HTTPException(status_code=400, detail="Could not extract any text from the file")
            
        flashcards = generate_flashcards_with_gemini(text)
        return GenerateResponse(flashcards=flashcards)
        
    except json.JSONDecodeError:
        raise HTTPException(status_code=500, detail="Failed to parse AI response into flashcards")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/")
def read_root():
    return {"message": "UdHocTap AI Service is running"}
