# UdHocTap AI Service - API Documentation (EchoAPI/Postman)

Tài liệu này cung cấp các ví dụ đầy đủ để bạn có thể import hoặc copy vào **EchoAPI** để test hệ thống.

- **Base URL:** `http://localhost:8000`
- **Thời gian phản hồi dự kiến:** 5s - 15s (tùy thuộc vào tốc độ xử lý của Gemini AI)

---

## 1. Kiểm tra trạng thái (Health Check)
Dùng để xác nhận server đang chạy và kết nối được với Gemini API.

- **Method:** `GET`
- **URL:** `{{base_url}}/`
- **Headers:** 
  - `Accept: application/json`

### Phản hồi mẫu (200 OK):
```json
{
    "status": "online",
    "model": "gemini-3.1-flash-lite-preview",
    "features": [
        "Native PDF Support",
        "Structured Output",
        "Difficulty Levels"
    ]
}
```

---

## 2. Tạo Flashcard từ File (PDF/TXT)
Trích xuất kiến thức từ tài liệu tải lên.

- **Method:** `POST`
- **URL:** `{{base_url}}/api/v1/generate-flashcards`
- **Body Type:** `form-data`
- **Body Parameters:**
  - `file`: (File) Chọn file tài liệu (`.pdf` hoặc `.txt`)
  - `count`: `10` (Kiểu: Number - Số lượng thẻ cần tạo)
  - `difficulty`: `medium` (Kiểu: Text - Các giá trị: `easy`, `medium`, `hard`)

### Phản hồi mẫu (200 OK):
```json
{
    "flashcards": [
        {
            "front": "Câu hỏi trích xuất từ tài liệu?",
            "back": "Câu trả lời tương ứng."
        }
    ]
}
```

---

## 3. Tạo Flashcard từ Văn bản hoặc URL
Tạo thẻ học nhanh từ nội dung copy hoặc link bài báo/wiki.

- **Method:** `POST`
- **URL:** `{{base_url}}/api/v1/generate-from-text`
- **Headers:** 
  - `Content-Type: application/json`
- **Body Type:** `raw (JSON)`
- **Body:**
```json
{
    "text": "https://vi.wikipedia.org/wiki/H%E1%BB%93_Ch%C3%AD_Minh",
    "is_url": true,
    "count": 5,
    "difficulty": "medium"
}
```
*Ghi chú: Nếu `is_url` là `false`, trường `text` sẽ được hiểu là nội dung văn bản thuần túy.*

---

## 4. Giải thích chuyên sâu thẻ học
Yêu cầu AI đóng vai gia sư giải thích chi tiết nội dung trên thẻ.

- **Method:** `POST`
- **URL:** `{{base_url}}/api/v1/explain-card`
- **Headers:** 
  - `Content-Type: application/json`
- **Body Type:** `raw (JSON)`
- **Body:**
```json
{
    "front": "Định luật Newton thứ 2 là gì?",
    "back": "F = m.a (Lực bằng khối lượng nhân gia tốc)"
}
```

### Phản hồi mẫu (200 OK):
```json
{
    "explanation": "Định luật 2 Newton khẳng định rằng gia tốc của một vật có cùng hướng với lực tác dụng lên vật... Ví dụ: Khi bạn đẩy một chiếc xe đẩy hàng trống, nó sẽ đi nhanh hơn so với khi xe đầy hàng nếu dùng cùng một lực đẩy."
}
```

---

## 5. Hỏi đáp dựa trên bộ tri thức (RAG Knowledge Base)
Hỏi các câu hỏi liên quan đến kiến thức vũ trụ, khoa học có trong hệ thống.

- **Method:** `POST`
- **URL:** `{{base_url}}/api/v1/ask-knowledge`
- **Headers:** 
  - `Content-Type: application/json`
- **Body Type:** `raw (JSON)`
- **Body:**
```json
{
    "query": "Hệ mặt trời có bao nhiêu hành tinh?"
}
```

### Phản hồi mẫu (200 OK):
```json
{
    "answer": "Hệ Mặt Trời của chúng ta có tổng cộng 8 hành tinh chính thức, bao gồm: Sao Thủy, Sao Kim, Trái Đất, Sao Hỏa, Sao Mộc, Sao Thổ, Sao Thiên Vương và Sao Hải Vương.",
    "sources": ["Hệ Mặt Trời"]
}
```

---

## 6. Trợ lý Chat (Multi-turn Chat)
Trò chuyện liên tục với AI, ghi nhớ lịch sử hội thoại.

- **Method:** `POST`
- **URL:** `{{base_url}}/api/v1/chat`
- **Headers:** 
  - `Content-Type: application/json`
- **Body:**
```json
{
    "message": "Nhà mình có 2 con chó.",
    "session_id": null
}
```

### Phản hồi mẫu (Lượt 1):
Backend sẽ trả về một `session_id` mới. Bạn cần lưu ID này lại cho lượt chat kế tiếp.
```json
{
    "answer": "Ồ, thật tuyệt! Chăm sóc 2 chú chó chắc hẳn rất vui...",
    "session_id": "8f3a2b1c-...",
    "history": [
        { "role": "user", "text": "Nhà mình có 2 con chó." },
        { "role": "model", "text": "Ồ, thật tuyệt! ..." }
    ]
}
```

### Request tiếp theo (Gửi kèm session_id):
```json
{
    "message": "Vậy tổng cộng nhà mình có bao nhiêu cái chân?",
    "session_id": "8f3a2b1c-..."
}
```
*Lưu ý: Bạn không cần gửi lại history, Backend sẽ tự tìm dựa trên `session_id`.*

---

## Bảng mã lỗi thường gặp

| Mã lỗi | Ý nghĩa | Cách khắc phục |
| :--- | :--- | :--- |
| `400` | Bad Request | Kiểm tra định dạng file (chỉ nhận PDF/TXT) hoặc URL không truy cập được. |
| `422` | Validation Error | Thiếu các trường bắt buộc trong Body hoặc sai kiểu dữ liệu. |
| `500` | Internal Server Error | Lỗi kết nối Gemini API (Kiểm tra API Key trong file .env). |
