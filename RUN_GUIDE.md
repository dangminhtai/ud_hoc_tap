# Hướng dẫn Chạy ứng dụng (Backend & Frontend)

Tài liệu này hướng dẫn cách cài đặt và chạy hệ thống **UdHocTap** bao gồm Backend (FastAPI) và Frontend (Android).

---

## 1. Yêu cầu hệ thống
*   **Backend:** Python 3.10 trở lên.
*   **Frontend:** Android Studio (phiên bản Ladybug trở lên khuyến nghị), JDK 17.
*   **Kết nối:** Cả điện thoại/emulator và máy tính chạy server nên cùng một mạng (nếu chạy thật) hoặc dùng `10.0.2.2` cho emulator.

---

## 2. Hướng dẫn chạy Backend (FastAPI)

### Bước 2.1: Chuẩn bị môi trường
Mở terminal tại thư mục gốc của dự án:
```bash
# Di chuyển vào thư mục backend
cd backend

# Tạo môi trường ảo (khuyến nghị)
python -m venv venv

# Kích hoạt môi trường ảo
# Windows:
venv\Scripts\activate
# Linux/macOS:
source venv/bin/activate
```

### Bước 2.2: Cài đặt thư viện
```bash
pip install -r requirements.txt
```

### Bước 2.3: Cấu hình biến môi trường
*   Kiểm tra file `.env` ở thư mục gốc.
*   Đảm bảo `LLM_API_KEY` đã được điền chính xác để các tính năng AI hoạt động.

### Bước 2.4: Khởi chạy Server
```bash
# Đứng tại thư mục gốc của dự án (ud_hoc_tap)
uvicorn backend.api.main:app --host 0.0.0.0 --port 8001 --reload
```
*   Server sẽ chạy tại: `http://localhost:8001`
*   Tài liệu API (Swagger): `http://localhost:8001/docs`

---

## 3. Hướng dẫn chạy Frontend (Android)

### Bước 3.1: Mở dự án
1.  Mở **Android Studio**.
2.  Chọn **Open** và tìm đến thư mục `ud_hoc_tap`.
3.  Đợi Android Studio đồng bộ Gradle (có thể mất vài phút lần đầu).

### Bước 3.2: Cấu hình địa chỉ Server
*   Nếu chạy trên **Emulator**: Ứng dụng mặc định kết nối tới `http://10.0.2.2:8001`.
*   Nếu chạy trên **Thiết bị thật**: Bạn cần thay đổi `BASE_URL` trong code (thường ở `core/network/RetrofitClient.kt` hoặc config tương ứng) thành địa chỉ IP local của máy tính (ví dụ: `http://192.168.1.5:8001`).

### Bước 3.3: Chạy ứng dụng
1.  Chọn thiết bị (Emulator hoặc Physical Device).
2.  Nhấn nút **Run** (biểu tượng Play màu xanh).

---

## 4. Các lưu ý quan trọng
*   **Cơ sở dữ liệu:** Backend sử dụng lưu trữ tệp cục bộ cho kho tài liệu (Knowledge Base). Frontend sử dụng Room Database để lưu thẻ ghi nhớ.
*   **WebSockets:** Tính năng AI Chat và Tạo câu hỏi sử dụng WebSocket. Hãy đảm bảo mạng không chặn cổng 8001.
*   **Logcat:** Nếu gặp lỗi ở App, hãy kiểm tra tab **Logcat** trong Android Studio với filter `UdHocTap` hoặc `com.duong.udhoctap`.
*   **Backend Logs:** Kiểm tra terminal chạy uvicorn để xem các yêu cầu đến và lỗi phát sinh từ AI.

---
*Chúc bạn có trải nghiệm học tập tuyệt vời!*
