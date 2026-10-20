# Car LED Ambient & Projection - Mobile Application

## Mirage mobile — Stitch implementation

Giao diện chạy hiện tại dùng thiết kế [Mirage Mobile LED Controller](https://stitch.withgoogle.com/projects/6847000307657413933): nền sáng, xanh sage, viền đen, font Inter (giấy phép OFL kèm tại `app/src/main/assets/licenses/`). Chỉ có ba tab **Thiết bị / Nội dung / Tài khoản**, cùng đăng nhập, quét BLE, điều khiển và chạy chữ. Luồng cửa hàng/tin nhắn cũ không còn nằm trong điều hướng ứng dụng.

**Pixel studio:** chọn 32×32, 64×32 hoặc 64×64 trước khi tạo mới; bút/tẩy 1–4 px; nét kéo liền mạch; lấy màu; bảng màu có thể thêm/đổi tên/xóa và sửa các mẫu màu qua khung màu/RGB/HSV; hình đường thẳng/chữ nhật/elip có viền hoặc tô kín; đổ vùng kín; đối xứng hai trục; bật/tắt lưới; hoàn tác/làm lại. Một ngón vẽ, hai ngón di chuyển/thu phóng. Hình dạng được xem trước trong lúc kéo, thả tay để ghi nhận.

**Văn bản:** là lớp đối tượng riêng trên pixel, chỉ sửa khi mở công cụ Văn bản, chọn khối và chỉnh tọa độ X/Y. Dùng ASCII tiếng Anh; bộ glyph 3×5 vẽ thủ công hiển thị chữ hoa, cỡ 5×7 và 7×9 phóng theo nearest-neighbor. Bút/tẩy không sửa lớp chữ. Gửi LED ghép các lớp nhưng bản nháp vẫn giữ khối chữ để sửa tiếp.

**Nhập ảnh:** Android Photo Picker → giải mã ảnh có giảm kích thước bộ nhớ → xem trước pixel ở đúng kích thước canvas → chọn thu vừa hoặc cắt đầy/căn vị trí → xác nhận thay lớp pixel. Lớp chữ giữ nguyên. Toàn bộ lần nhập có thể hoàn tác.

Mục **Tạo theo cách của bạn** dùng hàng ba nút chức năng có biểu tượng (DIY pixel / Chạy chữ / Nhập ảnh) theo bố cục launcher kiểu Divoom. Trong DIY có nút **Nhập ảnh từ thư viện** ngay phía trên canvas, mở thẳng bộ chọn ảnh điện thoại; không cần mở menu Thêm hoặc kết nối BLE.

**Lưu nháp:** nút Lưu nháp lưu thiết kế/tọa độ chữ trên điện thoại; mở lại ở Nội dung. Bảng màu lưu riêng. Thoát bản vẽ chưa lưu có xác nhận. Vẽ không tự gửi BLE hay xóa LED đang phát.

**Giới hạn tích hợp:** API đăng nhập vẫn là `MockApiService` có sẵn của repo, chưa phải xác thực backend thật. Firmware trong `IoT-Device/include/config.h` và giao thức upload hiện dùng **64×32**; canvas 32×32 và 64×64 tạo/lưu được nhưng bị chặn gửi, không tự ép kích thước. Chỉ báo nhận nội dung thành công khi có telemetry `UploadComplete`; chạy chữ chờ `MarqueeActive`. Chưa có đồng bộ thư viện Character từ backend hoặc đọc danh sách nội dung từ bộ nhớ LED; tab Nội dung hiển thị bản nháp thật trên điện thoại. Xem trước ở màn thiết bị/điều khiển là minh họa, không phải ảnh đọc ngược từ LED.

Build và kiểm tra: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`. Test UI có thể chạy bằng `:app:connectedDebugAndroidTest` trên máy ảo Android; đặt `ANDROID_SERIAL` để chọn đúng máy thử.

**Quét BLE trên Android 6–11:** cần cả quyền Vị trí và công tắc dịch vụ Vị trí của điện thoại đang bật. Android 12+ dùng quyền Thiết bị ở gần. Màn Thêm thiết bị kiểm tra điều kiện này trước khi quét, có nút mở cài đặt tương ứng và quét lại sau khi quay về từ cài đặt; không báo danh sách rỗng khi thiếu điều kiện quét. Không tự bật Vị trí của người dùng.

Danh sách quét chỉ giữ thiết bị quảng bá UUID dịch vụ LED của dự án hoặc tên bắt đầu bằng `CarLED` (dự phòng khi UUID/tên nằm trong scan response). Không nhận thiết bị chỉ mang tên ESP32 chung chung; không lưu/hiển thị/ghi log các thiết bị khác. Quét radio vẫn không gò bộ lọc Android để giữ tương thích với gói quảng bá ESP32; sau khi lọc, LED được sắp theo tín hiệu mạnh nhất. Đây là tìm thiết bị tương thích dự án, không phải nhận diện mọi sản phẩm LED trên thị trường.

Ứng dụng di động Native Android (Kotlin + Jetpack Compose) dành cho dự án **LED Windshield Projection System with Characters**.

Dự án được xây dựng theo kiến trúc chuẩn **MVVM (Model - View - ViewModel)** kết hợp **Clean Architecture** (Google Recommended Architecture).

---

## 📁 Cấu trúc Thư mục Dự án

```
mobile/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # Cấu hình quyền BLE (Android 12+ và API <= 30)
│   │   ├── java/com/carled/app/
│   │   │   ├── CarLedApplication.kt     # Application entrypoint & ServiceLocator init
│   │   │   ├── MainActivity.kt          # Host Activity xử lý cấp quyền & Compose Nav
│   │   │   ├── core/                    # Dùng chung: Constants, Resource, DI
│   │   │   ├── domain/                  # Tầng nghiệp vụ (Pure Kotlin - Model, Repo, UseCases)
│   │   │   ├── data/                    # Tầng dữ liệu (BLE Driver, Remote API, Preferences)
│   │   │   └── presentation/            # Tầng giao diện MVVM (Screens, ViewModels, Themes)
│   │   └── res/                         # Strings, Colors, Themes XML
│   └── build.gradle.kts
├── gradle/                              # Version Catalog & Wrapper
├── build.gradle.kts                     # Root build file
├── settings.gradle.kts
├── ARCHITECTURE.md                      # Tài liệu giải thích kiến trúc MVVM chi tiết
└── README.md
```

---

## 🚀 Các Tính Năng Hiện Có

1. **Quét & Kết Nối Thiết Bị BLE (`ScanScreen`, `ScanViewModel`)**:
   - Tự động lọc thiết bị mang Service UUID chuẩn của ESP32 Car LED.
   - Cơ chế tự dừng sau 10s tránh Scan Throttling của Android OS.
   - Phòng chống triệt để lỗi Status 133 (`GATT_ERROR`).
2. **Điều Khiển Hiệu Ứng & Màu Sắc (`ControlScreen`, `ControlViewModel`)**:
   - Điều khiển Bật/Tắt nguồn (Power).
   - Thanh trượt độ sáng (0 - 255) và tốc độ hiệu ứng (1 - 100).
   - Chọn chế độ hiển thị (Màu tĩnh, Nhịp thở, Sóng cầu vồng, Dòng chảy đuổi màu, Nháy theo nhạc xe, Chiếu nhân vật).
   - Bảng màu đèn xe thời gian thực với cơ chế Throttling 40ms (~25 FPS) tránh nghẽn buffer GATT.
3. **Thư viện bản vẽ và Pixel Studio (`StudioScreen`, `StudioViewModel`)**:
   - Tạo, lưu và mở lại bản nháp pixel trên điện thoại.
   - Nhập ảnh, quản lý bảng màu, lớp chữ, đối xứng và các công cụ vẽ.
   - Màn cửa hàng cũ không còn trong luồng ứng dụng.
4. **Xác Thực Người Dùng (`LoginScreen`, `LoginViewModel`)**:
   - Quản lý phiên đăng nhập và token người dùng.

---

## 🛠 Hướng Dẫn Mở Dự Án & Chạy Thử

1. Mở **Android Studio** (Hedgehog / Iguana / Jellyfish hoặc mới hơn).
2. Chọn **Open** -> Điều hướng tới thư mục `D:\SEP\mobile`.
3. Chờ Gradle đồng bộ (Sync Project with Gradle Files).
4. Kết nối điện thoại Android thật (hoặc máy ảo hỗ trợ BLE) và nhấn **Run (Shift + F10)**.

---

## 📖 Tham Khảo Thêm
- [ARCHITECTURE.md](ARCHITECTURE.md): Xem sơ đồ luồng dữ liệu 1 chiều (UDF) và hướng dẫn viết thêm UseCase/ViewModel mới.
- `D:\SEP\IoT-Device\docs\android_ble_guide.md`: Cẩm nang tối ưu Bluetooth BLE trên Android.
- `D:\SEP\IoT-Device\docs\api_protocol.md`: Đặc tả định dạng khung nhị phân và JSON.
2

## Mobile content and direct photo projection (October 2026)

- Nội dung → Gallery stores local DIY documents, including text objects. Inventory uses `CharacterInventoryRepository` with a separate mock adapter and the FE Character/UserEntitlement IDs; BE has no implemented ownership endpoint yet. Three previews are copied from FE assets. Purchased status is demonstration data, never a real entitlement check. Inventory currently projects a static preview; real animation binaries remain a BE integration task.
- Home → Nhập ảnh now opens a standalone system photo picker flow, without creating a DIY canvas. It resamples to firmware 64×32, supports Stretch/Cover/Contain, RGB565 Floyd–Steinberg dithering, image brightness/contrast and confirms upload using BLE telemetry. GIF uses its first frame. DIY keeps its own image import tool.
- Device controls include an HSV wheel, RGB/HSV entry and hardware brightness presets. A latest-per-control queue keeps the final dragged value and sends controls sequentially; image brightness is separate from device brightness.

### Static marquee auto-fit

The marquee screen exposes 5×7 and 10×14 glyph presets. Static mode centers and word-wraps the entire message within 64×32, reducing the preferred 10×14 preset to 5×7 if necessary. It uses up to four rows of readable 5×7 glyphs; longer text must be shortened or scrolled, never silently truncated or scaled below a pixel. Preview and transmitted RGB565 use the same layout. Static text uses the existing single-frame projection upload and waits for UploadComplete/UploadError; existing firmware does not need a flash. This writes the current projection slot, as photo upload does. Scroll/blink retain firmware marquee commands with size1/2. Font data derives from the firmware's Adafruit GFX classic ASCII font; license is bundled under assets/licenses/Adafruit-GFX.txt.
