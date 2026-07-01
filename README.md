# HiSync 🎸

**HiSync** là app Android giúp band nhạc / club âm nhạc quản lý lịch tập, repertoire bài hát và task luyện tập cá nhân — thay thế cho việc nhắc nhau qua group chat dễ quên, dễ loạn.

> Đây là repo **frontend (Android client)**. Backend (Spring Boot + PostgreSQL) nằm ở repo riêng: [`hisync-backend`](https://github.com/mystiquekem/hisync-backend).

---

## ✨ Tính năng chính

- **Đăng ký / Đăng nhập** kèm quên mật khẩu qua OTP gửi email.
- **Tạo band / Tham gia band** bằng invite code, không cần admin duyệt.
- **Lịch tập theo tuần** — vuốt qua lại giữa các tuần để xem session sắp tới & đã qua.
- **Repertoire bài hát** tích hợp YouTube Data API — search bài, lưu video tham khảo kèm thumbnail.
- **Task luyện tập cá nhân** — leader giao task cho từng member, member nộp **recording** (qua Cloudinary) làm bằng chứng đã tập.
- **Phân quyền theo vai trò**: `member` / `leader` / `admin`.

## 🛠️ Tech stack

| Layer | Công nghệ |
|---|---|
| Ngôn ngữ | Java (Android native) |
| Network | Retrofit2 + OkHttp |
| Ảnh / thumbnail | Glide |
| Lưu recording | Cloudinary |
| Tìm bài hát | YouTube Data API v3 |
| UI lịch tuần | ViewPager2 + `WeekPagerAdapter` |
| Build | Gradle (Kotlin DSL) |

Backend tương ứng: Spring Boot, Spring Data JPA, PostgreSQL, BCrypt, JavaMailSender (OTP qua Gmail SMTP).

## 📂 Cấu trúc project

```
app/src/main/java/com/example/hisync/
├── api/        # Retrofit service interface (HisyncApi) + client setup
├── dto/        # Request/response models map với backend
├── model/      # Data model dùng trong app
├── fragments/  # Các màn hình: Home, Schedule, Songs, Tasks, Profile...
└── schedule/   # WeekPagerAdapter, WeekFragment cho lịch tuần
```

## 🚀 Setup & chạy thử

1. **Clone repo:**
   ```bash
   git clone https://github.com/mystiquekem/hisync.git
   cd hisync
   ```

2. **Cấu hình API key & base URL** — tạo file `local.properties` ở root project (file này **không** commit lên git):
   ```properties
   YOUTUBE_API_KEY=your_youtube_data_api_key
   BASE_URL=http://10.0.2.2:8080/   # IP của backend khi chạy trên emulator
   ```
   Các giá trị này được expose vào code qua `BuildConfig`, nên nhớ đừng hardcode key trực tiếp trong source.

3. **Chạy backend trước** (xem hướng dẫn ở repo [`hisync-backend`](https://github.com/mystiquekem/hisync-backend)) — app cần backend sống để login/đăng ký được.

4. **Build & run** bằng Android Studio (mở project, sync Gradle, Run ▶️), hoặc CLI:
   ```bash
   ./gradlew assembleDebug
   ```

## ⚠️ Lưu ý bảo mật

- Không commit `local.properties`, `google-services.json` thật, hay bất kỳ API key/secret nào lên public repo.
- Backend hiện đang cho phép tất cả request (`permitAll()`), đang trong quá trình hardening sang token-based auth — xem mục Future Work trong [thesis](#) (đang viết 😅).

## 📄 License

TODO — thêm license phù hợp (MIT/Apache-2.0/...) trước khi public chính thức.

## 🙋 Liên hệ

Tác giả: Nguyễn Thái An — đồ án tốt nghiệp, USTH, 2026.
