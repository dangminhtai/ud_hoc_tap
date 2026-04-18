# 📱 Ứng Dụng Học Tập Thông Minh - Hướng Dẫn Thiết Lập

## 🎯 Giới Thiệu

**Ứng Dụng Học Tập Thông Minh** là một ứng dụng mobile Android cho phép tạo flashcard, ôn tập, kiểm tra thông qua quiz, và theo dõi tiến độ học tập. Ứng dụng tích hợp AI để tự động sinh flashcard từ tài liệu PDF/TXT.

### Tính Năng Phase 1 (MVP)
✅ Tạo & quản lý bộ thẻ (Deck)  
✅ Thêm/sửa/xóa flashcard  
✅ Chế độ ôn tập với lật thẻ 3D  
✅ Chế độ quiz trắc nghiệm  
✅ Thống kê học tập (hôm nay, chuỗi ngày, độ chính xác, tuần)  
✅ Tag cho flashcard  
✅ Nhắc nhở học tập hàng ngày  
✅ Hỗ trợ Dark/Light theme  

### Công Nghệ
- **Frontend**: Kotlin + Jetpack Compose + Material3
- **Database**: Room (SQLite)
- **DI**: Hilt
- **Architecture**: Clean Architecture + MVVM
- **Backend (Phase 2+)**: FastAPI + Python
- **AI**: Google Gemini API

---

## 🚀 Yêu Cầu Hệ Thống

### Android Development
- **Android Studio**: Electric Eel hoặc mới hơn
- **Android SDK**: API 26+ (minSdk 26, targetSdk 35)
- **Java**: JDK 17+
- **Gradle**: 8.2.0+ (sẽ tự download)

### Backend (Phase 2+)
- **Python**: 3.8+
- **FastAPI**, **Uvicorn**, **PyMuPDF**, **google-generativeai**
- Các dependencies đã được định nghĩa trong `backend/requirements.txt`

---

## 📦 Thiết Lập Android App

### 1. Clone/Download Project
```bash
cd d:\UTE\NAM3\KII\LAP\ TRINH\ Di\ DONG\cuoi_ki\ud_hoc_tap
```

### 2. Mở Project trong Android Studio
```bash
# Windows
start . android-studio

# Hoặc
android-studio .
```

### 3. Build Project
```bash
# Sử dụng Unix gradlew (nếu trên bash/WSL)
./gradlew clean build

# Hoặc sử dụng Windows gradlew
gradlew.bat clean build
```

### 4. Chạy trên Emulator hoặc Device
```bash
# Chạy trên emulator/device đã kết nối
./gradlew installDebug

# Hoặc từ Android Studio: Run > Run 'app'
```

---

## 🔧 Cấu Hình Backend (AI Generation - Phase 2+)

### 1. Cấu Hình Gemini API Key
Backend sử dụng Google Gemini API để tự động sinh flashcard từ tài liệu.

**Bước 1**: Lấy API Key từ Google AI Studio
1. Truy cập: https://ai.google.dev/
2. Click "Get API Key"
3. Lưu API Key của bạn

**Bước 2**: Cấu Hình `.env`
```bash
cd backend
# Mở hoặc tạo file .env
cat > .env << EOF
GEMINI_API_KEY=<YOUR_API_KEY_HERE>
EOF
```

### 2. Cài Đặt Dependencies Backend
```bash
cd backend
pip install -r requirements.txt
```

### 3. Chạy Backend Server
```bash
cd backend
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

✅ Server sẽ chạy tại: `http://localhost:8000`  
📚 API Documentation: `http://localhost:8000/docs`

### 4. Kết Nối Android App với Backend
- **Trên Emulator**: Backend URL tự động là `http://10.0.2.2:8000/` (xem `di/NetworkModule.kt`)
- **Trên Physical Device**: Thay đổi `BASE_URL` trong `di/NetworkModule.kt`:
  ```kotlin
  private const val BASE_URL = "http://<YOUR_IP>:8000/"
  ```

---

## 📁 Cấu Trúc Project

```
ud_hoc_tap/
├── app/
│   ├── src/main/
│   │   ├── java/com/duong/udhoctap/
│   │   │   ├── core/                    # Core layers
│   │   │   │   ├── database/           # Room DB (entities, DAOs)
│   │   │   │   ├── network/            # Retrofit API definitions
│   │   │   │   ├── data/repository/    # Repository implementations
│   │   │   │   ├── ui/                 # UI components & theme
│   │   │   │   └── notification/       # WorkManager reminder
│   │   │   ├── feature/                # Feature modules
│   │   │   │   ├── home/              # Home screen
│   │   │   │   ├── deck/              # Deck detail & edit
│   │   │   │   ├── review/            # Review (flip card) mode
│   │   │   │   ├── quiz/              # Quiz mode
│   │   │   │   └── stats/             # Statistics screen
│   │   │   ├── di/                     # Dependency injection modules
│   │   │   ├── MainActivity.kt
│   │   │   └── UdHocTapApp.kt
│   │   └── res/
│   │       ├── values/strings.xml
│   │       ├── values/themes.xml
│   │       └── ...
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── backend/
│   ├── main.py                         # FastAPI server
│   ├── requirements.txt                # Python dependencies
│   └── .env                            # API keys (create locally)
├── gradle/                             # Gradle wrapper
├── build.gradle.kts                    # Root build config
├── settings.gradle.kts                 # Project settings
├── gradle.properties                   # Gradle properties
├── gradlew                             # Unix wrapper
├── gradlew.bat                         # Windows wrapper
└── README.md                           # This file
```

---

## 🗄️ Database Schema

| Entity | Mục Đích |
|--------|---------|
| **Deck** | Bộ thẻ (collections) |
| **Flashcard** | Thẻ học (card) |
| **Tag** | Nhãn cho thẻ/bộ |
| **ReviewLog** | Lịch sử ôn tập |
| **DeckTagCrossRef** | Liên kết Deck ↔ Tag |
| **FlashcardTagCrossRef** | Liên kết Flashcard ↔ Tag |

---

## 🎮 Hướng Dẫn Sử Dụng

### Tạo Bộ Thẻ
1. Nhấn nút **+** trên trang chủ
2. Nhập tên bộ thẻ
3. Chọn màu (tùy chọn)
4. Nhấn "Tạo"

### Thêm Flashcard
1. Chọn bộ thẻ
2. Nhấn nút **+ Thêm thẻ**
3. Nhập mặt trước (câu hỏi) và mặt sau (câu trả lời)
4. Thêm tag (tùy chọn)
5. Nhấn "Lưu thẻ"

### Ôn Tập (Review Mode)
1. Chọn bộ thẻ → Nhấn **"Ôn tập"**
2. Chạm thẻ để lật
3. Chọn mức độ: Quên / Khó / Tốt / Dễ
4. Thẻ sẽ được lên lịch lại tự động

### Quiz Mode
1. Chọn bộ thẻ (cần ≥4 thẻ) → Nhấn **"Quiz"**
2. Trả lời các câu trắc nghiệm
3. Xem kết quả cuối cùng

### Thống Kê
- Xem trên tab **"Thống kê"**
- Hiển thị: thẻ hôm nay, chuỗi ngày, độ chính xác, biểu đồ 7 ngày

---

## 🐛 Khắc Phục Sự Cố

### Build fails: "Gradle build failed"
```bash
# Xóa cache và build lại
./gradlew clean
./gradlew build
```

### Cannot connect to backend
- ✅ Backend đã chạy tại `http://localhost:8000`?
- ✅ Firewall cho phép port 8000?
- ✅ URL trong `NetworkModule.kt` chính xác?

### AI generation fails
- ✅ Gemini API key đã set trong `.env`?
- ✅ API key hợp lệ và có quota?
- ✅ File PDF/TXT hợp lệ?

### Emulator slow
- Sử dụng GPU acceleration trong AVD Manager
- Tăng RAM allocation
- Hoặc sử dụng physical device

---

## 📝 Ghi Chú Phát Triển

### Phase 1 ✅ (Current)
- [x] Cấu trúc database
- [x] CRUD Deck/Flashcard
- [x] Review screen với flip animation
- [x] Quiz mode
- [x] Statistics screen
- [x] Tag management
- [x] Notification (WorkManager)
- [x] Theme (Dark/Light)

### Phase 2 🔄 (In Progress)
- [ ] Document Summarization (PDF → Flashcards)
- [ ] Backend API integration
- [ ] Gemini AI flashcard generation

### Phase 3 ⏳ (Future)
- [ ] FSRS (Free Spaced Repetition Scheduling)
- [ ] Personalized learning paths
- [ ] Cloud sync

---

## 📧 Support & Issues

Báo cáo lỗi hoặc đề xuất tính năng:
- **Email**: duong@example.com
- **GitHub Issues**: (Nếu có repository)

---

## 📄 License

MIT License - Xem tệp LICENSE để chi tiết

---

**Bắt đầu học tập thông minh ngay hôm nay! 🎓**
