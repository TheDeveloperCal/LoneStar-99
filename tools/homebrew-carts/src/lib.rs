// Copyright (C) 2026 DeveloperCal
// SPDX-License-Identifier: GPL-3.0-only

use libre99_asm::{assemble, Assembly, Options};
use libre99_core::{cartridge::Cartridge, keyboard::TiKey, machine::Machine};
use std::path::{Path, PathBuf};

const FONT_MARKER: &str = "* @HOMEBREW_FONT";
const RUNTIME_MARKER: &str = "* @HOMEBREW_RUNTIME";
const GAMES: [(&str, &str, &str); 3] = [
    ("starlight-patrol", "STARLIGHT PATROL", "games/starlight-patrol.asm"),
    ("solar-salvage-99", "SOLAR SALVAGE 99", "games/solar-salvage.asm"),
    ("oilfield-outrun", "OILFIELD OUTRUN", "games/oilfield-outrun.asm"),
];

pub fn project_root() -> PathBuf {
    Path::new(env!("CARGO_MANIFEST_DIR")).join("../..").canonicalize().expect("project root")
}

pub fn game_ids() -> impl Iterator<Item = &'static str> {
    GAMES.iter().map(|(id, _, _)| *id)
}

pub fn cartridge_name(id: &str) -> Option<&'static str> {
    GAMES.iter().find(|(game_id, _, _)| *game_id == id).map(|(_, name, _)| *name)
}

pub fn build_game(id: &str) -> Result<Assembly, String> {
    let (_, title, source_path) = GAMES.iter().find(|(game_id, _, _)| *game_id == id)
        .ok_or_else(|| format!("unknown homebrew game: {id}"))?;
    let root = project_root();
    let source = std::fs::read_to_string(root.join(source_path)).map_err(|e| e.to_string())?;
    let runtime = std::fs::read_to_string(root.join("games/common/runtime.asm")).map_err(|e| e.to_string())?;
    let font = std::fs::read_to_string(root.join("games/texas-trail/font-table.asm")).map_err(|e| e.to_string())?;
    if source.matches(RUNTIME_MARKER).count() != 1 || runtime.matches(FONT_MARKER).count() != 1 {
        return Err(format!("{id}: expected one runtime and one font marker"));
    }
    let runtime = runtime.replace(FONT_MARKER, font.trim_end());
    let source = source.replace(RUNTIME_MARKER, &runtime);
    let options = Options { name: Some((*title).to_owned()), ..Options::default() };
    assemble(&source, &options).map_err(|diagnostics| diagnostics.into_iter().map(|d| d.to_string()).collect::<Vec<_>>().join("\n"))
}

pub fn build_all() -> Result<Vec<(String, Assembly)>, String> {
    GAMES.iter().map(|(id, _, _)| build_game(id).map(|cart| ((*id).to_owned(), cart))).collect()
}

pub fn write_all() -> Result<Vec<(String, PathBuf, Assembly)>, String> {
    let root = project_root();
    let asset_dir = root.join("app/src/main/assets/games");
    std::fs::create_dir_all(&asset_dir).map_err(|e| e.to_string())?;
    build_all()?.into_iter().map(|(id, cart)| {
        let path = asset_dir.join(format!("{id}.ctg"));
        std::fs::write(&path, cart.ctg()).map_err(|e| format!("{}: {e}", path.display()))?;
        Ok((id, path, cart))
    }).collect()
}

/// Verifies generated cartridge assets are byte-for-byte reproducible.
pub fn verify_committed_assets() -> Result<(), String> {
    let root = project_root();
    for (id, cart) in build_all()? {
        let path = root.join("app/src/main/assets/games").join(format!("{id}.ctg"));
        let committed = std::fs::read(&path).map_err(|e| format!("{}: {e}", path.display()))?;
        if committed != cart.ctg() {
            return Err(format!("{} differs from source-built cartridge; run tools/build-homebrew-carts.sh", path.display()));
        }
    }
    Ok(())
}

const ROM: &[u8] = include_bytes!("../../../app/src/main/rust/assets/console-rom.bin");
const GROM: &[u8] = include_bytes!("../../../app/src/main/rust/assets/console-grom.bin");
const DSR: &[u8] = include_bytes!("../../../app/src/main/rust/assets/disk-dsr.bin");
const STARLIGHT: &[u8] = include_bytes!("../../../app/src/main/assets/games/starlight-patrol.ctg");
const SOLAR: &[u8] = include_bytes!("../../../app/src/main/assets/games/solar-salvage-99.ctg");
const OILFIELD: &[u8] = include_bytes!("../../../app/src/main/assets/games/oilfield-outrun.ctg");

#[derive(Debug)]
pub struct Checkpoint {
    pub name: String,
    pub screen_text: String,
    pub metrics: Vec<(String, u16)>,
    pub framebuffer: Vec<u32>,
}

#[derive(Debug)]
pub struct GameEvidence {
    pub id: String,
    pub title: String,
    pub inputs: Vec<String>,
    pub checkpoints: Vec<Checkpoint>,
    pub objective: String,
}

fn cartridge_bytes(id: &str) -> Option<&'static [u8]> {
    match id {
        "starlight-patrol" => Some(STARLIGHT),
        "solar-salvage-99" => Some(SOLAR),
        "oilfield-outrun" => Some(OILFIELD),
        _ => None,
    }
}

fn make_machine(id: &str) -> Result<Machine, String> {
    let bytes = cartridge_bytes(id).ok_or_else(|| format!("no cartridge for {id}"))?;
    let cart = Cartridge::parse(bytes).map_err(|e| format!("{id} cartridge rejected: {e:?}"))?;
    let mut machine = Machine::new(ROM, GROM);
    machine.load_disk_controller(DSR);
    machine.mount_cartridge(&cart);
    machine.reset();
    frames(&mut machine, 180);
    tap(&mut machine, TiKey::Space);
    frames(&mut machine, 120);
    tap(&mut machine, TiKey::Num2);
    frames(&mut machine, 180);
    let title = cartridge_name(id).expect("known title");
    if !screen_contains(&machine, title) {
        return Err(format!("{id} did not reach its title screen in the TI emulator"));
    }
    Ok(machine)
}

fn frames(machine: &mut Machine, count: usize) {
    for _ in 0..count { machine.run_frame(); }
}

fn tap(machine: &mut Machine, key: TiKey) {
    machine.set_key(key, true);
    frames(machine, 5);
    machine.set_key(key, false);
    frames(machine, 8);
}

fn screen_text(machine: &Machine) -> String {
    (0..768).map(|i| machine.vdp().vram(i) as char).collect()
}

fn screen_contains(machine: &Machine, text: &str) -> bool {
    screen_text(machine).contains(text)
}

fn framebuffer(machine: &mut Machine) -> Vec<u32> {
    let mut pixels = vec![0; 256 * 192];
    machine.render(&mut pixels);
    pixels
}

fn checkpoint(machine: &mut Machine, name: &str, metrics: &[(&str, u16)]) -> Checkpoint {
    Checkpoint {
        name: name.to_owned(),
        screen_text: screen_text(machine),
        metrics: metrics.iter().map(|(name, addr)| ((*name).to_owned(), machine.bus().peek_word(*addr))).collect(),
        framebuffer: framebuffer(machine),
    }
}

fn expect(condition: bool, message: impl Into<String>) -> Result<(), String> {
    if condition { Ok(()) } else { Err(message.into()) }
}

/// Boots a generated CTG in the same Libre99 machine and real console firmware
/// used by the Android app, then drives a deterministic game-specific sequence.
pub fn run_gameplay(id: &str) -> Result<GameEvidence, String> {
    use TiKey::*;
    let mut machine = make_machine(id)?;
    let title = cartridge_name(id).ok_or_else(|| format!("unknown game {id}"))?.to_owned();
    let mut inputs = vec!["firmware: Space, then 2 to start the mounted cartridge".to_owned()];
    let mut checkpoints = vec![checkpoint(&mut machine, "title", &[])];
    tap(&mut machine, Space);

    let objective = match id {
        "starlight-patrol" => {
            let (lane, threat, score, lives, mode) = (0x8320, 0x8322, 0x8324, 0x8326, 0x8328);
            expect(machine.bus().peek_word(lane) == 1 && machine.bus().peek_word(threat) == 1, format!("Starlight start state mismatch: lane={} threat={} score={} lives={} mode={}", machine.bus().peek_word(lane), machine.bus().peek_word(threat), machine.bus().peek_word(score), machine.bus().peek_word(lives), machine.bus().peek_word(mode)))?;
            checkpoints.push(checkpoint(&mut machine, "ready", &[("lane", lane), ("threat", threat), ("score", score), ("lives", lives)]));
            tap(&mut machine, Num2);
            inputs.push("2: fire".into());
            expect(machine.bus().peek_word(score) == 10, "firing did not increase score to 10")?;
            checkpoints.push(checkpoint(&mut machine, "shot-scored", &[("lane", lane), ("threat", threat), ("score", score), ("lives", lives)]));
            tap(&mut machine, Num3);
            inputs.push("3: move right into the threat lane".into());
            tap(&mut machine, Num4);
            inputs.push("4: advance threat and collide".into());
            expect(machine.bus().peek_word(lives) == 2, "threat collision did not remove one life")?;
            checkpoints.push(checkpoint(&mut machine, "collision", &[("lane", lane), ("threat", threat), ("score", score), ("lives", lives)]));
            for _ in 0..24 {
                if machine.bus().peek_word(mode) != 0 { break; }
                tap(&mut machine, Num4);
            }
            expect(machine.bus().peek_word(mode) == 2, "repeated collisions did not reach game over")?;
            checkpoints.push(checkpoint(&mut machine, "game-over", &[("lane", lane), ("threat", threat), ("score", score), ("lives", lives), ("mode", mode)]));
            tap(&mut machine, Space);
            expect(machine.bus().peek_word(mode) == 0 && machine.bus().peek_word(score) == 0 && machine.bus().peek_word(lives) == 3, "game-over restart did not reset patrol state")?;
            checkpoints.push(checkpoint(&mut machine, "restarted", &[("lane", lane), ("threat", threat), ("score", score), ("lives", lives), ("mode", mode)]));
            "firing scored, steering changed lane, and asteroid collisions reached game over".to_owned()
        }
        "solar-salvage-99" => {
            let (x, y, energy, cells, mode) = (0x8320, 0x8322, 0x8324, 0x8326, 0x8328);
            checkpoints.push(checkpoint(&mut machine, "ready", &[("x", x), ("y", y), ("energy", energy), ("cells", cells)]));
            tap(&mut machine, Num2); // Up into the top passage.
            inputs.push("2: move up".into());
            tap(&mut machine, Num3); // Wall at (1,0).
            inputs.push("3: attempt blocked passage".into());
            expect(machine.bus().peek_word(x) == 0 && machine.bus().peek_word(y) == 0 && machine.bus().peek_word(energy) == 6, format!("wall collision mismatch: x={} y={} energy={}", machine.bus().peek_word(x), machine.bus().peek_word(y), machine.bus().peek_word(energy)))?;
            checkpoints.push(checkpoint(&mut machine, "wall-blocked", &[("x", x), ("y", y), ("energy", energy), ("cells", cells)]));
            for key in [Num4, Num3, Num3, Num4, Num3, Num3] {
                tap(&mut machine, key);
                inputs.push(format!("{}: move through maze", match key { Num1 => 1, Num2 => 2, Num3 => 3, _ => 4 }));
            }
            expect(machine.bus().peek_word(cells) == 1, "energy cell was not collected")?;
            checkpoints.push(checkpoint(&mut machine, "cell-collected", &[("x", x), ("y", y), ("energy", energy), ("cells", cells)]));
            expect(machine.bus().peek_word(mode) == 1, "collected cell did not allow extraction win")?;
            expect(screen_contains(&machine, "SALVAGE RECOVERED"), "visible win message missing from VRAM")?;
            checkpoints.push(checkpoint(&mut machine, "extraction-win", &[("x", x), ("y", y), ("energy", energy), ("cells", cells), ("mode", mode)]));
            tap(&mut machine, Space);
            expect(machine.bus().peek_word(mode) == 0 && machine.bus().peek_word(cells) == 0 && machine.bus().peek_word(energy) == 8, "win-screen restart did not reset puzzle state")?;
            checkpoints.push(checkpoint(&mut machine, "restarted", &[("x", x), ("y", y), ("energy", energy), ("cells", cells), ("mode", mode)]));
            "wall blocked movement, the cell was collected, and extraction reached the win state".to_owned()
        }
        "oilfield-outrun" => {
            let (lane, hazard, distance, score, lives, mode) = (0x8320, 0x8322, 0x8324, 0x8326, 0x8328, 0x832A);
            checkpoints.push(checkpoint(&mut machine, "ready", &[("lane", lane), ("hazard", hazard), ("distance", distance), ("score", score), ("lives", lives)]));
            for _ in 0..2 {
                tap(&mut machine, Num2);
                inputs.push("2: accelerate through clear lane".into());
            }
            expect(machine.bus().peek_word(distance) == 20 && machine.bus().peek_word(score) == 20, format!("clear driving mismatch: distance={} score={} lives={} lane={} hazard={}", machine.bus().peek_word(distance), machine.bus().peek_word(score), machine.bus().peek_word(lives), machine.bus().peek_word(lane), machine.bus().peek_word(hazard)))?;
            checkpoints.push(checkpoint(&mut machine, "distance-scored", &[("lane", lane), ("hazard", hazard), ("distance", distance), ("score", score), ("lives", lives)]));
            tap(&mut machine, Num2);
            inputs.push("2: meet the rotating hazard".into());
            expect(machine.bus().peek_word(lives) == 2, "hazard collision did not remove a life")?;
            checkpoints.push(checkpoint(&mut machine, "hazard-collision", &[("lane", lane), ("hazard", hazard), ("distance", distance), ("score", score), ("lives", lives)]));
            for _ in 0..48 {
                if machine.bus().peek_word(mode) != 0 { break; }
                tap(&mut machine, Num2);
            }
            expect(machine.bus().peek_word(mode) == 2, "repeated hazard hits did not end the run")?;
            expect(screen_contains(&machine, "OUT OF LIVES"), "visible crash message missing from VRAM")?;
            checkpoints.push(checkpoint(&mut machine, "crash-game-over", &[("lane", lane), ("hazard", hazard), ("distance", distance), ("score", score), ("lives", lives), ("mode", mode)]));
            tap(&mut machine, Space);
            expect(machine.bus().peek_word(mode) == 0 && machine.bus().peek_word(distance) == 0 && machine.bus().peek_word(lives) == 3, "crash-screen restart did not reset drive state")?;
            checkpoints.push(checkpoint(&mut machine, "restarted", &[("lane", lane), ("hazard", hazard), ("distance", distance), ("score", score), ("lives", lives), ("mode", mode)]));
            "clear-lane driving scored distance; a hazard collision removed a life and repeated hits ended the run".to_owned()
        }
        _ => return Err(format!("unknown homebrew game {id}")),
    };

    Ok(GameEvidence { id: id.to_owned(), title, inputs, checkpoints, objective })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn all_homebrew_sources_build_as_titled_cartridges() {
        for (id, cart) in build_all().expect("all source assembles") {
            assert_eq!(cart.title, cartridge_name(&id).unwrap());
            assert_eq!(cart.entry, cart.symbol("START").expect("start symbol"));
            assert!(!cart.ctg().is_empty());
        }
    }

    #[test]
    fn committed_cartridges_match_current_assembly_sources() {
        verify_committed_assets().expect("generated CTGs must match their assembly sources");
    }
}
