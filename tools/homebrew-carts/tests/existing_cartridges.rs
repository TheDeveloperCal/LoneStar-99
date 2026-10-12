// Copyright (C) 2026 DeveloperCal
// SPDX-License-Identifier: GPL-3.0-only

use libre99_core::{cartridge::Cartridge, keyboard::TiKey, machine::Machine};

const ROM: &[u8] = include_bytes!("../../../app/src/main/rust/assets/console-rom.bin");
const GROM: &[u8] = include_bytes!("../../../app/src/main/rust/assets/console-grom.bin");
const DSR: &[u8] = include_bytes!("../../../app/src/main/rust/assets/disk-dsr.bin");
const TITRIS: &[u8] = include_bytes!("../../../app/src/main/assets/games/titris.ctg");
const SOKOBAN: &[u8] = include_bytes!("../../../app/src/main/assets/games/sokoban.ctg");

fn frames(machine: &mut Machine, n: usize) {
    for _ in 0..n { machine.run_frame(); }
}

fn tap(machine: &mut Machine, key: TiKey) {
    machine.set_key(key, true);
    frames(machine, 6);
    machine.set_key(key, false);
    frames(machine, 10);
}

fn start(bytes: &[u8]) -> Machine {
    let cart = Cartridge::parse(bytes).expect("bundled cartridge is valid");
    let mut machine = Machine::new(ROM, GROM);
    machine.load_disk_controller(DSR);
    machine.mount_cartridge(&cart);
    machine.reset();
    frames(&mut machine, 180);
    tap(&mut machine, TiKey::Space);
    frames(&mut machine, 120);
    tap(&mut machine, TiKey::Num2);
    frames(&mut machine, 180);
    machine
}

fn framebuffer(machine: &mut Machine) -> Vec<u32> {
    let mut pixels = vec![0; 256 * 192];
    machine.render(&mut pixels);
    pixels
}

#[test]
fn titris_bundle_starts_a_piece_and_real_joystick_input_moves_it() {
    let mut machine = start(TITRIS);
    tap(&mut machine, TiKey::Space);
    frames(&mut machine, 30);
    let x_before = machine.bus().peek_word(0x8324);
    assert_ne!(machine.bus().peek_word(0x8330), 1, "Titris must not start in game-over");
    let before = framebuffer(&mut machine);
    tap(&mut machine, TiKey::Joy1Left);
    frames(&mut machine, 4);
    let x_after = machine.bus().peek_word(0x8324);
    let after = framebuffer(&mut machine);
    assert!(x_after < x_before, "left input should move the active piece from {x_before} to a lower x, got {x_after}");
    assert_ne!(before, after, "the active board must visibly respond to the move");
}

#[test]
fn sokoban_bundle_moves_and_pushes_on_the_real_board() {
    let mut machine = start(SOKOBAN);
    tap(&mut machine, TiKey::Space);
    frames(&mut machine, 30);
    assert_eq!(machine.bus().peek_word(0x8320), 0, "Sokoban should start at level one");
    assert_eq!(machine.bus().peek_word(0x8324), 0, "Sokoban should start with zero moves");
    let start_pos = machine.bus().peek_word(0x8322);
    let before = framebuffer(&mut machine);
    for key in [TiKey::Joy1Right, TiKey::Joy1Down, TiKey::Joy1Down, TiKey::Joy1Left] {
        tap(&mut machine, key);
        frames(&mut machine, 2);
    }
    let after = framebuffer(&mut machine);
    assert_eq!(machine.bus().peek_word(0x8324), 4, "the scripted path should count four moves");
    assert_eq!(machine.bus().peek_word(0x8326), 1, "the final step should push a box");
    assert_ne!(machine.bus().peek_word(0x8322), start_pos, "movement should change the player's board position");
    assert_ne!(before, after, "movement and push should change the rendered board");
}
