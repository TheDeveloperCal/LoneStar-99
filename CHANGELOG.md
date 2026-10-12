# Changelog

## 0.1.0 — 2026-10-11

- Initial LoneStar 99 Android release candidate with the Libre99-based TI-99/4A emulator and reproducibly built clean-room console firmware.
- Added The Texas Trail, Titris, and Sokoban; local imports for CTG and raw BIN cartridges and DSK disk images; modified-disk export; and full-machine quick save/restore.
- Added portrait and landscape keyboard layouts, physical HID keyboard mapping, persistent appearance/input/audio settings, and safe reset confirmation.
- Added the TI PYTHON Programming Lab, offline-import PDF reader, run/help/history/about pages, license notices, and Liberapay link.
- Added native cartridge/disk eject support and regression coverage, Android instrumentation smoke tests, and GitHub Actions build/test/lint workflow.
- The combined application remains mixed-license. It is not represented as wholly GPL-3.0-only. See `LICENSE-SCOPE.md` and `THIRD_PARTY_NOTICES.md`.

### Known limitations

- Authentic TI BASIC firmware import and BASIC-source injection are not supported. The Lab runs the separate clean-room TI PYTHON language.
- External physical Bluetooth/USB keyboard behavior requires verification with a real device.
- TI manual redistribution rights were not established; the app does not bundle an original manual.
- Release-signed APK installation over the existing debug app was not run because the signature mismatch would require uninstalling the debug app and clearing its local data. Instrumentation smoke tests passed on the connected phone.
