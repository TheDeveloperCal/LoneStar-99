#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cargo run --offline --manifest-path "$root/tools/homebrew-carts/Cargo.toml" --bin build_carts
