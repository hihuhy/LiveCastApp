# LiveCast — App phát video có sẵn thành livestream (giống chức năng GoStream)

## Kết quả phân tích file bạn gửi

File `co_godream_gostream_2_13_516.apks` là bộ **Android App Bundle** (nhiều split APK) của app
**GoStream** (package `co.godream.gostream`, nhà phát triển VIREF COMPANY LIMITED).

Chức năng chính của app gốc: cho phép người dùng chọn **một video đã quay/dựng sẵn** trong máy,
rồi "giả lập" phát nó như một buổi **livestream thật** lên Facebook, YouTube hoặc nền tảng khác,
thông qua giao thức **RTMP**. Về mặt kỹ thuật, app gốc được viết bằng **React Native**, tích hợp:
- Đăng nhập Facebook (SDK Facebook) để lấy stream key nhanh
- Firebase (thông báo đẩy, phân tích)
- Google Play Billing (mua gói premium)
- Quảng cáo AdMob

## App tôi đã dựng: LiveCast

Vì mình không thể build/nộp thẳng một file `.apk` đã biên dịch từ môi trường này (không có quyền
truy cập Android SDK/Maven của Google trong sandbox), mình đã viết cho bạn **toàn bộ mã nguồn dự án
Android (Kotlin)** implement đúng chức năng lõi trên, để bạn mở bằng Android Studio và bấm Run là
có app chạy trên điện thoại.

### Tính năng
1. Chọn video từ thư viện máy.
2. Chọn nhanh nền tảng (Facebook / YouTube / Twitch / Tùy chỉnh) → tự điền sẵn địa chỉ RTMP server.
3. Nhập Stream Key (khóa luồng riêng do Facebook/YouTube cấp cho từng buổi live).
4. Bật/tắt tự động lặp lại video khi phát hết.
5. Nhấn "Bắt đầu phát trực tiếp" → app chạy Foreground Service đẩy luồng RTMP liên tục,
   có thông báo trạng thái (đang kết nối / đang LIVE / lỗi / đã ngắt).

### Công nghệ dùng
- Kotlin, Android Views (ViewBinding) — không dùng React Native để đơn giản hoá build.
- Thư viện mã nguồn mở **[RootEncoder](https://github.com/pedroSG94/RootEncoder)** (kế thừa từ
  `rtmp-rtsp-stream-client-java`) để đọc video local và đẩy luồng RTMP — đây là thư viện phổ biến,
  nhiều app "livestream video có sẵn" trên Google Play cũng dùng cơ chế tương tự.

### Cách 1 — Build hoàn toàn từ điện thoại, KHÔNG cần máy tính (khuyên dùng)

Project này đã có sẵn file `.github/workflows/build.yml` — GitHub sẽ tự động biên dịch ra file
APK giúp bạn trên máy chủ miễn phí của họ, bạn chỉ cần thao tác trên điện thoại.

**Bước 1 — Cài Termux** (chỉ dùng để đẩy code lên, rất nhẹ, không cần cài Android SDK gì cả):
- Cài app **Termux** từ **F-Droid** (KHÔNG dùng bản trên Google Play vì đã cũ/lỗi).
- Mở Termux, chạy lần lượt:
  ```
  pkg update -y
  pkg install git -y
  termux-setup-storage
  ```
  (bước cuối sẽ hiện hộp thoại xin quyền truy cập bộ nhớ, bấm Cho phép)

**Bước 2 — Giải nén project vào bộ nhớ máy:**
- Dùng app Quản lý tệp (Files) có sẵn trên điện thoại, giải nén file `LiveCastApp.zip` (mình gửi
  ở tin nhắn trước) vào thư mục `Download`, ra được thư mục `Download/LiveCastApp`.

**Bước 3 — Đẩy code lên GitHub (thao tác trong Termux):**
```
cd /sdcard/Download/LiveCastApp
git init
git add .
git commit -m "init"
git branch -M main
```
- Mở trình duyệt trên điện thoại, vào **github.com** → đăng ký/đăng nhập tài khoản (miễn phí).
- Vào **github.com/new** → đặt tên repo (vd `LiveCastApp`) → chọn **Public** → KHÔNG tick "Add
  README" → bấm **Create repository**.
- Vào **Settings → Developer settings → Personal access tokens → Tokens (classic)** → Generate
  new token, tick quyền `repo` → copy token (chuỗi ký tự dài, dùng thay mật khẩu ở bước sau).
- Quay lại Termux, chạy tiếp:
  ```
  git remote add origin https://github.com/<ten-tai-khoan>/LiveCastApp.git
  git push -u origin main
  ```
  Khi hỏi username/password: nhập tên tài khoản GitHub, và dán **token** vừa tạo vào ô password.

**Bước 4 — Lấy file APK:**
- Vào lại trang repo trên GitHub bằng trình duyệt → tab **Actions** → sẽ thấy workflow "Build
  APK" đang chạy tự động (mất khoảng 2-5 phút).
- Khi chạy xong (dấu ✅ xanh) → bấm vào lần chạy đó → kéo xuống mục **Artifacts** → tải file
  `LiveCast-debug-apk.zip` về điện thoại.
- Dùng app Quản lý tệp giải nén ra được file `app-debug.apk`.
- Bấm vào file đó để cài (nếu máy hỏi, cho phép "Cài ứng dụng không rõ nguồn gốc"). Xong — app
  **LiveCast** sẽ xuất hiện trên màn hình chính, chạy hoàn toàn trên điện thoại của bạn.

*Từ lần thứ 2 trở đi, mỗi khi bạn sửa code và muốn build lại, chỉ cần lặp lại `git add . && git
commit -m "..." && git push` trong Termux — không cần tạo lại token/repo nữa.*

### Cách 2 — Build bằng máy tính (nếu có sẵn máy tính)
1. Cài **Android Studio** (bản mới nhất) trên máy tính.
2. Mở thư mục `LiveCastApp` này bằng "Open an existing project".
3. Chờ Gradle sync xong (lần đầu cần internet để tải thư viện qua `jitpack.io`).
4. Cắm điện thoại Android (bật USB Debugging) hoặc dùng máy ảo, bấm nút ▶ Run.
5. App cài lên máy tên **LiveCast**.

### Lấy Stream Key ở đâu?
- **Facebook**: vào Studio phát trực tiếp trên Facebook (trên web hoặc app Creator Studio) →
  tạo buổi live mới → copy "Server URL" và "Stream Key".
- **YouTube**: YouTube Studio → Tạo video → Phát trực tiếp → copy URL luồng + Khóa luồng.

### Cấu trúc thư mục
```
LiveCastApp/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/livecast/
│       │   ├── ui/MainActivity.kt        (màn hình chính)
│       │   ├── service/StreamService.kt  (đẩy luồng RTMP nền)
│       │   └── model/StreamProfile.kt, ProfileStore.kt (lưu cấu hình kênh)
│       └── res/ (layout, strings, icon...)
├── build.gradle
└── settings.gradle
```

### Lưu ý quan trọng
- Việc phát lại video như thể đang "live" cần tuân thủ **điều khoản sử dụng của từng nền tảng**
  (Facebook, YouTube...). Một số nền tảng có quy định riêng về nội dung dựng sẵn gắn nhãn "đã ghi
  hình trước". Bạn nên đọc kỹ chính sách của nền tảng mình định phát trước khi dùng cho mục đích
  thương mại.
- Đây là bản khung lõi (MVP) tập trung vào đúng tính năng cốt lõi mà bạn hỏi. Các tính năng phụ của
  app gốc (đăng nhập Facebook, quảng cáo, thanh toán trong app, đa ngôn ngữ...) chưa được thêm vào,
  mình có thể bổ sung tiếp nếu bạn cần.
