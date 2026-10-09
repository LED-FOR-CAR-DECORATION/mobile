# Car LED Ambient & Projection - Mobile Application

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
3. **Cửa Hàng Phụ Kiện & Hiệu Ứng (`StoreScreen`, `StoreViewModel`)**:
   - Xem danh mục sản phẩm LED và gói hoạt ảnh nhân vật.
   - Tương thích Flow 1 E-commerce Scope.
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
