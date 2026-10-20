# Mirage — Bộ prompt thiết kế mobile cho Stitch

## Cơ sở và cách dùng

Đã đối chiếu FE/src/app/globals.css, Header.tsx, catalog sản phẩm/Character, các route customer và source Android/navigation, control, canvas, marquee, upload. Figma tham chiếu: https://www.figma.com/design/KMB4v4HtenNMtu8D0gyP13/LEDSCREEN. Chưa đọc được nội dung Figma vì MCP báo hết hạn mức Starter; không coi tài liệu này là bản đối chiếu pixel-perfect với Figma.

Định hướng: ứng dụng Android dành cho khách hàng Mirage, kết hợp điều khiển LED và mua sản phẩm/Character. Staff, manager, admin tiếp tục ở web. Đây là đề xuất thiết kế mới, không phải tuyên bố tất cả luồng đã tích hợp backend. Mobile hiện có cả canvas, marquee và upload; cửa hàng, checkout, commission được mở rộng từ FE. Wi-Fi là phần bổ sung theo đặc tả, cần kiểm tra khả năng firmware trước khi triển khai.

Dán prompt nền bên dưới vào Stitch trước. Sau đó gửi từng batch theo thứ tự trong cùng dự án. Khi gửi batch mới, đính kèm/chọn màn hình đã duyệt làm tham chiếu để giữ nhất quán. Prompt viết tiếng Anh, nội dung giao diện tiếng Việt. Không cần MCP để dùng trực tiếp các prompt này.

## 1. Prompt nền — gửi đầu tiên

```text
Design a cohesive, high-fidelity native Android mobile app called Mirage for an ESP32 Bluetooth LED matrix and car ambient lighting ecosystem. The app combines device control, a personal pixel-character library, a shop for physical LED hardware and digital characters, and account/order management.

This is a redesign of an existing product. Preserve its distinctive sage-green, black-outline, pixel-art identity while improving mobile information architecture and readability. Use supplied web screenshots as visual references if attached. Do not assume you can inspect the Figma URL or local source files. Do not copy a generic dark neon gaming dashboard.

VISUAL SYSTEM
- Primary sage #89C89B; pressed #6EB782; pale sage #A3D7B2; canvas #FDFDFD; surface #FFFFFF; ink #111111; secondary text #555555; accessible dark-green action color #165D3A.
- Main buttons: sage background, black text, 2px ink outline, restrained 3px solid offset shadow. Secondary buttons: white with ink outline. Cards: 1px soft green border, 12px radius; reserve 2px ink outlines for selected controls, key device cards and primary actions. Avoid outlining every element heavily.
- Use Inter with Vietnamese support, headings 24–28px, section titles 18–20px, body 14–16px, labels 12–14px. Pixel typography is decorative only. Use generous spacing on a 4/8px scale, 16–20px page gutters, 48dp touch targets, clear contrast and text labels alongside status colors.
- Pixel art belongs in LED previews, avatars, thumbnail artwork and subtle corner ornaments. Keep forms, prices and device data clean. Dark surfaces are reserved for accurate LED previews. Use simple consistent 24px outline icons.
- Design at 390x844 logical portrait size, respecting Android status/navigation bars, safe areas and keyboard insets. Layouts must also work at 360px width with larger text and scrolling. Avoid tiny desktop tables, sidebars, carousels as primary navigation and excessive gradients.

INFORMATION ARCHITECTURE
Four fixed root destinations with icon and Vietnamese label: Thiết bị, Thư viện, Cửa hàng, Tài khoản. Active destination uses a sage pill and dark icon. Keep bottom navigation on root screens; detail and focused editor screens use a back app bar. Cart is an app-bar action with quantity badge in shop screens. Commission and order history live under Tài khoản. Device home is the default destination. Connection status remains visible inside device-dependent screens. Do not add social feeds, likes, followers, public chat or invented membership benefits.

PRODUCT RULES
- Distinguish physical LED hardware from digital Character licenses. Digital-only checkout has no shipping address or shipping fee. Mixed baskets separate hardware delivery from digital fulfillment.
- A preview is not proof of ownership. Character actions depend on Chưa sở hữu / Đã sở hữu / Đã tải / Đang gửi / Đang hiển thị. Ownership unlocks only after verified payment. Sending additionally requires a connected compatible device.
- Show device connection independently from internet connectivity. Local BLE control may remain available when internet is unavailable. Never present a queued or failed BLE command as successfully applied.
- Supported control examples: power, brightness 0–255 displayed as 0–100%, speed 1–100, static color, breathing, rainbow, chase, music, character/custom and marquee. Show only controls relevant to the selected mode. Frame size must follow connected-device capability, not the purchased hardware catalog alone.
- Do not invent battery percentage for an externally powered ESP32 device. Use explicit connected/disconnected status and last sync; signal information only when available.
- All user-visible copy is natural Vietnamese with accents. Keep Mirage, Character, Bluetooth and Wi-Fi where appropriate. Retain the current catalog's USD currency in this concept; do not invent VND conversion, discounts or backend capabilities.

Create reusable components first: app bars, root navigation, device status chip, device card, LED preview, character card, product card, segmented control, sliders, buttons, fields, bottom sheets, inline errors, progress bars, empty states and skeletons. Use the same components across every subsequent screen. Generate actual mobile screen designs, not just an explanation or a collage. Name every frame using the IDs supplied in each batch. The full project will be generated in batches; generate only the current requested batch.
```

## 2. Batch A — thiết bị và điều khiển (ưu tiên duyệt trước)

```text
Using the established Mirage design system, create these 6 screens. Make A01 the visual anchor for all later batches.

A01_DeviceHome: Title “Thiết bị của tôi”, compact Mirage wordmark, add-device action. Hero device card “CarLED-Ambient”, labeled “Đã kết nối”, dark LED matrix preview showing a small pixel cat, clear power toggle and “Điều khiển” action. Below: shortcut tiles “Vẽ pixel”, “Chạy chữ”, “Tải nội dung”; recently used owned Characters. No promotional banner above essential controls. Include a second disconnected device card as a compact example; device switching must be explicit.

A02_AddDevice: Back app bar “Thêm thiết bị”; short Bluetooth explanation, scan action and animated scan/progress region. Nearby-device list with device name, signal label when available, and “Kết nối”. Show scanning and no-results variants. Explain that the scan stops after about 10 seconds and offer “Quét lại”. Separate Bluetooth off, missing Nearby Devices permission and connection failure states. Use an app rationale before the real Android system permission dialog; never imitate an OS permission dialog as an app card.

A03_Control: “Điều khiển LED”, selected device and connection chip. Large dark live preview, accessible power switch, brightness slider showing 70%, mode tiles labeled “Màu tĩnh”, “Nhịp thở”, “Cầu vồng”, “Đuổi màu”, “Theo nhạc”, “Character”, “Chạy chữ”. Show current mode clearly. A color selector with preset swatches and an advanced color bottom sheet. Speed appears only for applicable animated modes; mic sensitivity appears for music. Changes apply live; use subtle “Đang đồng bộ…” then device-confirmed state, not a Save button that contradicts live control.

A04_DeviceDetails: Device name, connection, supported matrix resolution, rename action and disconnect confirmation. Do not invent telemetry values. Put optional “Cấu hình Wi-Fi” under advanced settings and label it capability-dependent. Wi-Fi sheet contains SSID, masked password with reveal, connection progress and retry; never expose the saved password. Visually distinguish BLE connection from Wi-Fi/internet state.

A05_DeviceHomeEmpty: Calm pixel illustration, “Chưa có thiết bị”, helpful short copy, primary “Thêm thiết bị”, secondary “Khám phá cửa hàng”. Preserve root navigation.

A06_ControlDisconnected: Reuse A03 layout with disabled device-dependent controls, banner “Đã mất kết nối”, “Kết nối lại”, and last-known preview labeled “Trạng thái lần cuối”. Keep navigation available. Include reconnecting and failed reconnect variants. Do not imply unsent commands were applied.

Show realistic Vietnamese content and consistent touch targets. Connect the intended flow A05 → A02 → A01 → A03 → A04, with A06 as the connection failure branch. If interactive linking is unavailable, annotate the intended transitions outside the phone frames.
```

## 3. Batch B — Character và công cụ sáng tạo

```text
Continue the same Mirage project and reuse the approved A01/A03 components. Create 6 mobile screens:

B01_Library: Root “Thư viện”; segments “Đã sở hữu” and “Trên thiết bị”, search, type filter. Two-column cards with meaningful pixel art, names, ownership/download state. Use Mirage Cat Hero, Space Robot, Neon Ninja, Cherry Bloom and Mini Dragon. Owned-library cards must not show Buy as their main action. Include an empty-library variant with “Khám phá Character”.

B02_CharacterDetail: Large dark animated pixel preview, title “Mirage Cat Hero”, type “Hoạt ảnh lặp”, supported resolution metadata when known. Show explicit device compatibility. Main action “Gửi lên thiết bị” for an owned compatible Character, secondary “Xem trước”. Include variants for not downloaded, disconnected device and incompatible matrix; clearly explain and disable unavailable actions. Unowned content uses “Xem trong cửa hàng”. Previewing is possible without falsely marking a Character as owned.

B03_PixelCanvas: Focused “Vẽ pixel” editor with back and unsaved-changes confirmation. Zoomable 64x32 example matrix identified as the selected device's reported size, not a universal fixed size. Large drawing area, pencil/eraser/fill, undo/redo, color palette, zoom and “Gửi lên LED”. Keep tools reachable without making the grid too small. Distinguish local draft from confirmed device output. Reuse dark preview and sage action styles.

B04_Marquee: “Chạy chữ”; live LED preview, Vietnamese text input, color presets, direction options and speed slider; primary “Hiển thị trên LED”. Include a helper state explaining actual firmware character support must be checked; do not promise Vietnamese glyph support merely because app labels are Vietnamese. Keyboard must not cover the action. Show validation before sending unsupported input.

B05_Upload: “Tải nội dung”; file picker, selected file name/size, preview and supported-device format guidance. Do not invent exact limits or accepted extensions: show them as device-provided guidance. Explain crop/fit without stretching pixel art; “Xem trước” then “Gửi lên thiết bị”. Include preparation, transfer progress, interrupted transfer with retry, and confirmed success variants. Never mark upload complete solely when local preparation finishes.

B06_TransferSheet: Reusable transfer bottom sheet on Character detail: destination name, preview thumbnail, progress 42%, explanatory status and final device-confirmed “Đã gửi thành công”. Include failed transfer with “Thử lại”. Only offer Cancel if cancellation is supported; otherwise keep accurate progress and safe navigation guidance. Distinguish downloading from cloud and sending over BLE.

Intended flow: B01 → B02 → B06; A01 shortcuts open B03/B04/B05. Add a compact state strip outside frames showing loading, empty, disconnected and transfer failure components.
```

## 4. Batch C — cửa hàng và thanh toán

```text
Continue the approved Mirage style. Create 7 screens:

C01_Store: Root “Cửa hàng”, search and cart badge. Segments “Bảng LED” and “Character”. Two-column product grid. Sample current catalog: 32 × 32 Matrix Panel $20.00; 64 × 64 Matrix Panel $120.00; Mirage Cat Hero $3.50; Space Robot $4.00; Neon Ninja $4.50. Label “Thiết bị vật lý” versus “Nội dung số”. Use contained physical-product renders and crisp pixel previews, with generous spacing and no invented sale pricing.

C02_HardwareDetail: Image gallery, 32 × 32 Matrix Panel, $20.00, short description, verified-spec area, quantity selector, delivery information and sticky “Thêm vào giỏ”. Distinguish this purchased panel resolution from the currently connected device resolution. Avoid invented warranties and stock counts.

C03_CharacterShopDetail: Mirage Cat Hero, $3.50, large preview, looping-animation label, supported-resolution area and ownership status. Unowned variant “Thêm vào giỏ”; owned variant “Mở trong thư viện”. Clearly label digital fulfillment and do not promise free shipping for a digital item.

C04_Cart: Separate hardware and digital item groups; quantity controls only where sensible, digital license cannot be added twice. Subtotal and “Thanh toán”. Include empty-cart state. Do not calculate shipping until enough delivery information is available.

C05_Checkout: Clear sections for contact, delivery address when hardware exists, order summary and payment choice from backend-supported options. Use neutral “Phương thức thanh toán” without inventing a provider. Show both mixed-cart and digital-only variants; the digital-only version omits shipping fields/fee. Fixed bottom total and “Tiếp tục thanh toán”, with keyboard-safe layout. Prices remain USD in this design concept.

C06_PaymentResult: Distinct “Đang xác nhận thanh toán”, verified success, failed and cancelled variants. Only verified success says “Thanh toán thành công” and exposes owned digital content through “Mở thư viện”. Pending state offers “Kiểm tra trạng thái”; failed/cancelled states let users return to order safely. Never suggest a retry creates duplicate orders or grants ownership.

C07_OrderDetail: Order ID, status timeline, item groups, total and payment status. Physical item group has delivery tracking when available; digital item group has fulfillment state and “Mở trong thư viện” only after entitlement is confirmed. Do not show a delivery truck timeline for digital-only orders. Add a support entry point without fabricated tracking data.

Intended flow C01 → C02/C03 → C04 → C05 → C06 → C07. Keep purchase flow focused with back app bars, preserve the same button hierarchy, and show field validation, loading and payment pending states.
```

## 5. Batch D — tài khoản và thiết kế theo yêu cầu

```text
Continue the Mirage project. Create 6 screens:

D01_Login: Mirage branding, small pixel illustration, email/password with reveal, “Đăng nhập”, link “Tạo tài khoản”. Add field errors, loading and server error variants without losing entered email. Do not invent Google login or biometric authentication.

D02_Register: Full name, email, password/confirmation, concise validation and “Tạo tài khoản”. Keep the form keyboard-safe. Avoid unsupported verification promises.

D03_Account: Root “Tài khoản”; avatar, customer name, grouped links for profile, orders, addresses, owned Characters, design requests, security and settings. Show a guest variant with sign-in entry point. Keep order history a list destination; do not invent spend statistics or membership tiers.

D04_OrderList: Search and status filters, compact order cards with identifier/date, physical/digital label, total and status. Tap opens C07. Include empty and error-with-retry states.

D05_AddressesAndProfile: Editable customer profile and an address-list sibling frame with default-address badge, add/edit bottom sheet and delete confirmation. Stacked mobile fields, clear required/optional labels, save state and actionable validation. Address selection returns to checkout. Do not put dense desktop tables on mobile.

D06_Commission: “Thiết kế theo yêu cầu”; request list and a focused create-request sibling frame. Fields: name (3–100 chars), description (10–3000 chars), matrix size 16×16 / 32×32 / 64×64, animation type “Ảnh tĩnh”, “Hoạt ảnh lặp”, “Sprite”. Statuses: “Chờ duyệt”, “Đang thiết kế”, “Chờ phản hồi”, “Hoàn tất”, “Đã hủy”. Details show brief and status history if available. Allow cancellation only while awaiting review, with confirmation. Do not invent an artist marketplace, instant quotation or real-time chat. Treat backend integration as future implementation; keep prototype annotations outside consumer screens.

Use the same app bars, cards, buttons and input styles. Draw a compact external navigation map covering all batches and mark which screens require login, internet and a connected device separately.
```

## 6. Prompt tinh chỉnh sau mỗi batch

```text
Audit the generated Mirage screens against approved A01 and A03. Preserve layout intent and improve consistency: exact sage palette, white surfaces, restrained black outlines, Vietnamese diacritics, consistent typography, 48dp targets and correct Android insets. Remove clipped text, tiny labels, excessive shadows, fabricated telemetry and inconsistent icons. Verify selected root navigation, sticky actions above system bars, keyboard visibility, meaningful empty/loading/error states and correct physical-versus-digital fulfillment. Keep important actions reachable on a 360px-wide phone. Keep pixel artwork crisp without making body text pixelated. Change only screens with an identified problem and retain approved components.
```

## Tiêu chí duyệt

- Điều khiển LED truy cập ngay từ tab Thiết bị; các công cụ vẽ/chạy chữ/upload không nằm trong cửa hàng.
- Quyền sở hữu, tải từ cloud, truyền BLE và xác nhận hiển thị là những trạng thái riêng.
- Không có mạng không đồng nghĩa mất BLE; mất BLE không khóa điều hướng/cửa hàng.
- Checkout phân biệt phần cứng và nội dung số; chưa xác nhận thanh toán chưa cấp Character.
- Nhận diện Mirage nhất quán, đọc được tiếng Việt, thao tác được ở 360px và khi mở bàn phím.
- Đối chiếu lại Figma bằng screenshot/frame khi truy cập được; không mặc định Stitch đọc được link Figma.
