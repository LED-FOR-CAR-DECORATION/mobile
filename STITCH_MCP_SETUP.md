# Hoàn tất kết nối Stitch MCP

Đã kiểm tra: cấu hình người dùng `C:\Users\phamt\.codex\config.toml` có `[mcp_servers.stitch]`, endpoint `https://stitch.googleapis.com/mcp`, startup timeout 30s và tool timeout 300s. Giữ nguyên mục hiện có. Trong phiên kiểm tra không có biến STITCH_API_KEY ở process hoặc user environment. Chưa xác minh handshake hay gọi được công cụ Stitch; mục cấu hình không đồng nghĩa đã kết nối.

## Cách hoàn tất bằng API key

1. Mở https://stitch.withgoogle.com/settings, đăng nhập, vào API Keys và tạo key. Không cần gửi key vào chat.
2. Mở PowerShell cục bộ, nhập key ở lời nhắc ẩn bằng đoạn dưới. Đoạn này lưu key vào user environment; không in key ra màn hình. Đây là biến môi trường của tài khoản Windows, không phải kho bí mật mã hóa.

```powershell
$stitchSecureKey = Read-Host 'Stitch API key' -AsSecureString
$stitchPlainKey = [System.Net.NetworkCredential]::new('', $stitchSecureKey).Password
if ([string]::IsNullOrWhiteSpace($stitchPlainKey)) { throw 'API key trống' }
[Environment]::SetEnvironmentVariable('STITCH_API_KEY', $stitchPlainKey, 'User')
Remove-Variable stitchPlainKey, stitchSecureKey
```

3. Trong mục `[mcp_servers.stitch]` hiện có, dùng dòng sau để ánh xạ header. Không tạo thêm mục trùng tên. Nếu có header X-Goog-Api-Key dạng tĩnh, thay cơ chế đó bằng ánh xạ biến môi trường; giữ nguyên cấu hình không liên quan.

```toml
[mcp_servers.stitch]
url = "https://stitch.googleapis.com/mcp"
env_http_headers = { "X-Goog-Api-Key" = "STITCH_API_KEY" }
startup_timeout_sec = 30
tool_timeout_sec = 300
enabled = true
```

4. Thoát hoàn toàn rồi mở lại Codex để nhận biến môi trường. Nếu app vẫn không nhận biến mới, đăng xuất/đăng nhập lại Windows. Kiểm tra server Stitch trong phần MCP, rồi thử liệt kê project (read-only). Có thể dùng `codex mcp list` nếu CLI có trên PATH; danh sách cấu hình chỉ xác minh đăng ký server, chưa chứng minh xác thực thành công.

Nếu key đã được lưu bằng cơ chế khác trong cấu hình hiện tại, kiểm tra kết nối trước khi thay đổi. Không cần cài server npm bên thứ ba: Stitch cung cấp endpoint HTTP trực tiếp.

## Nguồn

- Google Stitch setup: https://stitch.withgoogle.com/docs/mcp/setup
- Google SDK, xác thực và endpoint: https://github.com/google-labs-code/stitch-sdk/tree/main/packages/sdk
- OpenAI Docs, cấu hình HTTP MCP và env_http_headers: https://developers.openai.com/codex/mcp

## Dùng ngay khi chưa kết nối

Mở Stitch và dùng bộ `STITCH_MOBILE_PROMPTS.md`: gửi prompt nền, Batch A, duyệt phong cách rồi mới gửi B/C/D. Prompt không phụ thuộc MCP.
