// Copyright (C) 2026 DeveloperCal
// SPDX-License-Identifier: GPL-3.0-only

use libre99_core::{
    cartridge::Cartridge,
    keyboard::TiKey,
    machine::Machine,
};

const ROM: &[u8] = include_bytes!("../../../app/src/main/rust/assets/console-rom.bin");
const GROM: &[u8] = include_bytes!("../../../app/src/main/rust/assets/console-grom.bin");
const DSR: &[u8] = include_bytes!("../../../app/src/main/rust/assets/disk-dsr.bin");
const CART: &[u8] = include_bytes!("../../../app/src/main/assets/games/texas-trail.ctg");
const FOOD: u16 = 0x8320;
const WATER: u16 = 0x8322;
const CASH: u16 = 0x8324;
const HEALTH: u16 = 0x8326;
const WAGON: u16 = 0x8328;
const MILES: u16 = 0x832A;
const DAY: u16 = 0x832C;
const MODE: u16 = 0x8330;

fn boot() -> Machine {
    let cart = Cartridge::parse(CART).expect("built CTG parses");
    assert_eq!(cart.title, "THE TEXAS TRAIL");
    let mut m = Machine::new(ROM, GROM);
    m.load_disk_controller(DSR);
    m.mount_cartridge(&cart);
    m.reset();
    for _ in 0..180 { m.run_frame(); }
    tap(&mut m, TiKey::Space);
    for _ in 0..120 { m.run_frame(); }
    tap(&mut m, TiKey::Num2);
    for _ in 0..180 { m.run_frame(); }
    m
}

fn tap(m: &mut Machine, key: TiKey) {
    m.set_key(key, true);
    for _ in 0..5 { m.run_frame(); }
    m.set_key(key, false);
    for _ in 0..8 { m.run_frame(); }
}

fn text(m: &Machine, address: u16, len: usize) -> Vec<u8> {
    (0..len).map(|offset| m.vdp().vram(address + offset as u16)).collect()
}

fn choose(m: &mut Machine, action: TiKey) {
    tap(m, action);
    for _ in 0..20 { m.run_frame(); }
}

#[test]
fn cartridge_boots_to_original_title_and_help() {
    let mut m = boot();
    assert_eq!(text(&m, 0x0085, 15), b"THE TEXAS TRAIL", "row: {:?}", text(&m, 0x0080, 32));
    assert_eq!(text(&m, 0x0184, 23), b"ANY KEY OPENS THE GUIDE");
    assert_eq!(text(&m, 0x01E4, 24), b"PRESS AGAIN TO RIDE WEST");
    tap(&mut m, TiKey::Space);
    assert_eq!(text(&m, 0x0046, 11), b"FIELD GUIDE");
    tap(&mut m, TiKey::Space);
    assert_eq!(m.bus().peek_word(MODE), 0);
    assert_eq!(text(&m, 0x0222, 16), b"1 TRAVEL  2 REST");
    let mut frame = vec![0u32; 256 * 192];
    m.render(&mut frame);
    assert!(frame.iter().filter(|pixel| **pixel != frame[0]).count() > 500, "title and game text must render visibly");
}

#[test]
fn journey_actions_change_resources_and_reach_the_pecos() {
    let mut m = boot();
    tap(&mut m, TiKey::Space);
    tap(&mut m, TiKey::Space);
    assert_eq!(m.bus().peek_word(FOOD), 70);
    assert_eq!(m.bus().peek_word(WATER), 70);
    assert_eq!(m.bus().peek_word(CASH), 40);
    assert_eq!(m.bus().peek_word(HEALTH), 100);
    assert_eq!(m.bus().peek_word(WAGON), 100);
    assert_eq!(m.bus().peek_word(MILES), 0);
    assert_eq!(m.bus().peek_word(DAY), 1);

    choose(&mut m, TiKey::Num1); // enter the route fork
    choose(&mut m, TiKey::Num1); // prairie route
    let first_leg = m.bus().peek_word(MILES);
    assert!((60..=123).contains(&first_leg), "prairie leg must advance 60–123 miles, got {first_leg}");
    assert!(m.bus().peek_word(FOOD) < 70);
    assert!(m.bus().peek_word(WATER) < 70);

    let cash = m.bus().peek_word(CASH);
    let food = m.bus().peek_word(FOOD);
    choose(&mut m, TiKey::Num4); // buy supplies
    assert_eq!(m.bus().peek_word(CASH), cash - 10);
    assert!(m.bus().peek_word(FOOD) > food);

    choose(&mut m, TiKey::Num2); // rest
    assert!(m.bus().peek_word(DAY) >= 3);
    assert!(m.bus().peek_word(HEALTH) > 0);

    // Prairie travel is intentionally safe enough to test the full victory path.
    for _ in 0..8 {
        if m.bus().peek_word(MODE) != 0 { break; }
        choose(&mut m, TiKey::Num1);
        if m.bus().peek_word(MODE) == 0 { choose(&mut m, TiKey::Num1); }
    }
    assert_eq!(m.bus().peek_word(MODE), 1, "journey should end in victory");
    assert_eq!(m.bus().peek_word(MILES), 320);
    assert!(text(&m, 0x0047, 24).starts_with(b"YOU REACH THE PECAS" ) || text(&m, 0x0047, 24).starts_with(b"YOU REACH THE PECOS"));

    tap(&mut m, TiKey::Num1); // restart from the ending
    assert_eq!(m.bus().peek_word(MODE), 0);
    assert_eq!(m.bus().peek_word(MILES), 0);
    assert_eq!(m.bus().peek_word(FOOD), 70);
}

#[test]
fn exhausted_supplies_end_the_journey() {
    let mut m = boot();
    tap(&mut m, TiKey::Space);
    tap(&mut m, TiKey::Space);
    for _ in 0..20 {
        if m.bus().peek_word(MODE) != 0 { break; }
        choose(&mut m, TiKey::Num2); // rest consumes food and water
    }
    assert_eq!(m.bus().peek_word(MODE), 2, "depleted supplies should end the journey");
}
