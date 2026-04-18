# Ứng Dụng Học Tập Thông Minh (Smart Study Assistant)

> 📚 Flashcard learning app với AI integration, spaced repetition, và thống kê học tập

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-26%2B-green)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/Compose-2024.12-brightgreen)](https://developer.android.com/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## 🎯 Mục Tiêu

Xây dựng ứng dụng học tập cá nhân hóa với:
- ✨ Flashcard & spaced repetition scheduling (FSRS)
- 🤖 AI tự động sinh flashcard từ tài liệu
- 📊 Thống kê & heatmap tiến độ
- 🔔 Nhắc nhở học tập cá nhân hóa

---

## 🚀 Quick Start

### 1️⃣ Chuẩn Bị
```bash
# Yêu cầu
- Android Studio (Electric Eel+)
- JDK 17+
- Android SDK 26+
```

### 2️⃣ Clone & Mở Project
```bash
cd d:\UTE\NAM3\KII\LAP\ TRINH\ Di\ DONG\cuoi_ki\ud_hoc_tap
android-studio .
```

### 3️⃣ Build & Run
```bash
# Build
./gradlew clean build

# Run on emulator/device
./gradlew installDebug
```

### 4️⃣ Backend Setup (Optional - Phase 2+)
```bash
cd backend
pip install -r requirements.txt

# Cấu hình API key
cp .env.example .env
# Edit .env với Gemini API key từ https://ai.google.dev

# Chạy server
uvicorn main:app --reload --port 8000
```

📖 Chi tiết xem **[SETUP.md](SETUP.md)**

---

## 📋 Tính Năng Phase 1 (100% Complete ✅)

### Quản Lý Flashcard
- ✅ Tạo/chỉnh sửa/xóa bộ thẻ
- ✅ Thêm flashcard với mặt trước/sau
- ✅ Gán tag cho flashcard
- ✅ Tìm kiếm bộ thẻ
- ✅ Swipe-to-delete action

### Chế Độ Ôn Tập
- ✅ Flip card 3D với animation mượt
- ✅ 4 mức đánh giá: Again/Hard/Good/Easy
- ✅ Điều chỉnh due date tự động
- ✅ Shuffle cards khi start review

### Chế Độ Quiz
- ✅ Multiple choice (4 tùy chọn)
- ✅ Random câu hỏi từ bộ thẻ
- ✅ Hiển thị đáp án đúng/sai
- ✅ Điểm số & phần trăm đúng

### Thống Kê & Dashboard
- ✅ Thẻ ôn tập hôm nay
- ✅ Chuỗi ngày liên tiếp (Streak)
- ✅ Độ chính xác (%)
- ✅ Biểu đồ 7 ngày
- ✅ Tổng số thẻ/bộ/lần ôn

### UI/UX
- ✅ Material3 Design System
- ✅ Dark/Light theme
- ✅ Responsive layout
- ✅ Smooth animations
- ✅ Vietnamese localization

### Thông Báo
- ✅ WorkManager periodic reminder
- ✅ Notification với số thẻ cần ôn
- ✅ Intent quay lại app khi tap

---

## 🏗️ Architecture

```
Clean Architecture + MVVM + Feature-based Structure

┌─────────────────────────────────────┐
│     Presentation Layer              │
│  (Screens, ViewModels, Composables) │
└────────────────┬────────────────────┘
                 │
┌─────────────────┴────────────────────┐
│      Domain Layer                    │
│  (Use Cases, Repositories)           │
└────────────────┬────────────────────┘
                 │
┌─────────────────┴────────────────────┐
│      Data Layer                      │
│  (Room DB, API, Implementations)     │
└─────────────────────────────────────┘
```

### Tech Stack
| Component | Technology |
|-----------|-----------|
| UI | Jetpack Compose + Material3 |
| Architecture | MVVM + Clean Architecture |
| Database | Room + SQLite |
| Networking | Retrofit + Moshi |
| DI | Hilt |
| Async | Coroutines + Flow |
| Notifications | WorkManager |
| Build | Gradle Kotlin DSL |

---

## 📁 Project Structure

```
app/src/main/java/com/duong/udhoctap/
├── core/
│   ├── database/
│   │   ├── entity/              # 6 entities
│   │   ├── dao/                 # 4 DAOs
│   │   ├── converter/           # Type converters
│   │   └── AppDatabase.kt
│   ├── network/
│   │   └── DocumentApi.kt       # Retrofit API
│   ├── data/repository/         # 4 repositories
│   ├── ui/
│   │   ├── theme/              # Colors, Typography, Shapes
│   │   ├── components/         # DeckCard, QuizButton, etc
│   │   └── navigation/         # App routing
│   └── notification/
│       └── StudyReminderWorker.kt
├── feature/
│   ├── home/                   # Home screen
│   ├── deck/                   # Deck detail & add/edit
│   ├── review/                 # Review/flip mode
│   ├── quiz/                   # Quiz mode
│   └── stats/                  # Statistics
├── di/
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   ├── RepositoryModule.kt
│   └── (Hilt DI setup)
├── MainActivity.kt
└── UdHocTapApp.kt              # @HiltAndroidApp

Database: Room (1 version)
  - 6 entities registered
  - 4 DAOs with Flow-based queries
  - Cascade delete configured
```

---

## 🗄️ Database Schema

```sql
-- 6 Tables + Relationships

Deck (1:M) → Flashcard
  ├── id, name, description, color
  ├── createdAt, updatedAt
  └── Indices on id, updatedAt

Flashcard (M:1) → Deck, (1:M) → ReviewLog
  ├── id, deckId (FK), front, back
  ├── FSRS fields: stability, difficulty, retrievability
  ├── State: New/Learning/Review/Suspended
  ├── isDraft, dueDate
  └── Indices on deckId, dueDate

ReviewLog (M:1) → Flashcard
  ├── id, flashcardId (FK), rating (1-4)
  ├── reviewedAt, scheduledDays, elapsedDays
  ├── FSRS fields: stability, difficulty, state
  └── Indices on flashcardId, reviewedAt

Tag (M:M) ← DeckTagCrossRef
Tag (M:M) ← FlashcardTagCrossRef
```

---

## 🔌 API Endpoints (Backend Phase 2+)

```
POST /api/v1/generate-flashcards
  ├── Request: multipart/form-data (PDF/TXT file)
  └── Response: { flashcards: [{ front, back }] }
```

---

## 📊 Component Overview

### Screens (6)
| Screen | Purpose | Status |
|--------|---------|--------|
| HomeScreen | Deck list + search + FAB | ✅ 100% |
| DeckDetailScreen | Flashcard list + actions | ✅ 100% |
| AddEditFlashcardScreen | Create/edit flashcard | ✅ 100% |
| ReviewScreen | Flip-based learning | ✅ 100% |
| QuizScreen | Multiple choice quiz | ✅ 100% |
| StatsScreen | Statistics dashboard | ✅ 100% |

### ViewModels (6)
| ViewModel | State | Status |
|-----------|-------|--------|
| HomeViewModel | Decks, search query | ✅ 100% |
| DeckDetailViewModel | Deck, flashcards, AI | ✅ 100% |
| AddEditFlashcardViewModel | Flashcard, tags | ✅ 100% |
| ReviewViewModel | Cards, progress, ratings | ✅ 100% |
| QuizViewModel | Questions, score | ✅ 100% |
| StatsViewModel | Streak, accuracy, chart | ✅ 100% |

### Components (2)
| Component | Purpose |
|-----------|---------|
| DeckCard | Deck preview card |
| QuizOptionButton | Quiz answer button |
| FlipCard | Animated flip card |

---

## ✅ Testing & Quality

- **Compilation**: 0 errors, 0 warnings ✅
- **Dependencies**: All pinned to stable versions
- **Room Database**: Proper entity & DAO setup
- **Coroutines**: Flow-based reactive queries
- **Memory**: Optimized with viewModelScope
- **UI**: Compose best practices

---

## 🔄 Development Workflow

### Branch Strategy
```
main (production)
  └── develop (staging)
        └── feature/xyz (feature branches)
```

### Commit Convention
```
feat: add X
fix: resolve X
refactor: improve X
docs: update X
test: add tests for X
```

### Build & Deploy
```bash
# Development
./gradlew assembleDebug

# Release
./gradlew assembleRelease

# ProGuard obfuscation enabled for release
```

---

## 🐛 Known Issues & Limitations

- Backend không hoàn thiện (Phase 2+)
- FSRS algorithm chưa full implementation (basic scheduling hiện tại)
- Offline-first hoàn toàn (sync cloud ở Phase 3)
- Analytics chưa setup

---

## 🚀 Roadmap

### Phase 1 ✅ Complete
- [x] MVP: Flashcard + Review + Quiz + Stats
- [x] Database schema
- [x] Clean Architecture
- [x] UI/UX design

### Phase 2 🔄 In Progress
- [ ] FastAPI backend
- [ ] Document summarization
- [ ] AI flashcard generation
- [ ] API integration testing

### Phase 3 ⏳ Future
- [ ] FSRS full implementation
- [ ] Cloud sync (Firebase)
- [ ] Collaborative study groups
- [ ] Mobile app on Play Store

### Phase 4 ⏳ Long-term
- [ ] Web version
- [ ] Analytics dashboard
- [ ] Advanced scheduling
- [ ] Multi-language support

---

## 📚 Resources

- **Android Docs**: https://developer.android.com
- **Jetpack Compose**: https://developer.android.com/compose
- **Room**: https://developer.android.com/training/data-storage/room
- **Hilt**: https://developer.android.com/training/dependency-injection/hilt-android
- **Kotlin Coroutines**: https://kotlinlang.org/docs/coroutines-overview.html

---

## 👥 Contributors

- **Duong** - Project Lead, Full-stack Developer

---

## 📄 License

MIT License - Xem [LICENSE](LICENSE) file

---

## 💬 Feedback

Góp ý, báo lỗi, hoặc đề xuất tính năng:
```bash
# Tạo issue
# Hoặc email: duong@example.com
```

---

## 🎓 Learning Resources Included

Project này giới thiệu các khái niệm:
- ✨ Modern Android development (Compose)
- 🏗️ Clean Architecture & MVVM
- 💾 Room Database & SQLite
- 🔌 REST API với Retrofit
- 💉 Dependency Injection với Hilt
- ⚡ Coroutines & Flow (reactive programming)
- 🎨 Material Design 3
- 📱 Responsive UI design

---

**⭐ Hãy star nếu bạn thấy project hữu ích!**

**Bắt đầu học tập thông minh ngay hôm nay! 🚀**
