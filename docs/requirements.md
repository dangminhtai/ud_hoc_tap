# Requirements - UdHocTap AI System

## 1. Tổng quan (Overview)
Hệ thống UdHocTap là một ứng dụng hỗ trợ học tập sử dụng trí tuệ nhân tạo (Gemini AI). Dự án bao gồm Backend (FastAPI) và Mobile App (Android).

## 2. Tính năng lõi (Core Features)

### 2.1. Flashcard Generation
- Tạo flashcard tự động từ tài liệu tải lên (PDF, TXT).
- Tạo flashcard từ văn bản thuần túy hoặc liên kết (URL).
- Tùy chỉnh số lượng câu hỏi và độ khó (Easy, Medium, Hard).

### 2.2. AI Tutor (Giải thích thẻ)
- Giải thích chi tiết các khái niệm trên flashcard.
- Cung cấp ví dụ thực tế và các điểm cần lưu ý.

### 2.3. Hệ thống Tri thức RAG (Knowledge-based RAG) - [NEW]
- **Mục tiêu**: Cho phép người dùng đặt câu hỏi dựa trên bộ dữ liệu tri thức có sẵn (`knowledges.json`).
- **Nguồn dữ liệu**: `backend/datas/knowledges.json`.
- **Kỹ thuật**: 
    - Sử dụng **Vector Embedding** (mô hình `text-embedding-004` hoặc `gemini-embedding-001`).
    - Lưu trữ vào **Vector Database** (ChromaDB) để tìm kiếm ngữ nghĩa (Semantic Search).
    - Sử dụng mô hình **Gemini-2.0-Flash** để tổng hợp câu trả lời từ dữ liệu tìm được.

## 3. Yêu cầu kỹ thuật (Technical Requirements)

### 3.1. Backend
- Framework: **FastAPI**.
- AI SDK: **google-genai** (phiên bản mới nhất).
- Vector DB: **ChromaDB**.
- Lưu trữ: Local storage cho file PDF/TXT và Vector index.

### 3.2. API Endpoints
1. `GET /`: Kiểm tra trạng thái.
2. `POST /api/v1/generate-flashcards`: Tạo thẻ từ file.
3. `POST /api/v1/generate-from-text`: Tạo thẻ từ text/URL.
4. `POST /api/v1/explain-card`: Giải thích thẻ.
5. `POST /api/v1/ask-knowledge`: [NEW] Hỏi đáp dựa trên bộ tri thức (RAG).

## 4. Kế hoạch triển khai RAG (RAG Implementation Plan)
1. **Tiền xử lý**: Viết script nạp dữ liệu từ `knowledges.json` vào ChromaDB khi server khởi động.
2. **Xử lý Query**:
    - Nhận câu hỏi từ User.
    - Embedding câu hỏi.
    - Truy vấn Top 3 đoạn tri thức liên quan nhất từ ChromaDB.
3. **Sinh nội dung**: Kết hợp câu hỏi và tri thức tìm được vào Prompt để Gemini trả lời.
