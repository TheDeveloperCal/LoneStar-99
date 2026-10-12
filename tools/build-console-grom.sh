#!/bin/sh
# Rebuild the bundled 24 KiB console GROM from GPL source and generated tables.
# The offline GPL assembler is vendored at the pinned revision documented beside it.
set -eu
root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
manifest="$root/tools/firmware-toolchain/Cargo.toml"
out="$root/app/src/main/rust/assets/console-grom.bin"
cargo run --offline --manifest-path "$manifest" -p libre99-gpl --bin libre99gpl -- console "$out"
cargo test --offline --manifest-path "$manifest" -p libre99-gpl
