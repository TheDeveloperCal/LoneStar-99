# LoneStar 99 — Final QA and ship report

**Date:** 2026-10-11 (America/Chicago)

**App:** 0.1.0 (versionCode 1), `com.developercal.ti99pocket`

**Device:** BLU B1660V, Android 15

**Publication:** Public GitHub repository and `v0.1.0` prerelease verified.

## Delivered

- Refined portrait/landscape emulator layouts, touch keyboard/D-pad, themes, palettes, console skins, display scale/filtering, physical-keyboard handling, audio controls, lifecycle cleanup, and safe reset confirmation.
- Added/finished local software management, cartridge/disk eject, offline PDF import/reader, help/about/history/licenses/support, and an actual TI PYTHON workflow in the clean-room firmware REPL. The Lab sends source through the emulated keyboard matrix. It is a small Python-like language, not Python 3 or TI BASIC.
- Fixed the Texas Trail source builder's missing font-table injection. Generated cartridge output matches the existing asset byte-for-byte.
- Added Android instrumentation coverage, regression tests, CI workflow, changelog, updated README and notices. See [`SHIP-MODE-REPORT.md`](../SHIP-MODE-REPORT.md) for historical status and detailed implementation notes.

## Test results

- Libre99 Rust core: **35 passed**.
- Texas Trail: **3 gameplay tests passed** in debug and **3 passed** in release; the cartridge builder test passes in each profile.
- Firmware/GROM: source rebuild and validation passed, including **43 unit tests** and the integration suite; **24 exhaustive cases** remain intentionally ignored.
- Strict Rust Clippy (`-D warnings`): passed for firmware, core, game, and Android JNI bridge.
- Android unit tests: **11/11 passed**.
- Android lint, debug build, and signed release build: passed. Gradle reports the known AGP 8.8.2 / compileSdk 36 compatibility warning and two deprecated system-bar color setter warnings.
- Android instrumentation on BLU B1660V / Android 15: **3/3 passed**: bundled cartridge load/eject, settings and Lab navigation/create/reset cancellation, and About/history/support visibility.
- Physical Bluetooth/USB keyboard hardware, full external SAF imports/exports, sound loopback, browser network result, TalkBack, and prolonged soak: **NOT RUN**.

## Device evidence and compatibility

Genuine device captures for portrait, landscape, settings, firmware, About, PDF, and games are in [`screenshots/`](screenshots/). The current instrumentation run exercised the emulator UI and bundled Texas Trail cartridge. Earlier device QA verified Texas Trail progression and Titris gameplay with touch input. No current screenshot was captured by the instrumentation runner itself.

| Software type | Status |
|---|---|
| Bundled CTG cartridge (Texas Trail) | Loaded and ejected on device; gameplay previously verified on device |
| Bundled Titris and Sokoban cartridges | Bundled; Titris gameplay previously verified on device |
| User CTG and raw BIN | Loader paths implemented; external SAF import not run in this pass |
| DSK disk image | Core mount/eject/export path implemented; external image read/write/export not run on device |
| RPK and cassette/tape | Unsupported |
| Authentic TI BASIC | Requires authentic firmware; import/source injection unsupported |

## Programming Lab, help, manual, and history

- Lab creates, edits, saves, reopens, renames, copies, deletes, imports, and exports `.tipy` source; it executes via the clean-room TI PYTHON REPL in the emulated console. Unit tests cover file operations and device smoke test covers Lab navigation/create. Full real-device source import/export was not exercised.
- About/how-to/history, project story, license notices, and Liberapay action are present. History sources are linked in-app. The app does not bundle an original TI manual because redistribution rights were not established. PDFs can be imported and copied into app-private storage for offline reading; external PDF variants beyond the smoke fixture were not exhaustively checked.
- The combined app remains **mixed-license** under retained upstream terms, including Modified MIT + Commons Clause material. It is not presented as wholly GPL-3.0-only or commercially distributable. This build is a prerelease candidate, not a stable v1.0 claim.

## APKs

- Debug: [`app-debug.apk`](../app/build/outputs/apk/debug/app-debug.apk), SHA-256 `f8c196df84a34570e701c71ad8a618d8f4d191d5a089e816811a5c10b4408383`.
- Signed release: [`app-release.apk`](../app/build/outputs/apk/release/app-release.apk), 8,024,207 bytes, SHA-256 `9abccc9be3e0330f2c92683ec9652859852a95cde813d3957ab957450f3c04a1`. `apksigner verify --verbose --print-certs` passed using APK Signature Scheme v2. Release certificate SHA-256: `f023c9409e7e5d575f883f06fcee9aa293bf6fc202ae0fdeca9774c47a79c7e5`.
- The existing keystore is `~/.android/lonestar99-release.jks`, mode 0600. Keep encrypted backups outside the repository and store signing passwords in a password manager. Never add the keystore or signing properties to Git.

## GitHub and release status

The workspace was initialized as a new repository after preserving a backup and auditing staged files. Authenticated owner `TheDeveloperCal` created the public repository [TheDeveloperCal/LoneStar-99](https://github.com/TheDeveloperCal/LoneStar-99). Initial source commit `dfa31cf91052efcf6898a4ca3f522bdc9d14ecd3` is pushed on `main` and tagged `v0.1.0`. The signed APK is published as the [0.1.0 prerelease](https://github.com/TheDeveloperCal/LoneStar-99/releases/tag/v0.1.0). GitHub reports the release is marked prerelease and contains `app-release.apk` (8,024,207 bytes); the remote asset SHA-256 digest matches the local verified APK: `9abccc9be3e0330f2c92683ec9652859852a95cde813d3957ab957450f3c04a1`.

## Remaining checks

1. Complete physical HID keyboard, external file import/export, sound loopback, browser intent result, and broader orientation/lifecycle checks when practical.
2. Retain the mixed-license and non-TI-BASIC limitations in release notes.
