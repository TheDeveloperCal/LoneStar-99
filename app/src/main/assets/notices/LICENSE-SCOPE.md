# License scope

This repository is a mixed-license distribution. The root `LICENSE` contains the unmodified GNU General Public License, version 3. It is not a statement that every file, dependency, or the combined Android application is GPL-3.0-only.

## DeveloperCal contributions under GPL-3.0-only

- Original Android application source: `app/src/main/java/com/developercal/ti99pocket/*.kt`.
- Original Android/native build configuration and JNI adapter: root Gradle Kotlin scripts, `app/build.gradle.kts`, app manifest/resources, and `app/src/main/rust/src/lib.rs`.
- Original Texas Trail game logic: `games/texas-trail/texas-trail.asm`, excluding the attributed bitmap-font table adapted from Titris.
- Original Texas Trail cartridge builder and gameplay test source under `tools/texas-trail/`; their linked build/test dependencies and generated cartridge retain their own terms.

GPL identifiers on original source files apply to DeveloperCal-authored material in those files. They do not relicense upstream code, adapted assets, linked dependencies, firmware, or generated binaries that contain third-party material.

## Components with separate terms

- `app/src/main/rust/libre99-core/`, `tools/libre99-asm/`, the clean-room firmware sources/binaries, and upstream Titris/Sokoban content: Copyright © 2026 Joel Odom, Modified MIT License with Commons Clause. Full text: `app/src/main/rust/LIBRE99_LICENSE.md`.
- The font table in Texas Trail is adapted from Titris and remains under that same upstream license.
- Sokoban includes levels from David W. Skinner's Microban set under its credited free-distribution condition; details are in `original-content/cartridges/sokoban/README.md`.
- Android SDK/NDK, AndroidX, Kotlin/Compose, and Rust crates retain their respective licenses as enumerated in `THIRD_PARTY_NOTICES.md` and their upstream packages.

Because the Android app statically links the separately restricted Libre99 core and bundles content with separate terms, the combined application is not represented as wholly GPL-3.0-only or fully free-software compatible. The GPL license on DeveloperCal components does not grant rights over separately licensed components.
