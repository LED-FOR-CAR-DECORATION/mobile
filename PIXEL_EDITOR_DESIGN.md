# Mirage pixel editor — design requirements

Stitch: https://stitch.withgoogle.com/projects/6847000307657413933

Scope: interaction specification implemented by the native Kotlin StudioScreen/StudioViewModel and PixelDocument engine. See README for integration limits and supported font rendering.

## New design

Choose canvas size before creating: 32×32, 64×32, 64×64. Existing drafts open directly in the editor. Device compatibility is checked separately before sending.

## Tools

| Tool | Behavior |
|---|---|
| Pen | Selected color, tap or continuous finger drag; square brush width 1–4 pixels. |
| Eraser | Same gestures and sizes; removes pixels to empty/off state. |
| Color | Choose drawing color; add/edit/delete named palettes and individual swatches. Color frame with hue strip, synchronized RGB (0–255) and HSV (H 0–360°, S/V 0–100%) inputs. Apply/cancel. Palette edits do not recolor existing art. |
| Eyedropper | Sample exact color from tapped canvas pixel, then return to pen. Empty pixels have no color. |
| Undo/redo | Stroke-level history; shape/fill/text/import edits are single committed actions. New action clears redo. |
| Shape | Line, rectangle, ellipse; filled/outlined rectangle and ellipse, current color. Drag preview before commit. |
| Flood fill | Selected color replaces contiguous area within its boundary. Proposed adjacency: 4 neighbors. |
| Mirror | Reflect every placed pixel across horizontal/vertical axis, or both. One undo step per original+mirrored stroke. |
| Grid | Toggle display overlay; grid is excluded from export. |
| Text | English/ASCII bitmap glyphs, starting at 3×5; proposed further sizes 5×7, 7×9. Validate unsupported characters and canvas overflow. Separate text block objects above raster canvas; reopen Text tool to select/edit/delete. Ordinary drawing does not alter glyph objects. Draft retains objects; export composites them. |
| Image import | Android photo picker; downsample into current canvas size. Proposed fit/crop options preserve aspect ratio; preview before import. Confirm raster replacement, preserve text objects, undo entire import. |

Proposed gesture convention: one finger draws, two fingers pan, pinch zooms. Empty pixels remain distinguishable from painted black. Save locally independent of BLE; sending requires connection and compatible resolution.

## New screen set

09 Tạo thiết kế — size choice.

10 Trình vẽ pixel — canvas, primary tools, pen/eraser sizes, undo/redo, save/send.

The main editor now exposes **Nhập ảnh từ thư viện** above the canvas. It launches the Android photo picker directly, works offline, then uses the existing fit/crop preview and raster-replacement confirmation. Image import preserves text objects and can be undone. Home uses three Divoom-style icon function tiles: **DIY pixel / Chạy chữ / Nhập ảnh**.

11 Màu & bảng màu — palette CRUD, swatches, frame/RGB/HSV, eyedropper state.

12 Công cụ & đối xứng — shapes, fill, mirror, grid, expanded toolbar.

13 Văn bản pixel — English input, font sizes, editable text objects and positioning.

14 Nhập ảnh — source, crop/fit, pixel preview, import confirmation and error states.

Design uses existing Mirage sage/white/black-outline identity and Vietnamese interface labels. New requirements supersede the earlier simplified pixel editor description.
