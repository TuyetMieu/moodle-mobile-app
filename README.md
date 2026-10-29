# Moodle USTH Mobile App

Ứng dụng Android theo dõi việc học cho sinh viên USTH, kèm giao diện riêng cho giáo viên.
App viết bằng Kotlin và Jetpack Compose, gọi tới một backend REST API chạy bằng Node.js.
Toàn bộ dữ liệu là **dữ liệu mô phỏng** (môn học, bài tập, thời khóa biểu, thông báo, tài khoản).

## Tính năng

**Sinh viên**
- Trang chủ: đếm ngược deadline, mục "Ưu tiên của bạn" (bài chưa xong, chưa quá hạn, gần hạn nhất), tiến độ học kỳ.
- Môn học: danh sách môn, chi tiết môn với thành phần điểm, bài tập, tài liệu, bảng tin lớp.
- Thời khóa biểu hằng tuần, theo dõi bài tập (cần làm, gấp, đã xong), đánh dấu hoàn thành.
- Điểm theo thang 20, điểm trung bình học kỳ có trọng số tín chỉ, tải báo cáo điểm CSV.
- Thông báo hai luồng: *Ngành của bạn* và *Thầy cô & toàn trường*; nhắc deadline và đồng bộ chạy nền.
- Cài đặt: giao diện Hệ thống / Sáng / Tối, ngôn ngữ Tiếng Việt / English / Français.

**Giáo viên**
- Tạo, sửa, xoá bài tập; đặt thang điểm theo thành phần (tổng 100%) và bonus cho từng môn.
- Xem thống kê nộp bài, điểm từng sinh viên; máy chấm tự động hoặc bấm "Chấm ngay".
- Đăng tin cho lớp, cho cả ngành hoặc toàn trường.

Đăng nhập xong, app tự chuyển sang giao diện sinh viên hoặc giáo viên theo vai trò của tài khoản.

## Công nghệ

| Phần | Chi tiết |
|---|---|
| App | Kotlin 2.2, Jetpack Compose (Material 3), ViewModel, WorkManager |
| Android | `minSdk` 24, `targetSdk` / `compileSdk` 37, Android Gradle Plugin 9.4 |
| Backend | Node.js ≥ 22.13, `node:http`, SQLite có sẵn của Node (`node:sqlite`), không cần `npm install` |

## Cấu trúc repo

```
moodle-usth-mobile-app/
├── app/                     # Ứng dụng Android
│   └── src/main/
│       ├── java/com/example/moodle_usth_mobile_app/
│       │   ├── data/        # local (phiên, cài đặt), model, remote (API), repository
│       │   ├── reminders/   # nhắc deadline, đồng bộ nền, nội dung thông báo
│       │   ├── ui/          # screens, teacher, components, dialogs, theme, ViewModel
│       │   └── MainActivity.kt
│       ├── res/             # tài nguyên; chuỗi ở values (mặc định), values-vi, values-fr
│       └── assets/sample_bootstrap.json   # dữ liệu mẫu cho chế độ xem thử
├── backend/                 # REST API (Node.js + SQLite), xem backend/README.md
├── docs/                    # tài liệu thiết kế và giải thích code
├── build.gradle.kts, settings.gradle.kts, gradle/
└── gradlew, gradlew.bat
```

## Chạy dự án

### 1. Chạy backend

Cần Node.js ≥ 22.13.

```bash
cd backend
npm start          # http://localhost:8080, lần đầu tự tạo và seed data/moodle-usth.db
npm run reset      # seed lại để làm mới deadline
npm test           # chạy test API
```

Deadline được seed tương đối theo thời điểm chạy, nên sau khoảng một ngày nhiều bài sẽ quá hạn.
Chạy `npm run reset` để làm mới. Chi tiết về API, biến môi trường và quy tắc tính điểm xem
[backend/README.md](backend/README.md).

### 2. Chạy app Android

Mở thư mục gốc bằng Android Studio, để Gradle sync xong rồi chạy cấu hình `app`. Hoặc dùng dòng lệnh:

```bash
./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug
./gradlew installDebug         # cài lên thiết bị / emulator đang kết nối
```

Địa chỉ backend nằm trong `BuildConfig.API_BASE_URL`, mặc định `http://10.0.2.2:8080`
(địa chỉ để emulator truy cập máy host). Với điện thoại thật cùng Wi‑Fi, thêm vào `local.properties`:

```properties
moodle.apiUrl=http://192.168.x.x:8080
```

`192.168.x.x` là địa chỉ LAN mà backend in ra khi khởi động. Có thể cần mở cổng 8080 trên Windows Firewall.
Địa chỉ này cũng nhập được ở mục *Máy chủ* trên màn đăng nhập.

App cần các quyền: `INTERNET`, `ACCESS_LOCAL_NETWORK` (Android 17 trở lên, để gọi tới địa chỉ mạng nội bộ),
`POST_NOTIFICATIONS` và `RECEIVE_BOOT_COMPLETED` (nhắc deadline).

### Tài khoản demo

Mật khẩu chung: `usth2025`.

| Tên đăng nhập | Vai trò |
|---|---|
| `BA22-041`, `BA22-087` | Sinh viên |
| `gv-minh`, `gv-linh`, `gv-quang` | Giáo viên |

Đăng nhập sai 5 lần trong 15 phút sẽ bị khóa tạm thời.

## Tài liệu

| File | Nội dung |
|---|---|
| [backend/README.md](backend/README.md) | Chạy backend, danh sách API, quy tắc nghiệp vụ |
| [docs/BACKEND_API.md](docs/BACKEND_API.md) | Giải thích code backend và tài liệu API |
| [docs/GIAI_THICH_BACKEND.md](docs/GIAI_THICH_BACKEND.md) | Giải thích từng file, từng hàm của backend |
| [docs/GIAI_THICH_FRONTEND.md](docs/GIAI_THICH_FRONTEND.md) | Giải thích app Android: hàm cốt lõi và các vòng lặp |
| [docs/CODE_WALKTHROUGH.md](docs/CODE_WALKTHROUGH.md) | Giải thích code và workflow của app |
| [docs/CHUC_NANG_VA_CSDL.md](docs/CHUC_NANG_VA_CSDL.md) | Chức năng và cơ sở dữ liệu |
| [docs/CO_SO_DU_LIEU.md](docs/CO_SO_DU_LIEU.md) | Các bảng và quan hệ trong cơ sở dữ liệu |
| [docs/HUONG_DAN_TEST.md](docs/HUONG_DAN_TEST.md) | Hướng dẫn test giao diện app và API backend |
| [docs/NANG_CAP_V2.md](docs/NANG_CAP_V2.md) | Đợt nâng cấp v2: giáo viên, thang 20, thông báo, dark mode, 3 ngôn ngữ |
| [docs/CAI_TIEN.md](docs/CAI_TIEN.md) | Đợt cải tiến 26/10/2026 |
| [docs/DON_GIAN_HOA.md](docs/DON_GIAN_HOA.md) | Đợt đơn giản hóa code 27/10/2026 |
