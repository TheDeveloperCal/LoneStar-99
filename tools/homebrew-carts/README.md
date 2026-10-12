# LoneStar 99 homebrew cartridges

Build the three original 8 KiB TI-99/4A cartridges from assembly and the shared runtime/font pipeline:

```sh
./tools/build-homebrew-carts.sh
cargo run --locked --offline --manifest-path tools/homebrew-carts/Cargo.toml --bin build_carts -- --check
cargo test --locked --offline --manifest-path tools/homebrew-carts/Cargo.toml
cargo test --release --locked --offline --manifest-path tools/homebrew-carts/Cargo.toml
cargo run --locked --offline --manifest-path tools/homebrew-carts/Cargo.toml --bin gameplay_evidence -- --out docs/homebrew-gameplay/2026-10-11
```

The builder combines each game source in `games/` with `games/common/runtime.asm`. The font table is reused from the existing Titris cartridge and remains under the upstream Modified MIT + Commons Clause license; the generator inserts it without changing its license. Game rules and assembly are original DeveloperCal code. Generated `.ctg` assets are committed so Android builds work directly, and the command above reproduces them from source.

The gameplay tests mount each generated CTG in the same Rust `Machine` core used by the Android JNI adapter, boot the committed console ROM/GROM and clean-room firmware menu, send keys through `TiKey`/the real keyboard matrix, and assert RAM state plus rendered VRAM/framebuffer changes. They also boot the shipped Titris and Sokoban cartridges and verify actual joystick-matrix movement (including a Sokoban push). The evidence command writes raw 256×192 framebuffer PNGs, JSON, and Markdown to a dated folder in `docs/homebrew-gameplay/`; these are emulator screen captures, not Android UI screenshots.

| Cartridge | Controls | Objective |
|---|---|---|
| Starlight Patrol | 1 left, 2 fire, 3 right, 4 advance threat | Score hits, dodge the asteroid lane, survive three lives |
| Solar Salvage 99 | 1 left, 2 up, 3 right, 4 down | Navigate the maze, avoid walls, collect the cell, reach the exit |
| Oilfield Outrun | 1 left, 2 drive, 3 right | Build distance and score in clear lanes; avoid hazards |

Starlight Patrol is a deterministic lane-based shooter: firing earns score and advances the threat; steering into the asteroid lane costs one of three lives. Oilfield Outrun advances the hazard on each drive input and awards score only for a clear lane. Both can reach game over and restart. Solar Salvage 99 can win by collecting the cell and reaching the exit, or lose if blocked moves drain all energy; either result can restart.

These tests prove cartridge load, state changes and the visible TI display through the production emulator core. Android library/load/eject, in-app portrait/landscape layout and touch/UI behavior use the `app/src/androidTest/` suite; connected execution requires a safe target with a compatible signer. No test-only hooks are added to the release app.
