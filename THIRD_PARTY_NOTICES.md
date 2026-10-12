# Third-party notices and license audit

LoneStar 99 is a mixed-license project. See [LICENSE-SCOPE.md](LICENSE-SCOPE.md) for which DeveloperCal-authored files are GPL-3.0-only. The complete application, as currently integrated, is not GPL-3.0-only and is not represented as wholly free-software compatible.

## Libre99 emulator, firmware, games, and assembler

This project vendors `libre99-core` from [joelodom/libre99](https://github.com/joelodom/libre99), upstream commit `6c86f10` (recorded in the original import). The exact core sources are in `app/src/main/rust/libre99-core/`; the retained full license text is `app/src/main/rust/LIBRE99_LICENSE.md`. The core is Copyright © 2026 Joel Odom and uses a Modified MIT License plus Commons Clause v1.0.

The Commons Clause excludes the right to sell the software or a product/service whose value derives entirely or substantially from it for a fee or other consideration, including related hosting or support. The text grants no GPL-compatible exception. No separate GPL-compatible permission was found in the vendored license or the upstream project information inspected for this audit. We did not contact the upstream author, change their license, or infer permission from the separate GPL toolchain terminology used by Libre99.

The clean-room console ROM, console GROM, and disk DSR binaries in `app/src/main/rust/assets/` and their sources in `app/src/main/rust/firmware-source/` came from the same Libre99 import and retain its Modified MIT + Commons Clause terms. The GROM source and generated image include DeveloperCal modifications, including a new product startup presentation and revised system-information spacing. These changes do not change the upstream copyright, authorship, or license. The reproducible GROM build uses the vendored upstream `libre99-gpl` assembler sources in `tools/firmware-toolchain/crates/libre99-gpl/` at the same pinned commit recorded in `tools/firmware-toolchain/UPSTREAM_REVISION`, together with `tools/libre99-asm/`; those tool sources retain their original license and attribution. The firmware header, complete license, and this notice remain included.

**Titris** and **Sokoban** are upstream Libre99 cartridges with source in `original-content/cartridges/`; they retain the same terms and Joel Odom attribution. Sokoban includes Microban levels by David W. Skinner, subject to the attribution and free-distribution condition in `original-content/cartridges/sokoban/README.md`.

The original game logic in `games/texas-trail/texas-trail.asm` is a DeveloperCal contribution under GPL-3.0-only. Its adapted 8×8 font table comes from Titris and is expressly excluded from that grant; it retains the Libre99 terms. The compiled `texas-trail.ctg` consequently contains mixed-licensed material. Its builder and gameplay test source are identified as GPL-3.0-only DeveloperCal contributions, but their Libre99-linked build/test tools and generated output keep their own terms.

No authentic TI firmware, commercial cartridge, or commercial disk image is bundled. The clean-room firmware is Libre99's work; it is not relicensed as GPL by this project.

## Considered alternative emulator

The TI-99/Sim project's own site says its code is released under “the GPL license” and documents a C emulator with CPU, video, sound, keyboard, and disk support. The published statement does not identify a GPL version, and the linked license page could not be retrieved/verified during this audit. Its published build requirements use SDL and its source is a desktop C/C++ project; no Android/NDK compatibility or drop-in interface has been established. We therefore did not copy or integrate it, and cannot treat it as a verified GPL-3.0-only replacement. Reference: [TI-99/Sim project page](https://mrousseau.org/programs/ti99sim/).

## Runtime and build dependencies

- Android Gradle Plugin 8.8.2 — Apache License 2.0.
- Kotlin Android and Compose compiler plugins 2.0.21 — Apache License 2.0.
- AndroidX Activity Compose 1.10.1 and Compose UI/Material 3 via BOM 2025.05.00 — Apache License 2.0.
- AndroidX Test runner 1.6.2, AndroidX Test JUnit extension 1.2.1, and Compose UI test artifacts from BOM 2025.05.00 — Apache License 2.0; used only by the Android instrumentation test source set.
- Rust `jni` 0.21.1 and locked transitive crates — MIT and/or Apache License 2.0; exact versions are in `app/src/main/rust/Cargo.lock`.
- Android SDK and NDK — their respective Android SDK license terms.

Dependency license summaries above describe upstream dependency licenses; they do not change the Libre99 terms or make the integrated app GPL-compatible. No network permission or broad storage permission is requested. Android's Storage Access Framework grants access only to files selected by the user.

## Licensing outcome

This repository applies GPL-3.0-only to identified original DeveloperCal source where permitted. The GPL does not extend to third-party files or dependencies. The current integrated Android distribution still contains the Commons-Clause core and other separately licensed materials. Resolving the user's goal of one genuinely free/open-source GPL-3.0-only application requires documented compatible permission from the relevant rights holder(s), or a separately audited and tested compatible emulator core. Until then, do not label or distribute the combined application as a GPL-only app.
