// Copyright (C) 2026 DeveloperCal
// SPDX-License-Identifier: GPL-3.0-only

fn main() {
    if std::env::args().nth(1).as_deref() == Some("--check") {
        match lonestar_homebrew_carts::verify_committed_assets() {
            Ok(()) => println!("PASS: committed CTGs match freshly assembled source"),
            Err(error) => { eprintln!("cartridge reproducibility check failed: {error}"); std::process::exit(1); }
        }
        return;
    }
    match lonestar_homebrew_carts::write_all() {
        Ok(games) => for (id, path, cart) in games {
            println!("Built {id}: {} ({} bytes, entry >{:04X}) -> {}", cart.title, cart.ctg().len(), cart.entry, path.display());
        },
        Err(error) => {
            eprintln!("homebrew cartridge build failed: {error}");
            std::process::exit(1);
        }
    }
}
