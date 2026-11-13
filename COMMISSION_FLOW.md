# Mobile Commission flow

Open **Home → Quick start → Commission** after signing in. The workspace uses the existing Mirage bottom navigation.

- **Create request** pre-fills the signed-in user and accepts an Image or GIF request, a description, and up to five reference images from Android's photo picker. GIF requests require positive whole numbers for FPS and frame count. The preview uses the Figma demo rate of USD 0.50 per frame. Submitting opens Progress tracking.
- **Progress tracking** lists requests and opens a timeline. Demo requests illustrate in-progress and canceled states. A new local request starts at Request created.
- **Inbox** lists the latest message and notifications for demo requests. Open a conversation to compose text or select a photo. Canceled conversations are read-only.

The Commission view model keeps drafts, requests and replies in memory, scoped to the signed-in account and navigation entry. Leaving the process or signing out clears them. The request IDs, timeline, inbox events, GIF price and example receipt are demo data. The flow does not upload files, contact staff, collect payments or receive server updates.

## Verification

1. Open Commission and confirm that Submit request is disabled until a description is entered.
2. Select GIF and verify the default 8 FPS and 24 frames display a USD 12.00 estimate. Enter zero frames and confirm submission is disabled.
3. Attach and remove a reference image. Confirm the picker button disappears when five distinct images are attached.
4. Submit a valid request. Open the new request and confirm only Request created is marked complete.
5. Open Inbox, select the in-progress conversation and send a text reply. Confirm the inbox preview reflects the reply. Open the canceled conversation and confirm the composer is hidden.

Run `gradlew.bat :app:assembleDebug :app:testDebugUnitTest`. With an Android emulator attached, run `gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.carled.app.CommissionFlowTest"`.

[Figma reference](https://www.figma.com/design/KMB4v4HtenNMtu8D0gyP13/LEDSCREEN?node-id=3089-1300)
