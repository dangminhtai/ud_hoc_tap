# Hướng dẫn Xây dựng Hệ thống RAG với Python Backend

Trong thực tế, các hệ thống RAG (Retrieval-Augmented Generation) thường được phát triển trên Python do hệ sinh thái AI/ML và xử lý dữ liệu của ngôn ngữ này cực kỳ mạnh mẽ, đặc biệt là sự hỗ trợ từ các thư viện chuẩn công nghiệp như LangChain, LlamaIndex, FastAPI.

Dưới đây là một **cấu trúc thư mục chuyên nghiệp** và **hướng dẫn triển khai** cho một dự án Python Backend kết hợp hệ thống RAG sử dụng API mới của Gemini.

## 1. Cấu trúc thư mục dự án chuẩn (Clean Architecture)

```text
rag_python_backend/
│
├── app/                        # Mã nguồn chính của ứng dụng
│   ├── __init__.py
│   ├── main.py                 # Điểm Entry point của ứng dụng (Khởi tạo FastAPI)
│   │
│   ├── api/                    # Chứa các Controller / Routers xử lý request (Không chứa logic)
│   │   ├── __init__.py
│   │   ├── routers/
│   │   │   ├── rag.py          # API Endpoint cho search và RAG (Q&A)
│   │   │   └── document.py     # API Endpoint để upload tài liệu, chunking, tạo embedding
│   │   └── dependencies.py     # Quản lý Database Session, Verify Token/Auth,...
│   │   
│   ├── core/                   # Cấu hình thiết lập hệ thống
│   │   ├── __init__.py
│   │   ├── config.py           # Load biến môi trường (Dùng Pydantic BaseSettings)
│   │   └── security.py         # Hàm mã hóa, xử lý authentication
│   │
│   ├── models/                 # Chứa cấu trúc Model Entity Database (SQLAlchemy, MongoDB...) 
│   │   ├── __init__.py
│   │   └── document.py
│   │
│   ├── schemas/                # Chứa Pydantic models (Data Transfer Object - Validate request/res)
│   │   ├── __init__.py
│   │   ├── rag_schema.py       # Pydantic schema cho RAG Query
│   │   └── doc_schema.py       # Pydantic schema cho Upload Document
│   │
│   ├── services/               # **NƠI CHỨA BUSINESS LOGIC CHÍNH** (1 Hàm - 1 Chức năng)
│   │   ├── __init__.py
│   │   ├── rag_service.py      # Logic thực hiện RAG (Nhận câu hỏi -> Tìm DB -> Đưa cho LLM)
│   │   ├── ai_service.py       # Tương tác với Google GenAI (Chat/Embed Content)
│   │   └── vector_db.py        # Logic tương tác với Vector DB (ChromaDB, Pinecone, FAISS)
│   │
│   └── utils/                  # Thư mục chứa các hàm helpers dùng chung
│       ├── __init__.py
│       ├── text_chunker.py     # Tách cụm từ, chunking văn bản 
│       └── formatters.py       # Định dạng chuỗi
│
├── data/                       # Thư mục chứa dữ liệu thô hoặc local file db (nên đưa vào .gitignore)
├── tests/                      # Chứa các Unit Test (Pytest)
├── .env                        # Chứa các secret keys (GEMINI_API_KEY)
├── requirements.txt            # Danh sách thư viện tải qua pip
└── README.md                   # Tài liệu mô tả dự án
```

---

## 2. Giải thích Kiến trúc

- **`api/routers`**: Chỉ làm một việc duy nhất là lắng nghe HTTP Request, kiểm tra tính hợp lệ bằng schema, đưa cho `services` xử lý cốt lõi, và trả về HTTP Response.
- **`services/`**: Nơi trái tim của RAG hoạt động. Tách biệt `ai_service` (chỉ chuyên gọi Google) và `vector_db` (chỉ chuyên gọi DB). `rag_service` sẽ ghép nối các bước lại với nhau. Giúp clean code và dễ maintain.
- **`schemas/`**: Dùng `Pydantic` để định nghĩa kiểu dữ liệu chặt chẽ. Hạn chế tối đa lỗi runtime crash như "TypeError: string is not an object".

---

## 3. Quy trình Core Code RAG bằng Python & Google GenAI SDK

### Bước 1: Setup Môi trường (`requirements.txt`)
```text
fastapi>=0.109.0
uvicorn>=0.27.0
pydantic>=2.5.3
pydantic-settings>=2.1.0
google-genai>=0.3.0
chromadb>=0.4.22
python-dotenv>=1.0.1
```

### Bước 2: Cài đặt Configuration (`app/core/config.py`)
```python
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    GEMINI_API_KEY: str
    VECTOR_DB_PATH: str = "./data/chroma"
    EMBEDDING_MODEL: str = "gemini-embedding-001"
    CHAT_MODEL: str = "gemini-2.5-flash"
    
    class Config:
        env_file = ".env"

settings = Settings()
```

### Bước 3: AI Service Sinh Embedding (`app/services/ai_service.py`)
Nơi duy nhất giao tiếp với Google GenAI.

```python
from google import genai
from app.core.config import settings

# Nạp Client theo thư viện mới
client = genai.Client(api_key=settings.GEMINI_API_KEY)

def get_embedding(text: str) -> list[float]:
    """Tạo vector embedding từ một đoạn text"""
    response = client.models.embed_content(
        model=settings.EMBEDDING_MODEL,
        contents=text,
    )
    # Trả về list chứa các chiều (dimension) của Vector
    return response.embeddings[0].values

def get_llm_response(prompt: str) -> str:
    """Gọi LLM sinh ra câu trả lời cuối cùng"""
    response = client.models.generate_content(
        model=settings.CHAT_MODEL,
        contents=prompt
    )
    return response.text
```

### Bước 4: Search trong Vector Database (`app/services/vector_db.py`)
*(VD giả định sử dụng ChromaDB hoặc kho lưu trữ vector cục bộ)*

```python
from app.services.ai_service import get_embedding

def search_related_chunks(query_vector: list[float], top_k: int = 3) -> list[str]:
    """
    Giả định hàm này gọi vào DB thực tế (Pincecone/ChromaDB).
    Nó sẽ dùng query_vector để search thuật toán Cosine Similarity trong DB.
    """
    # [LOGIC DATABASE Ở ĐÂY]...
    
    # Trả về các chunk tài liệu tốt nhất
    return [
        "Tài liệu 1: RAG là quá trình bổ sung tài liệu để LLM...",
        "Tài liệu 2: Kỹ thuật nhúng Embedding biến chữ thành số..."
    ]
```

### Bước 5: Ghép nối quy trình - RAG Service (`app/services/rag_service.py`)

```python
from app.services.ai_service import get_embedding, get_llm_response
from app.services.vector_db import search_related_chunks

def handle_rag_question(query: str) -> str:
    try:
        # 1. Chuyển đổi câu hỏi của User thành Vector (Query Embedding)
        query_vector = get_embedding(query)
        
        # 2. Retrieval: Tìm các đoạn text liên quan nhất trong DB
        relevant_chunks = search_related_chunks(query_vector, top_k=3)
        
        # 3. Augmentation: Gộp tài liệu lại làm Context (Ngữ cảnh)
        context_str = "\n\n".join([f"[Đoạn {i+1}]: {str(chunk)}" for i, chunk in enumerate(relevant_chunks)])
        
        # 4. Khởi tạo System Prompt ép LLM dùng tài liệu
        prompt = f"""Bạn là một tư vấn viên hỗ trợ RAG thông minh. 
Dựa vào các TÀI LIỆU được cung cấp bên dưới, hãy trả lời CÂU HỎI của người dùng.
NẾU tài liệu không có thông tin để trả lời, HÃY TRẢ LỜI "Tôi không có đủ dữ liệu", KHÔNG ĐƯỢC tự bịa đặt.

--- TÀI LIỆU ---
{context_str}

--- CÂU HỎI ---
{query}"""

        # 5. Generation: Sinh câu trả lời
        answer = get_llm_response(prompt)
        return answer
        
    except Exception as e:
        raise Exception(f"Lỗi trong quá trình RAG: {str(e)}")
```

### Bước 6: Mở Endpoint ra ngoài Internet (`app/api/routers/rag.py`)

```python
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from app.services.rag_service import handle_rag_question

router = APIRouter(prefix="/api/rag", tags=["RAG Services"])

class QuestionRequest(BaseModel):
    query: str

class QuestionResponse(BaseModel):
    answer: str

@router.post("/ask", response_model=QuestionResponse)
async def ask_question(payload: QuestionRequest):
    try:
        if not payload.query.strip():
            raise HTTPException(status_code=400, detail="Câu hỏi rỗng.")
            
        answer = handle_rag_question(payload.query)
        return QuestionResponse(answer=answer)
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
```

---

## 4. Những nguyên tắc tối ưu (Best Practices) khi mở rộng Backend

Để xây một hệ thống Production-Grade (Thực tế quy mô lớn), bạn cần:

1. **Chunking Document Nâng Cao**: Khi Parsing PDF hay Web, không thể đẩy cả 1 trang giấy vào API. Cần chia nhỏ theo kích thước Token (khoảng **500 - 1000 tokens/chunk**) và phải có khoảng trùng lặp (Overlap) là **50-100 tokens** giữa 2 chunk để tránh cắt ngang nghĩa câu.
2. **Chọn Vector Database Chuẩn**: API Embedding có thể tốn phí và thời gian. Backend không dùng vòng lặp For như Frontend. Bắt buộc kết nối với Database Vector xịn như **ChromaDB, Pinecone, Milvus, Qdrant** sử dụng Index (Ví dụ: `HNSW - Hierarchical Navigable Small World`) để search Vector ra trong tích tắc O(log N).
3. **Tiết Kiệm Token API**:
   - Viết logic caching (Redis) câu hỏi của user: Nếu câu hỏi giống 95% câu hỏi trong quá khứ -> Nhả luôn Cache, khỏi tốn tiền chạy Embedding rồi gọi LLM.
4. **Asynchronous (Code Bất đồng bộ)**:
   - Các logic gọi Gemini API nên chạy bằng `async/await` với SDK bất đồng bộ ở Python để không block backend khi scale lên nhiều Request con-current.
