# Changelog

## 0.2.0-rc.1 — 2026-10-12 (prerelease candidate)

- Added three original offline TI-99/4A cartridges: Starlight Patrol, Solar Salvage 99, and Oilfield Outrun. All are available in the normal Software Library and include their controls on-screen.
- Added source-to-CTG builder and reproducibility check under `tools/homebrew-carts/`; gameplay tests boot the generated cartridges in the same Libre99 machine, firmware and keyboard-matrix path used by the Android app.
- Added Android library and gameplay instrumentation coverage for all three games and regression inputs for Titris and Sokoban. See the dated status in `SHIP-MODE-REPORT.md` for exactly which checks ran and which clean-install/device checks remain blocked.
- The APK version is 0.2.0 (versionCode 2), but this candidate is not a stable release. The published v0.1.0 prerelease and tag remain unchanged.
- Candidate APK SHA-256: `3ea8336b03e9982bedbc6dcc3afa8d022b69fe1726dfb72361d4237c18c28ccc`. The candidate branch and tag are public; the GitHub prerelease and APK asset are pending authentication. Host gameplay, Android unit, lint, and build checks passed; device instrumentation and clean-install gameplay are not verified. See `SHIP-MODE-REPORT.md` for the complete status.
- The app remains mixed-license. New cartridges incorporate the separately licensed Titris-derived font; no upstream license or attribution was changed.

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
