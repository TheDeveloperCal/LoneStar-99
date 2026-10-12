# LoneStar 99 v0.2.0 candidate report

Date: 2026-10-11

Branch: `release/v0.2.0`

Status: candidate prepared; stable release is blocked

## Candidate contents

The three original TI-99/4A cartridges are built reproducibly and bundled in the ordinary `com.developercal.ti99pocket` release APK as 8,285-byte CTG assets. The normal Library registers Starlight Patrol, Solar Salvage 99, and Oilfield Outrun alongside Texas Trail, Titris, and Sokoban. Source, build tooling, gameplay evidence, controls, and licensing notices are included in the repository. The release APK contains no instrumentation test classes.

Host-side emulator gameplay was exercised using the shipped Libre99 core, TI firmware, and real TI key input. Starlight Patrol verified movement, shot scoring, collision, game over, and restart. Solar Salvage 99 verified movement, blocked movement, cell pickup, extraction win, and restart. Oilfield Outrun verified steering, distance scoring, hazard collision, game over, and restart. Existing Titris movement and Sokoban movement/box-push regression paths also passed.

## Validation

- **PASS** — Firmware, emulator-core, Texas Trail, new homebrew, and existing Titris/Sokoban Rust test suites in debug and release profiles; strict clippy checks; Android JNI bridge tests. Full host output: `docs/homebrew-gameplay/2026-10-11/host-tests.log`.
- **PASS** — Reproducible cartridge build and committed-asset comparison (`tools/build-homebrew-carts.sh` and builder `--check`). All three APK cartridge assets match the committed generated files byte-for-byte.
- **PASS** — Android unit tests (11 total), Android instrumentation-test Kotlin compilation and APK assembly, debug/release APK builds, and `lintDebug`.
- **PASS** — Final release APK signature verification (APK Signature Scheme v2), package ID `com.developercal.ti99pocket`, version name `0.2.0`, version code `2`; release APK includes updated license and third-party notices.
- **PASS** — `git diff --check`.
- **NOT RUN** — Android instrumentation tests on a device/emulator. Test code compiles, but the available BLU B1660V had a signing conflict during instrumentation install, and no usable local Android system image/AVD was available.
- **BLOCKED** — Clean installation, cold launch, Library portrait/landscape inspection, and gameplay through the installed release APK. Therefore actual Android UI screenshots and installed-release gameplay are not verified. The checked-in gameplay screenshots are authentic emulator-core framebuffer captures from the automated TI gameplay harness, not app UI/device captures.
- **BLOCKED** — Stable GitHub release publication. The installed-release quality gate above is required by the release criteria and has not passed. The historical `v0.1.0` prerelease/tag is preserved.

## Device handling note

The requested disposable package cleanup command targeted only `com.developercal.ti99pocket.showcase`; Android returned `DELETE_FAILED_INTERNAL_ERROR`. A subsequent read-only package enumeration showed neither that package nor `com.developercal.ti99pocket`. No uninstall command targeted the production package and no app-data clear was issued. Because the package listing changed and the device state could not be established safely, no further device mutation or install was attempted. Production package/data presence after that event is unknown and is not claimed as preserved.

## Screenshots

All captures are under `docs/homebrew-gameplay/2026-10-11/` and show rendered TI framebuffer output from the gameplay harness:

- `starlight-patrol-title.png`, `starlight-patrol-shot-scored.png`, `starlight-patrol-collision.png`, `starlight-patrol-game-over.png`, `starlight-patrol-restarted.png`
- `solar-salvage-99-title.png`, `solar-salvage-99-cell-collected.png`, `solar-salvage-99-wall-blocked.png`, `solar-salvage-99-extraction-win.png`, `solar-salvage-99-restarted.png`
- `oilfield-outrun-title.png`, `oilfield-outrun-distance-scored.png`, `oilfield-outrun-hazard-collision.png`, `oilfield-outrun-crash-game-over.png`, `oilfield-outrun-restarted.png`

The README uses title and representative gameplay captures and explicitly describes their source. No Library or Android app screenshots are presented as verified.

## Release artifact

- APK: `app/build/outputs/apk/release/app-release.apk`
- Size: 8,028,703 bytes
- SHA-256: `3ea8336b03e9982bedbc6dcc3afa8d022b69fe1726dfb72361d4237c18c28ccc`
- Signer certificate SHA-256: `f023c9409e7e5d575f883f06fcee9aa293bf6fc202ae0fdeca9774c47a79c7e5`

The APK is signed and its signature verifies. It has not been installed on a clean target, uploaded, or published as a GitHub release. The public `v0.1.0` release remains unchanged.

## Repository state

This report describes the candidate branch at commit `6e5897ce78e2ac4a9a2ac61db3fadbcaae2851fa`. The commit is local on `release/v0.2.0`; push failed because the configured GitHub credential helper references a missing `gh` executable and Git has no interactive credential source in this session. No tag, stable release, or release asset was published.
