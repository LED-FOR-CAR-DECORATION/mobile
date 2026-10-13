# Run-App.ps1 - Script tự động build, cài đặt và khởi chạy app CarLed trên điện thoại
param (
    [switch]$SkipBuild = $false,
    [switch]$Logs = $false
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "🚀 CAR LED MOBILE - AUTOMATION RUNNER   " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. Kiểm tra lệnh adb
$adbCmd = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adbCmd) {
    Write-Host "❌ Không tìm thấy công cụ 'adb' trong PATH." -ForegroundColor Red
    Write-Host "Vui lòng kiểm tra Android SDK platform-tools." -ForegroundColor Yellow
    exit 1
}

# 2. Kiểm tra thiết bị kết nối
Write-Host "`n📱 [1/4] Đang kiểm tra thiết bị Android kết nối qua USB..." -ForegroundColor Yellow
$adbOutput = @(adb devices)
$deviceLines = @($adbOutput | Where-Object { $_ -match '\b(device|unauthorized|offline)\b' -and $_ -notmatch 'List of' })

if ($deviceLines.Count -eq 0) {
    Write-Host "❌ Chưa tìm thấy điện thoại nào kết nối!" -ForegroundColor Red
    Write-Host "👉 Hãy đảm bảo:" -ForegroundColor White
    Write-Host "   1. Đã cắm cáp USB vào máy tính." -ForegroundColor Gray
    Write-Host "   2. Đã bật 'Gỡ lỗi USB' (USB Debugging) trong Cài đặt nhà phát triển." -ForegroundColor Gray
    Write-Host "   3. Chọn chế độ kết nối USB là 'Truyền tệp' (File Transfer)." -ForegroundColor Gray
    exit 1
}

$firstLine = "$($deviceLines[0])".Trim()
$firstDevice = $firstLine -split '\s+'
$deviceId = $firstDevice[0]
$deviceStatus = $firstDevice[1]

if ($deviceStatus -eq "unauthorized") {
    Write-Host "⚠️ Thiết bị ($deviceId) chưa được cấp quyền (unauthorized)!" -ForegroundColor Yellow
    Write-Host "👉 Hãy mở khóa màn hình điện thoại và bấm 'Cho phép gỡ lỗi USB' (Allow USB Debugging)." -ForegroundColor White
    exit 1
}

if ($deviceStatus -ne "device") {
    Write-Host "⚠️ Thiết bị ($deviceId) đang ở trạng thái: $deviceStatus" -ForegroundColor Yellow
    exit 1
}

$deviceModel = (adb -s $deviceId shell getprop ro.product.model).Trim()
Write-Host "✅ Đã tìm thấy thiết bị: $deviceModel (Serial: $deviceId)" -ForegroundColor Green

# 3. Biên dịch ứng dụng (Gradle)
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
if ($SkipBuild -and (Test-Path $apkPath)) {
    Write-Host "`n⏩ [2/4] Bỏ qua bước build (-SkipBuild)." -ForegroundColor Yellow
} else {
    Write-Host "`n🔨 [2/4] Đang biên dịch ứng dụng (assembleDebug)..." -ForegroundColor Yellow
    & .\gradlew.bat assembleDebug
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Quá trình biên dịch thất bại!" -ForegroundColor Red
        exit $LASTEXITCODE
    }
    Write-Host "✅ Biên dịch thành công!" -ForegroundColor Green
}

if (-not (Test-Path $apkPath)) {
    Write-Host "❌ Không tìm thấy file APK tại: $apkPath" -ForegroundColor Red
    exit 1
}

# 4. Cài đặt APK vào máy
Write-Host "`n📦 [3/4] Đang cài đặt APK vào $deviceModel..." -ForegroundColor Yellow
$installOutput = adb -s $deviceId install -r $apkPath
$installText = $installOutput -join " "
if ($LASTEXITCODE -ne 0 -or ($installText -notmatch "Success")) {
    Write-Host "❌ Cài đặt thất bại: $installText" -ForegroundColor Red
    Write-Host "Lưu ý: Nếu điện thoại hiện thông báo xác nhận cài qua USB, hãy chọn Đồng ý/Cho phép." -ForegroundColor Yellow
    exit 1
}
Write-Host "✅ Cài đặt APK thành công!" -ForegroundColor Green

# 5. Khởi chạy ứng dụng
Write-Host "`n🚀 [4/4] Đang mở ứng dụng trên điện thoại..." -ForegroundColor Yellow
$appPackage = "com.carled.app"
$mainActivity = "com.carled.app.MainActivity"

adb -s $deviceId shell am start -n "$appPackage/$mainActivity" | Out-Null
Start-Sleep -Seconds 1

$pidProcess = (adb -s $deviceId shell pidof $appPackage).Trim()
if ($pidProcess) {
    Write-Host "🎉 Ứng dụng đã chạy thành công! (PID: $pidProcess)" -ForegroundColor Green
} else {
    Write-Host "⚠️ Đã gửi lệnh mở app, vui lòng kiểm tra màn hình điện thoại." -ForegroundColor Yellow
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Hoàn tất! Bạn có thể xem trên màn hình điện thoại." -ForegroundColor Green

if ($Logs) {
    Write-Host "`n📋 Đang stream logcat của $appPackage (Ctrl+C để thoát)..." -ForegroundColor Cyan
    adb -s $deviceId logcat --pid=$pidProcess
}
