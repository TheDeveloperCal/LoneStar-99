# LoneStar 99 ship report

## Current ship status — 2026-10-11 (authoritative)

This section supersedes earlier contradictory release and test statuses below. Historical sections remain as an audit trail.

### Implementation and test results

- The Programming Lab now runs source on the emulated TI through the clean-room firmware's actual TI PYTHON REPL. It resets into the REPL and enters each line using the emulated keyboard matrix. It is explicitly not Python 3 or authentic TI BASIC. Authentic BASIC firmware import and BASIC-source injection remain unsupported.
- Added native cartridge and disk eject paths, keyed in-session disk write retention, PDF import to app-private storage with size/type validation, pixel-filter selection, reset confirmation, foreground cancellation/key release, and regression/UI coverage. Fixed the Texas Trail cartridge source builder to include its licensed font table; its rebuilt cartridge remains byte-identical to the existing generated asset.
- **PASS** — Libre99 core: 35 tests. Texas Trail: 3 gameplay tests in debug and 3 in release; builder unit test passes in both profiles. Firmware GROM rebuild and verification suite passed (43 unit tests; integration suite passed, 24 exhaustive cases intentionally ignored). Strict `cargo clippy --all-targets -- -D warnings` passed for firmware, core, game, and Android JNI bridge.
- **PASS** — Android unit tests: 11/11 (6 physical keyboard mapping/router, 5 Programming Lab file operations). `:app:lintDebug`, `:app:assembleDebug`, and `:app:assembleRelease` succeeded. The Android Gradle Plugin 8.8.2 warns that it was tested only through SDK 35 while this app compiles against SDK 36; Kotlin also reports two existing deprecated system-bar color setters.
- **PASS** — Connected BLU B1660V, Android 15: all 3 instrumentation smoke tests passed. The tests load/eject the bundled Texas Trail, inspect settings and visit/create a Lab file, cancel reset, and inspect About/history/support content. Earlier real-device game/UI captures remain in `docs/screenshots/`; Texas Trail and Titris gameplay progression were previously verified on this phone.
- **NOT RUN** — Installing the release-signed APK over the device's debug-signed install. Android signing identities differ, and uninstalling the debug app would erase local app data; the existing install and data were preserved.
- **NOT RUN** — Physical Bluetooth/USB HID keyboard, full external SAF cartridge/disk import and export, sound loopback, browser network result, TalkBack, and prolonged soak. No external keyboard was attached. The current instrumentation suite does not cover every settings persistence option or rotation/lifecycle combination.

### Signed artifacts and legal status

- Version `0.1.0` (versionCode 1), application ID `com.developercal.ti99pocket`, min SDK 26, target SDK 36, arm64-v8a.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`; SHA-256 `f8c196df84a34570e701c71ad8a618d8f4d191d5a089e816811a5c10b4408383`.
- Signed release APK: `app/build/outputs/apk/release/app-release.apk`; 8,024,207 bytes; SHA-256 `9abccc9be3e0330f2c92683ec9652859852a95cde813d3957ab957450f3c04a1`. `apksigner verify --verbose --print-certs` passed with APK Signature Scheme v2. Certificate subject is `CN=LoneStar 99 Release, OU=DeveloperCal, O=DeveloperCal, C=US`; certificate SHA-256 `f023c9409e7e5d575f883f06fcee9aa293bf6fc202ae0fdeca9774c47a79c7e5`. Existing keystore permissions are now 0600. Back up the keystore encrypted outside the repository and keep its passwords in a password manager.
- The app remains mixed-license because Libre99 core/firmware and some game/assets retain Modified MIT + Commons Clause terms. No whole-project GPL-only claim is made. The published 0.1.0 is an honest prerelease, not a stable v1.0 claim.

### Repository and publication

- **PUBLISHED** — Public repository: `https://github.com/TheDeveloperCal/LoneStar-99`. Authenticated owner verified as TheDeveloperCal; initial source commit `dfa31cf91052efcf6898a4ca3f522bdc9d14ecd3` is on `main` and tagged `v0.1.0`.
- **PUBLISHED PRERELEASE** — `https://github.com/TheDeveloperCal/LoneStar-99/releases/tag/v0.1.0`. The release contains the verified signed `app-release.apk` and checksum. It is marked prerelease, not stable v1.0, with mixed-license and hardware-test limitations disclosed.
- External report destination requested by the owner: `~/SHIP-MODE-REPORTS/2026-10-11-LoneStar-99/final-qa-report.md`; the complete current report is also kept in `docs/final-qa-report.md` in this project.

---

## Follow-up implementation and device QA — 2026-10-11

This update records the current source and final debug APK after the landscape/portrait repair and the added learning/help screens. It supersedes older statements below that said the Programming Lab and PDF reader were absent.

### Changes completed

- Rebuilt the landscape composition so the emulator and controls sit above a full-width keyboard, with the D-pad grouped on the right. A follow-up BLU screenshot showed the display was still small, so landscape now gives the upper area 60% of the content, removes nonessential bezel/status decoration, and keeps the 4:3 output at least 95% of its available display area. This makes the actual output substantially larger while retaining all five keyboard rows. Portrait uses larger typing keys and a compact D-pad on the right of the lower keyboard rows. Kept the touch input router's multi-source ownership and added two focus-loss/simultaneous-key regression cases.
- Replaced the About placeholder with the project story, version/copyright/unofficial status, How to Run, TI-99/4A history and cited source links, Programming Lab, PDF import, license/notice, manual archive, and Liberapay actions.
- Added a local TI BASIC source notebook: create/edit/save/reopen, rename, copy, delete with confirmation, text import/export, and a beginner PRINT/GOTO lesson. It is explicitly text storage only; it does not interpret or run BASIC.
- Added a user-selected, offline Android `PdfRenderer` view with page navigation, zoom, and last-page position stored per document. Search is not available. No TI manual is bundled because redistribution rights were not established; About offers a community archive link and the reader accepts user-selected PDFs.

### Current verification

- **PASS** — `./tools/build-console-grom.sh`: regenerated the 24,576-byte GROM and passed its firmware unit/integration suites (43 unit tests; integration groups passed, with the documented exhaustive tests ignored).
- **PASS** — `cargo test --offline --manifest-path tools/texas-trail/Cargo.toml`: 3/3 Texas Trail gameplay tests.
- **PASS** — Android `:app:testDebugUnitTest`: 9/9 tests (6 key mapping/router, 3 Programming Lab file copy/rename/collision tests); `:app:assembleDebug` succeeded.
- **PASS** — Android `:app:lintDebug`: 0 errors, 7 warnings (AGP/compileSdk support, existing deprecated system-bar APIs, ABI/resource guidance). Lint also exposed the API 29 requirement for `InputDevice.isExternal`; added an API-level guard and the final lint run is clean of errors.
- **PASS** — Installed the final debug build on BLU B1660V (Android 15/API 35) without clearing app data. Genuine screenshots reviewed for both orientations: `docs/screenshots/lonestar-b1660v-final-main-portrait.png` and `...-landscape.png`. The landscape capture confirms the larger live 4:3 output, all five keyboard rows, and right-side D-pad without clipping. Automatic rotation was restored (accelerometer rotation enabled, user rotation 0).
- **PASS** — Opened the About and Programming Lab views on the BLU and created the starter source file. This verifies the entry and create flow; full file rename/delete/import/export behavior was not device-exercised.
- **PASS** — Selected a self-generated, one-page PDF through Android's document picker; the app rendered it and displayed page count and zoom controls. Capture: `docs/screenshots/lonestar-b1660v-pdf-reader-smoke.png`. This verifies basic rendering, not a broad range of PDF variants or large documents. The temporary PDF was removed from Downloads after the check.
- **PASS (historical on-device check)** — Texas Trail and Titris had already been loaded and advanced on this same phone in the preceding QA pass; these UI-only changes do not alter their emulator path. Physical external Bluetooth/USB keyboard hardware was not connected.
- **NOT RUN** — Android instrumentation/full UI tests, a complete hardware key-by-key/multi-touch lifecycle matrix, external SAF cartridge/disk import/export, sound loopback, and external browser network result. No physical HID keyboard was attached.
- **NOT IMPLEMENTED / BLOCKED** — The Programming Lab cannot execute TI BASIC. The current emulator exposes no authentic BASIC firmware import or source-injection path, and the bundled clean-room firmware is not TI BASIC. The lab discloses this directly. PDF text search is also unavailable through Android's built-in renderer.

### Build, licensing, and publication

- Final debug APK: `app/build/outputs/apk/debug/app-debug.apk`, 13,200,577 bytes, SHA-256 `f7ceae9605773923a7e2081dafd2ecb032dd1fc994f459ca1425f7ac2dc0385e`. APK signature verification passed (v2); it is installed on the BLU.
- Release build: `app/build/outputs/apk/release/app-release-unsigned.apk`, 7,983,083 bytes, SHA-256 `709d86581d5afe854a5aedff97d4c49c35d8bf86d730dfa1feef8622e9cde71d`. **BLOCKED for release use**: unsigned, no release signing configuration/key in the workspace. `apksigner` reports “DOES NOT VERIFY.” No signing credentials were created or stored in the repository.
- Libre99 Modified MIT + Commons Clause attribution and notices remain intact. The integrated project remains mixed-license; it is not described as wholly GPL-3.0-only or commercially distributable.
- **GitHub publication BLOCKED** — this workspace has no usable Git repository metadata (`git status` reports “not a git repository”), no remote, and `gh` is not installed. I did not initialize/rewrite Git history or claim a push/release. Publishing requires a valid Git checkout/repository and authenticated GitHub access.

---

## Final source-build, input, and device QA update — 2026-10-11

This section supersedes the earlier branding status in this report. Sections below preserve earlier QA and licensing history; where a status conflicts, this final update is authoritative.

### Branding and license

- The visible startup output is produced by the console GROM startup routine in `app/src/main/rust/firmware-source/console-grom.gpl`; the menu and system-information pages are rendered by that firmware source. A reproducible offline build toolchain is now vendored under `tools/firmware-toolchain/`, pinned to upstream Libre99 revision recorded in `UPSTREAM_REVISION`. `./tools/build-console-grom.sh` regenerates `app/src/main/rust/assets/console-grom.bin` (24,576 bytes) and runs the firmware validation suite.
- The rebuilt boot screen presents LS-99 and LoneStar 99 by DeveloperCal and describes the hardware as an unofficial TI-99/4A emulator. It removes the Libre99/Joel Odom promotional banner from primary firmware output while retaining upstream source copyright and license terms. The emulator core, firmware, and assembler are still upstream-origin code and are not DeveloperCal-authored wholesale or relicensed. Full notices remain in the in-app Licenses & Notices view and `THIRD_PARTY_NOTICES.md`.
- Firmware screen code was adjusted without changes to CPU behavior, memory layout, or hardware compatibility. The system-information row overlap observed on-device was corrected in source, rebuilt, and is awaiting final physical-screen capture below.

### Input and appearance

- Added a distinct directional pad: below the typing keyboard in portrait and to its right in landscape. The six settings colors, four console skins, three theme modes, and existing audio/display options are retained; keyboard size and display scale are persistent settings.
- Settings → Input now exposes physical-keyboard acceptance, on-screen-keyboard visibility while a keyboard is connected, real connection state, and Bluetooth/USB HID setup guidance. The input mapper has four Android unit tests. No physical Bluetooth/USB HID keyboard was connected, so hardware key input is **NOT RUN** on the device; the UI accurately reported `Not connected`.

### Verification

- **PASS** — `./tools/build-console-grom.sh`; 43 unit tests and firmware integration suites passed, including boot/title, system-information, keyboard scan, machine compatibility, and a gate that the committed GROM matches a fresh source build. One exhaustive coverage sweep and one all-cartridges sweep are intentionally ignored by default.
- **PASS** — Libre99 core: 34 tests. Texas Trail gameplay: 3 tests.
- **PASS** — Android `testDebugUnitTest`, `assembleDebug`, and `assembleRelease`; see final build artifacts and signatures below. Compile SDK 36 warning from AGP 8.8.2 remains; compile source reports existing Rust `unused_mut` and Android deprecated system-bar API warnings.
- **PASS** — Connected BLU B1660V, Android 15/API 35: portrait/landscape layout and source-built Texas-shaped LS-99 startup captures; final rebuilt system-information screen reviewed for row spacing. Input settings showed `Not connected`. Texas Trail advanced through firmware selection, title, guide, and active gameplay. Titris reached its title and live board; left/right/down touch D-pad input moved the piece. Captures are listed below.
- **NOT RUN** — End-to-end physical Bluetooth/USB HID keyboard input; no external HID was attached. Automated mapping tests passed, but ADB-injected virtual key events are not evidence of physical keyboard support.
- **NOT RUN** — Release-signing verification: release signing is not configured. Release artifact is unsigned; debug APK is debug-signed.



### Final artifacts and test limits

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`, 13,200,577 bytes, SHA-256 `bbbb00dc6cbc6cf7824f711d1de01ae0d77a1dbef2e9ffb0910acd8e487cc7be`. `apksigner verify --verbose` passed with APK Signature Scheme v2 (Android Debug certificate); installed on the BLU B1660V.
- Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`, 7,950,315 bytes, SHA-256 `a531c3f373339f15ff4d61d2cbbda25e34783d68e68c7cccce159073bf53bf3c`. **BLOCKED for release use** because no release signing key/configuration exists; `apksigner` correctly reports the artifact does not verify.
- Android unit tests: 4 physical-key mapper tests passed. **NOT RUN** — instrumentation/UI tests; no Android instrumentation test source set is present. **NOT RUN** — real external HID key events, because no Bluetooth/USB keyboard was attached. ADB taps/swipes were used only for virtual touch controls, not presented as physical keyboard evidence.
- The tested device was returned to automatic rotation (`accelerometer_rotation=1`, `user_rotation=0`). Latest checked logcat excerpt contained no fatal exception, ANR, or fatal signal.
- Visual QA found and corrected crowding in the system-information GROM page; the last capture displays the chip specification rows with blank rows between them. All six palettes, four skins, and three themes were previously selected on-device and their captures remain available. Some existing settings pages require vertical scrolling; this is expected and was not treated as a layout defect.
- The external delivery folder `the external LoneStar 99 ship-report folder` was updated with this report, README, notices, complete screenshot set, debug APK, and unsigned release APK. Existing prior artifacts were preserved.

Captures: `docs/screenshots/lonestar-b1660v-final-boot.png`, `lonestar-b1660v-final-firmware-menu.png`, `lonestar-b1660v-final-system-info.png`, `lonestar-b1660v-landscape-dpad.png`, and `lonestar-b1660v-external-keyboard-settings.png`; game captures use `lonestar-b1660v-texas-trail-final-{title,guide,gameplay}.png` and `lonestar-b1660v-titris-final-{title,gameplay}.png`. Settings, theme, palette, and skin captures from the full appearance audit are retained in `docs/screenshots/`.

---

## Earlier branding audit notes — superseded by final source-build update below (2026-10-11)

- Android primary branding remains LoneStar 99 / LS-99 by DeveloperCal. The transient initial status and About/Settings copy no longer describe the app as Libre99; component details and attribution remain in the license viewer and `THIRD_PARTY_NOTICES.md`.
- The actual boot presentation comes from GROM firmware, not Android resources: `app/src/main/rust/firmware-source/console-grom.gpl`, startup routine `START`, writes the logo/title/copyright cells and draws strings at `TITLE1`, `TITLE2`, `LIBRE`, `PRESS`, `COPYT`, and `COPYURL`. The shipped generated image is `app/src/main/rust/assets/console-grom.bin` (24,576 bytes). The ROM/GROM binary is not string-searchable because it contains encoded firmware data.
- The embedded `LIBRE99_LICENSE.md` is Modified MIT plus Commons Clause v1.0, Copyright (c) 2026 Joel Odom. It allows modification while requiring inclusion of the copyright and permission notice in copies/substantial portions and restricting sale. Both app-visible notices and the full text are retained. The source attribution in the GROM header remains intact.
- No `libre99-gpl` assembler source/binary or reproducible GROM build task exists in this checkout. The firmware source says it is assembled with that toolchain; that toolchain could not be fetched here because crates.io DNS/network access failed. The request also expressly forbids silently patching a generated binary. Consequently the GROM source and binary were not changed, and no claim of LS-99 authorship was added to firmware. The boot branding replacement and its BLU device verification remain **BLOCKED** pending the compatible upstream GPL assembler/build pipeline (or a clean-room replacement assembler that can reproducibly assemble this source and validate behavior).
- The debug APK rebuilt successfully and was reinstalled on the connected BLU B1660V (Android 15). The launch capture confirms LS-99 app branding around the still-upstream firmware screen; the Settings screen also reflects the appearance system. Captures: `docs/screenshots/lonestar-b1660v-branding-audit.png` and `docs/screenshots/lonestar-b1660v-settings-audit.png`. The branded firmware screen itself could not be produced or verified due to the build-tool blocker above. This app-only UI copy change did not modify the emulator core or game content; the previously recorded Texas Trail and Titris on-device checks remain applicable, but were not rerun as part of the branding audit.
- This audit build: `app/build/outputs/apk/debug/app-debug.apk` (13,200,577 bytes), SHA-256 `7b408eeb20ea4ecb06de5669d43f01c0d1a489ae1d7d1c80b49933e591e1649b`.

Date: 2026-10-11
Workspace: `the project root`

## Build and device

- App name: LoneStar 99; application ID remains `com.developercal.ti99pocket`; version `0.1.0` (version code 1).
- Target device: BLU B1660V, Android 15, API 35. The app hides system bars using immersive mode and Android's transient system-bar behavior. Device navigation mode was gesture navigation (`navigation_mode=0`); no system navigation setting was changed.
- Build: `gradle --no-daemon :app:assembleDebug` succeeded. APK: `app/build/outputs/apk/debug/app-debug.apk` (13,199,589 bytes).
- APK SHA-256: `2733ed68f653665940dd210684fa8c5cf4345b2e1cbe932757b3f27e4f045fff`.
- `apksigner verify --verbose` succeeded, using APK Signature Scheme v2. This is a debug-signed APK; release signing is not configured.

## What was delivered

- Rebranded, immersive TI-99/4A interface with touch keyboard, controls, quick save/restore, pause/run/reset, sound toggle, and disk export.
- Libre99 core framebuffer alpha corrected so emulated video is visible on Android.
- Software shelf contains The Texas Trail, Titris, and Sokoban; recently used items are ordered first (up to five). Imports `.ctg`, `.bin`, and `.dsk` through Android's document picker; DSK1 can be exported. RPK and tape formats are unsupported.
- Original TMS9900 cartridge game **The Texas Trail** includes title and guide screens, travel and route choices, resource management, events, win/loss states, and restart.
- The app states it is unofficial and not affiliated with Texas Instruments. It bundles Libre99 clean-room firmware; authentic TI firmware and commercial software are not included. TI BASIC therefore requires firmware supplied by the user.
- Licensing constraints are documented in `LICENSE`, `THIRD_PARTY_NOTICES.md`, and `app/src/main/rust/LIBRE99_LICENSE.md`. Libre99 and its bundled games use Modified MIT + Commons Clause terms; commercial distribution requires permission from the relevant rights holder. App distribution must remain free and without ads, paid features, or paid services under those terms.

## Verification performed

- Libre99 Rust core: 34 tests passed.
- Texas Trail CPU/core integration: 3 gameplay tests passed in debug and 3 in release. The integration exercises boot, guide/start flow, rendered pixels, actions/resource changes, route and victory/restart, and defeat.
- Android debug APK assembled successfully and was installed/launched on the B1660V. The Texas Trail title/guide/gameplay flow and the Titris title screen were observed on-device. No fatal exception, ANR, or fatal signal was found in the checked logcat.
- Android Kotlin unit/UI/instrumentation tests were not run; the project has no Android test sources. Import/export with external user files, rotation, and a full input/timing matrix were not exhaustively exercised.
- Device screenshots are in `docs/screenshots/`; the README embeds the primary captures. The older route screenshot is intentionally omitted because it predates the sprite-table fix and contains visual artifacts.

## Repository and naming notes

- This checkout has no Git repository metadata (`git status` reports “not a git repository”), so no commit history or Git diff could be preserved or reported. Existing workspace files were retained and changes are present in the working tree.
- A web search did not surface an apparent TI emulator/game collision for LoneStar 99 or The Texas Trail. A separate radio station uses the adjacent name “Lonestar 99.5” ([station site](https://lonestar995fm.com/listen-live/)). This is a preliminary search, not trademark clearance or legal advice.

## Remaining release work

- Configure a release signing key and build a release APK before public release.
- Complete file-picker import/export QA with representative files on the target device and test lifecycle/rotation and sound/pause behavior more broadly.
- Confirm licensing and naming for the intended distribution channel; no formal trademark or legal review was performed.

## Licensing audit update — 2026-10-11

- `LICENSE` now contains the complete GNU GPL version 3 text. GPL-3.0-only applies to identified DeveloperCal-authored Android/Kotlin source, JNI adapter, build configuration, and original Texas Trail game logic (excluding its adapted font table). File headers, Cargo package metadata, About text, `LICENSE-SCOPE.md`, and `CONTRIBUTING.md` describe that scope.
- The vendored Libre99 core, clean-room firmware, assembler, Titris and Sokoban remain under Joel Odom's Modified MIT License with Commons Clause. The 8×8 font table embedded in Texas Trail is explicitly excluded from the DeveloperCal GPL grant. The Texas Trail cartridge is mixed-license. No license texts or upstream files were changed to imply otherwise.
- Auditing the retained license and upstream project description found no documented GPL-compatible exception or separate permission. The TI-99/Sim alternative states only “GPL” without identifying a version in its project page, and its documented desktop SDL/C/C++ implementation has no verified Android integration; it was not imported or treated as GPL-3.0-only compatible.
- **Outcome 2:** original DeveloperCal components are GPL-3.0-only where identified, but the integrated Android app remains mixed-license and the goal of a wholly free/open-source GPL-3.0-only app remains unresolved. Do not describe the combined application as GPL-only or publish it under that claim. See `README.md`, `LICENSE-SCOPE.md`, and `THIRD_PARTY_NOTICES.md`.
- This checkout still has no Git metadata, so no Git history/diff exists to preserve or report.

## Implementation and device QA update — 2026-10-11

### Delivered in this pass

- Added density-specific launcher PNGs and Android adaptive/monochrome icon resources from the supplied LS-99 Texas emblem. Added the same transparent image to the emulator header.
- Added a persistent Settings screen with System/Light/Dark theme modes; Lone Star Blue, Classic TI-99, TI Rainbow, Midnight Console, Warm Retro, and Monochrome palettes; and Classic TI-99/4A, Lone Star Blue, Pocket Calculator, and Minimal Console skins.
- Palette/theme/shell surfaces now respond immediately. Pocket Calculator compacts keycaps. Minimal Console enlarges the live display and can hide/show its on-screen keyboard. Changing appearance does not reset the emulated machine.
- Added persistent immersive mode, command-row visibility, key feedback, audio enable, and master-volume settings. Added Android physical keyboard mappings for supported TI keys and hardware modifiers. Settings About & Support includes license notices and the Liberapay browser intent.
- Embedded `LICENSE-SCOPE.md` and `THIRD_PARTY_NOTICES.md` copies for the in-app license viewer. The real emulator framebuffer remains displayed by the existing Android view.

### Status matrix

- **PASS** — Debug APK build; package `com.developercal.ti99pocket`, version 0.1.0/code 1, min SDK 26, target SDK 36, arm64-v8a. APK: `app/build/outputs/apk/debug/app-debug.apk` (13,200,577 bytes), SHA-256 `ee81a5a6d37453dde4d458712b301adb7da9e985da829673e17e49f496913ed7`. `apksigner verify --verbose` confirms a debug v2 signature.
- **PASS** — Release build completed at `app/build/outputs/apk/release/app-release-unsigned.apk` (7,933,763 bytes), SHA-256 `7403b431274cd510fca616460367fc2e344c24109e2f0556de5260962ef55fe2`. It is unsigned and does not pass `apksigner verify`; release signing is not configured.
- **PASS** — Libre99 core tests: 34 passed, 0 failed. Texas Trail gameplay tests: 3 passed, 0 failed.
- **PASS** — Android `assembleDebug`, `lintDebug`, and `testDebugUnitTest` tasks completed. The unit-test task reports `NO-SOURCE`; no Kotlin unit, UI, or instrumentation test sources are configured. Lint reports 0 errors and 7 warnings (dependency freshness, window-size API, ChromeOS ABI, and resource-qualifier guidance).
- **PASS** — Connected device identified as BLU B1660V, Android 15/API 35. Latest debug build installed over the same package without clearing app data and launched. Main emulator screen shows live clean-room firmware output.
- **PASS** — Texas Trail loaded inside the emulator and advanced through title, field guide, journey, route choice, and changing resource states using virtual keys. Titris loaded to its active board. Pause/resume, quick save, reset, and restore returned to a live game board.
- **PASS** — All three theme modes, six palettes, and four skins were selected on device. Theme/palette/skin and audio/immersive/command preferences were read back from app SharedPreferences. Preference state remained after app process restart/update. The Minimal Console keyboard collapsed and reopened.
- **PASS** — Fullscreen preference immediately hid/revealed Android system bars. An unsupported `.xyz` was selected through the Android document picker and the app displayed `Unsupported extension .XYZ. Supported formats: BIN, CTG, DSK.` The temporary probe file was removed afterward.
- **PASS** — Settings license viewer displayed the bundled license scope, third-party notices, and full GPL text. Recent app log scans after launch/game interactions contained no `FATAL EXCEPTION` or `ANR in` entries. The Android audio-focus request was observed in logcat when the emulator opened.
- **PARTIAL** — Audio setting, focus request, mute path, and volume slider are wired and persisted, but sound was not measured with an audio loopback on the phone.
- **NOT RUN** — Valid external SAF cartridge import; disk mount/read/write/export on device; external browser/network result for Liberapay; physical keyboard hardware; Android game-controller input; rotation; long-duration/ANR soak; release install; accessibility/TalkBack session; complete compatibility matrix.
- **NOT IMPLEMENTED** — Programming Lab/BASIC editor and authentic program injection. TI BASIC needs user-provided compatible firmware. Android controller mapping is not implemented.
- **BLOCKED** — A wholly GPL-3.0-only distribution remains incompatible with the integrated Libre99 Modified MIT + Commons Clause core and upstream assets absent written permission or a tested replacement core.

### Evidence and workspace

- Real device captures, including the launcher icon, settings, all palettes/themes/skins, Texas Trail title/guide/gameplay, Titris title/gameplay, and in-app notices, are in `docs/screenshots/` and referenced by `README.md`.
- The requested external report bundle is under `the external LoneStar 99 ship-report folder`, with separately named final debug/release APKs, `final-qa-report.md`, and a `screenshots/` copy. The older report/APK files in that folder were preserved.
- The checkout has no `.git` metadata, so no `git status` or diff can be supplied.
- Android unit and instrumentation test infrastructure still needs to be added before those suites can provide automated coverage of the new preference/UI behavior.
