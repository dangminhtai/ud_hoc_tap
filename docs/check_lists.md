# Checklist: Backend & Mobile Alignment Plan

Dưới đây là các đầu việc cần thực hiện để Backend đồng bộ hoàn toàn với Template Mobile (Android).

## Phase 1: Chat & WebSocket (Nâng cấp giao thức)
- [ ] Cấu hình chạy Backend trên Port `8001` (hoặc cấu hình để Mobile gọi đúng Port).
- [ ] Triển khai WebSocket Endpoint `/api/v1/chat`.
- [ ] Tích hợp tính năng **Streaming** từ Gemini SDK (gửi dữ liệu từng phần).
- [ ] Định dạng dữ liệu trả về theo chuẩn `ChatEvent` (Session, Status, Stream, Result, Error).
- [ ] Tích hợp **RAG (Knowledge Base)** trực tiếp vào luồng Chat khi có flag `enable_rag`.
- [ ] Tích hợp **Google Search** vào luồng Chat khi có flag `enable_web_search`.

## Phase 2: Session Management (Quản lý lịch sử)
- [ ] API `GET /api/v1/chat/sessions`: Liệt kê danh sách các phiên chat từ SQLite.
- [ ] API `GET /api/v1/chat/sessions/{id}`: Lấy chi tiết toàn bộ tin nhắn của một phiên.
- [ ] API `DELETE /api/v1/chat/sessions/{id}`: Xóa phiên chat khỏi database.
- [ ] Cập nhật Database SQLite để lưu thêm thông tin `title` (tự động tạo tiêu đề dựa trên tin nhắn đầu tiên).

## Phase 3: Knowledge Base CRUD (Quản lý tri thức động)
- [ ] API `GET /api/v1/knowledge/list`: Liệt kê các nguồn tri thức đã nạp.
- [ ] API `POST /api/v1/knowledge/upload`: Upload file PDF/TXT mới và tự động cập nhật ChromaDB.
- [ ] API `DELETE /api/v1/knowledge/{name}`: Xóa nguồn tri thức và gỡ khỏi Vector DB.
- [ ] API `POST /api/v1/quiz/generate-from-kb`: Tạo trắc nghiệm trực tiếp từ một nguồn tri thức cụ thể.

## Phase 4: Notebook & Flashcards (Quản lý thẻ học)
- [ ] Chuyển đổi logic `/generate-flashcards` để lưu kết quả vào SQLite thay vì chỉ trả về JSON.
- [ ] API `GET /api/v1/notebook/list`: Liệt kê các bộ thẻ (Decks).
- [ ] API `POST /api/v1/notebook/add_record`: Lưu một flashcard vào bộ thẻ.
- [ ] API `GET /api/v1/notebook/{id}`: Lấy danh sách thẻ trong một bộ.

---
*Ghi chú: Các task này sẽ giúp Backend hỗ trợ đầy đủ các tính năng "Premium" mà giao diện Mobile đã thiết kế.*
