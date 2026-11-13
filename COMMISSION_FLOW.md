# Mobile Commission flow

Open **Home → Quick start → Commission** after signing in.

1. **Choose design and price.** Select a 32 × 32 or 64 × 32 canvas. Static Image costs 50,000 VND and 100,000 VND respectively. Animated GIF costs 50,000 VND per frame at either size. The demo accepts 2–8 animation frames.
2. **Design brief.** Write a description and optionally select up to five reference images with Android Photo Picker. Returning to the price screen preserves the brief.
3. **SePay demo checkout.** The selected price is locked into the request. Checkout shows a unique transfer code, a demo account label and the total. Choose Pay later to reopen checkout from Progress tracking.
4. **Chat.** Simulate a successful transfer to mark the request Paid, record one receipt and create its conversation and Inbox notification. Open the conversation from checkout or Inbox. The conversation accepts text and image attachments.

The Commission view model keeps drafts, requests, replies and payment state in memory for the signed-in account and navigation entry. Leaving the process or signing out clears them. The flow is a customer-side demo with no live payments.

## Animation frame limit

The firmware accepts an 8-bit frame count in BLE command 0x10 and buffers RGB565 data in RAM for upload and playback. Its ESP32 build uses a 128 KiB SPIFFS partition. Mobile sends 64 × 32 RGB565 frames, each 4 KiB, even when the design canvas is smaller. The demo cap of 8 frames uses 32 KiB of raw pixel data and leaves space for overhead and other files. The physical maximum depends on available flash and contiguous heap and requires testing on the LED hardware.

## Verification

Run `gradlew.bat :app:assembleDebug`. With an Android emulator attached, run `gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.carled.app.CommissionFlowTest"`.

Manually verify the two static prices, attaching and removing reference images, and the five-image limit.

[Figma reference](https://www.figma.com/design/KMB4v4HtenNMtu8D0gyP13/LEDSCREEN?node-id=3089-1300)
