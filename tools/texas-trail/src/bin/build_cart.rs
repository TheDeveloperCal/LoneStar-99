// Copyright (C) 2026 DeveloperCal
// SPDX-License-Identifier: GPL-3.0-only
// Generated cartridge content also includes third-party licensed material;
// see THIRD_PARTY_NOTICES.md.

use libre99_asm::{assemble, Assembly, Options};

fn assemble_cartridge() -> Assembly {
    let source = include_str!("../../../../games/texas-trail/texas-trail.asm");
    let font_table = include_str!("../../../../games/texas-trail/font-table.asm");
    let marker = "* @FONT_TABLE_FROM_BUILD";
    assert_eq!(source.matches(marker).count(), 1, "font table marker must appear exactly once");
    let source = source.replace(marker, font_table);
    let options = Options {
        name: Some("THE TEXAS TRAIL".to_string()),
        ..Options::default()
    };
    assemble(&source, &options).unwrap_or_else(|errors| {
        for error in errors {
            eprintln!("{error}");
        }
        std::process::exit(1);
    })
}

fn main() {
    let cartridge = assemble_cartridge();
    let output = "app/src/main/assets/games/texas-trail.ctg";
    std::fs::write(output, cartridge.ctg()).expect("write built cartridge");
    println!(
        "Built {} ({} bytes), entry >{:04X}",
        cartridge.title,
        cartridge.ctg().len(),
        cartridge.entry
    );
}

#[cfg(test)]
mod tests {
    use super::assemble_cartridge;

    #[test]
    fn build_injects_the_separately_licensed_font_table() {
        let cartridge = assemble_cartridge();
        assert_eq!(cartridge.title, "THE TEXAS TRAIL");
        assert!(cartridge.symbol("FONT").is_some());
        assert!(!cartridge.ctg().is_empty());
    }
}
