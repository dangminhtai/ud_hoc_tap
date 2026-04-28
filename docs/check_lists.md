# Checklist: Backend & Mobile Alignment Plan

Dưới đây là các đầu việc cần thực hiện để Backend đồng bộ hoàn toàn với Template Mobile (Android).

## Phase 1: Chat & WebSocket (Nâng cấp giao thức)
- [x] Cấu hình chạy Backend trên Port `8001` (Cần chạy lệnh: `uvicorn main:app --port 8001`).
- [x] Triển khai WebSocket Endpoint `/api/v1/chat`.
- [x] Tích hợp tính năng **Streaming** từ Gemini SDK (gửi dữ liệu từng phần).
- [x] Định dạng dữ liệu trả về theo chuẩn `ChatEvent` (Session, Status, Stream, Result, Error).
- [x] Tích hợp **RAG (Knowledge Base)** trực tiếp vào luồng Chat khi có flag `enable_rag`.
- [x] Tích hợp **Google Search** vào luồng Chat khi có flag `enable_web_search`.

## Phase 2: Session Management (Quản lý lịch sử)
- [x] API `GET /api/v1/chat/sessions`: Liệt kê danh sách các phiên chat từ SQLite.
- [x] API `GET /api/v1/chat/sessions/{id}`: Lấy chi tiết toàn bộ tin nhắn của một phiên.
- [x] API `DELETE /api/v1/chat/sessions/{id}`: Xóa phiên chat khỏi database.
- [x] Cập nhật Database SQLite để lưu thêm thông tin `title`.

## Phase 3: Knowledge Base CRUD (Quản lý tri thức động)
- [x] API `GET /api/v1/knowledge/list`: Liệt kê các nguồn tri thức đã nạp.
- [x] API `POST /api/v1/knowledge/create`: Upload file PDF/TXT mới (Mock xử lý).
- [x] API `DELETE /api/v1/knowledge/{name}`: Xóa nguồn tri thức.
- [x] API `POST /api/v1/quiz/generate-from-kb`: Tạo trắc nghiệm trực tiếp từ tri thức (Sử dụng Gemini RAG).

## Phase 4: Notebook & Flashcards (Quản lý thẻ học)
- [x] Triển khai bảng Database cho Notebooks và Records trong SQLite.
- [x] API `GET /api/v1/notebook/list`: Liệt kê các bộ thẻ (Decks).
- [x] API `POST /api/v1/notebook/add_record`: Lưu một flashcard/kết quả vào bộ thẻ.
- [x] API `GET /api/v1/notebook/{id}`: Lấy chi tiết bộ thẻ và danh sách thẻ bên trong.

---
*Ghi chú: Toàn bộ hệ thống Backend hiện đã khớp 100% với giao thức mà App Mobile yêu cầu.*
