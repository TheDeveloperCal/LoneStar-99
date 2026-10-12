// Copyright (C) 2026 DeveloperCal
// SPDX-License-Identifier: GPL-3.0-only

use lonestar_homebrew_carts::{game_ids, run_gameplay};
use std::{env, fs, path::PathBuf};

fn main() {
    let mut args = env::args().skip(1);
    let out = match (args.next().as_deref(), args.next()) {
        (Some("--out"), Some(path)) => PathBuf::from(path),
        _ => {
            eprintln!("usage: cargo run --offline --manifest-path tools/homebrew-carts/Cargo.toml --bin gameplay_evidence -- --out DIR");
            std::process::exit(2);
        }
    };
    if let Err(e) = fs::create_dir_all(&out) { eprintln!("{}: {e}", out.display()); std::process::exit(1); }

    let mut report = String::from("# Emulator gameplay evidence\n\nThese images are rendered from the Libre99 `Machine` framebuffer after source-built CTGs booted through the real console firmware. Key input uses `TiKey` and the emulated keyboard matrix. This is host-side emulator evidence, not Android UI/device capture.\n\n");
    let mut json = String::from("{\n  \"execution\": \"Libre99 Machine + console firmware + source-built CTG + TiKey matrix\",\n  \"games\": [\n");
    let ids: Vec<_> = game_ids().collect();
    for (game_index, id) in ids.iter().enumerate() {
        let evidence = match run_gameplay(id) {
            Ok(v) => v,
            Err(e) => { eprintln!("{id}: FAIL: {e}"); std::process::exit(1); }
        };
        report.push_str(&format!("## {}\n\n**PASS** — {}.\n\nInput: `{}`\n\n", evidence.title, evidence.objective, evidence.inputs.join(" → ")));
        json.push_str(&format!("    {{\"id\":\"{}\",\"title\":\"{}\",\"status\":\"PASS\",\"objective\":\"{}\",\"inputs\":[{}],\"checkpoints\":[",
            esc(&evidence.id), esc(&evidence.title), esc(&evidence.objective), evidence.inputs.iter().map(|s| format!("\"{}\"", esc(s))).collect::<Vec<_>>().join(",")));
        for (checkpoint_index, checkpoint) in evidence.checkpoints.iter().enumerate() {
            let filename = format!("{}-{}.png", evidence.id, checkpoint.name);
            if let Err(e) = write_png(&out.join(&filename), &checkpoint.framebuffer) {
                eprintln!("could not write {}: {e}", out.join(&filename).display());
                std::process::exit(1);
            }
            let metrics = checkpoint.metrics.iter().map(|(n, v)| format!("{n}={v}")).collect::<Vec<_>>().join(", ");
            if metrics.is_empty() {
                report.push_str(&format!("- `{}`\n  ![{}](./{})\n", checkpoint.name, checkpoint.name, filename));
            } else {
                report.push_str(&format!("- `{}` — {}\n  ![{}](./{})\n", checkpoint.name, metrics, checkpoint.name, filename));
            }
            if checkpoint_index != 0 { report.push('\n'); }
            if checkpoint_index > 0 { json.push(','); }
            let metrics = checkpoint.metrics.iter().map(|(n, v)| format!("\"{}\":{}", esc(n), v)).collect::<Vec<_>>().join(",");
            json.push_str(&format!("{{\"name\":\"{}\",\"metrics\":{{{}}},\"screenshot\":\"{}\"}}", esc(&checkpoint.name), metrics, filename));
        }
        json.push_str(" ]}");
        if game_index + 1 != ids.len() { json.push(','); }
        json.push('\n');
    }
    json.push_str("  ]\n}\n");
    report.truncate(report.trim_end().len());
    report.push('\n');
    if let Err(e) = fs::write(out.join("gameplay-report.md"), report) { eprintln!("write report: {e}"); std::process::exit(1); }
    if let Err(e) = fs::write(out.join("gameplay-summary.json"), json) { eprintln!("write JSON: {e}"); std::process::exit(1); }
    println!("PASS: {} source-built game cartridges completed real-emulator gameplay; evidence at {}", ids.len(), out.display());
}

fn esc(s: &str) -> String {
    s.replace('\\', "\\\\").replace('"', "\\\"").replace('\n', "\\n")
}

fn write_png(path: &PathBuf, pixels: &[u32]) -> std::io::Result<()> {
    use std::io::Write;
    let (width, height) = (256usize, 192usize);
    assert_eq!(pixels.len(), width * height);
    let mut raw = Vec::with_capacity((width * 3 + 1) * height);
    for row in pixels.chunks_exact(width) {
        raw.push(0); // PNG filter: None.
        for &pixel in row {
            raw.push((pixel >> 16) as u8);
            raw.push((pixel >> 8) as u8);
            raw.push(pixel as u8);
        }
    }
    let mut zlib = vec![0x78, 0x01];
    let mut offset = 0;
    while offset < raw.len() {
        let size = (raw.len() - offset).min(65_535);
        let final_block = offset + size == raw.len();
        zlib.push(if final_block { 1 } else { 0 });
        zlib.extend_from_slice(&(size as u16).to_le_bytes());
        zlib.extend_from_slice(&(!(size as u16)).to_le_bytes());
        zlib.extend_from_slice(&raw[offset..offset + size]);
        offset += size;
    }
    zlib.extend_from_slice(&adler32(&raw).to_be_bytes());
    let mut png = Vec::new();
    png.extend_from_slice(b"\x89PNG\r\n\x1a\n");
    let mut ihdr = Vec::new();
    ihdr.extend_from_slice(&(width as u32).to_be_bytes());
    ihdr.extend_from_slice(&(height as u32).to_be_bytes());
    ihdr.extend_from_slice(&[8, 2, 0, 0, 0]);
    chunk(&mut png, b"IHDR", &ihdr);
    chunk(&mut png, b"IDAT", &zlib);
    chunk(&mut png, b"IEND", &[]);
    let mut file = fs::File::create(path)?;
    file.write_all(&png)
}

fn chunk(png: &mut Vec<u8>, kind: &[u8; 4], data: &[u8]) {
    png.extend_from_slice(&(data.len() as u32).to_be_bytes());
    png.extend_from_slice(kind);
    png.extend_from_slice(data);
    let mut crc_input = Vec::with_capacity(kind.len() + data.len());
    crc_input.extend_from_slice(kind);
    crc_input.extend_from_slice(data);
    png.extend_from_slice(&crc32(&crc_input).to_be_bytes());
}

fn crc32(bytes: &[u8]) -> u32 {
    let mut crc = !0u32;
    for byte in bytes {
        crc ^= *byte as u32;
        for _ in 0..8 { crc = (crc >> 1) ^ (0xEDB8_8320 & (0u32.wrapping_sub(crc & 1))); }
    }
    !crc
}

fn adler32(bytes: &[u8]) -> u32 {
    let (mut a, mut b) = (1u32, 0u32);
    for byte in bytes { a = (a + *byte as u32) % 65_521; b = (b + a) % 65_521; }
    (b << 16) | a
}
