# Mirage — Mobile LED Controller

Stitch project: https://stitch.withgoogle.com/projects/6847000307657413933

Phạm vi: đăng nhập và điều khiển thiết bị LED qua BLE. Thiết kế tiếng Việt, nhận diện Mirage xanh sage #89C89B, nền sáng, viền đen, điểm nhấn pixel.

8 màn hình: Đăng nhập; Thiết bị của tôi; Thêm thiết bị; Điều khiển LED; Nội dung LED; Vẽ pixel; Chạy chữ; Tài khoản & thiết bị.

Hai màn tham chiếu mới nhất cho thay đổi nút chức năng và nhập ảnh: **02 Thiết bị — Nút chức năng V2** (`64d81c2cefd74b3aadc403ba30279080`) và **10 DIY pixel — Nhập ảnh V2** (`c0af44c538dd4e1cb1405cdfa19c92a0`). Home dùng ba ô chức năng DIY pixel / Chạy chữ / Nhập ảnh; DIY có nút Nhập ảnh từ thư viện ngay phía trên canvas.

Nội dung LED chỉ phục vụ gửi bản vẽ/nội dung có sẵn lên thiết bị. Không có cửa hàng, thanh toán, đơn hàng hoặc cộng đồng. Giao diện đã được triển khai trong Kotlin; chi tiết chức năng, kiểm tra và giới hạn tích hợp được ghi trong README.md.

Nguồn tham chiếu đã đọc: FE globals.css, Header.tsx, LoginForm.tsx, Android navigation và LedControlState. Figma KMB4v4HtenNMtu8D0gyP13 vẫn báo hết hạn mức MCP Starter; chưa đối chiếu trực quan được file gốc.

Đã kiểm tra ảnh màn hình đăng nhập và điều khiển đầu tiên; yêu cầu chỉnh bỏ các chi tiết debug, FPS/firmware, cài đặt dòng điện không có căn cứ, nhãn OTA và thông báo mất kết nối xuất hiện sai ở trạng thái đang kết nối.

## V3 — controls, content, phone projection

Project: https://stitch.withgoogle.com/projects/6847000307657413933

Latest corrected screens:
- `debab6e35d7040628dbcb1a6687a837d` — 04 Điều khiển LED V3 (Live Wheel)
- `16ede9ccb4d84b39806a35bf3f8ee8b7` — 05 Nội dung LED — Gallery V3
- `127a66f4215b407998d4a627e4aee01b` — 05 Nội dung LED — Inventory V3
- `a123e58f5bf14374a919ded9660bb935` — 14 Chiếu ảnh trực tiếp V3 (Chuẩn hóa)

Implementation: local DIY Gallery; separate mocked Character inventory using FE catalog IDs and previews; standalone phone photo picker → 64×32 processing → BLE projection. RGB/HSV wheel sends latest control values with queue pacing; hardware brightness is separate from image brightness. No real entitlement backend, animation binary, BLE latency or LED capacity claims. GIF is first frame only.

## V4 — Static marquee auto-fit

`c4a85d787a524dfc92bfa93d980e8da9` — 07 Chạy chữ — Tự vừa màn hình V4. Mobile implements exact 64×32 pixel layout with5×7 /10×14 presets, word wrapping, size2→1 fallback, centered lines, and overflow validation. The rendered frame itself is uploaded through the existing projection protocol, so no firmware flash is needed. Existing scroll/blink JSON commands retain size1/2.

## V5 — DIY LIVE

`94be36dea83249d2a70cbe6a320540a7` — DIY Pixel LIVE V5. LIVE status/switch, clear canvas, existing pixel tools and phone photo import. Two separate sticky actions: Lưu lên thiết bị LED (SPIFFS, confirmed by device) and Lưu vào Gallery (phone, offline). Existing firmware opcodes are reused; no firmware update is required. Dense changes synchronize progressively; LIVE itself never saves Flash.

## V6 — Separate Effect feature

- `02e49fc7691d427995ae6b825fc63569` — 01 Thiết bị của tôi — Home V6
- `49b9215577e045c7a3359f47334c1999` — 02 Điều khiển LED — Tối giản V6
- `36a111275c5344489639694758cefc5c` — 03 Effect — Màu đơn sắc & Hiệu ứng V6

Root functions form a2×2 grid (DIY pixel, Chạy chữ, Nhập ảnh, Effect). Control is power plus one brightness slider only. Effect is a separate route with solid color, breathing, rainbow, chase and music modes; color wheel and advanced RGB/HSV remain here. No power/brightness controls on Effect and no effect choices on Control. Final Stitch edit removed automatic preset brightness buttons and a manual apply button to match the app's live controls.
