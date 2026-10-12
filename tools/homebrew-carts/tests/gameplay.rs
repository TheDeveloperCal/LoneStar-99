// Copyright (C) 2026 DeveloperCal
// SPDX-License-Identifier: GPL-3.0-only

use lonestar_homebrew_carts::run_gameplay;

#[test]
fn starlight_real_machine_scores_moves_and_reaches_game_over() {
    let run = run_gameplay("starlight-patrol").expect("Starlight Patrol scripted TI gameplay");
    assert_eq!(run.title, "STARLIGHT PATROL");
    assert!(run.checkpoints.iter().any(|c| c.name == "collision"));
    assert!(run.checkpoints.iter().any(|c| c.name == "game-over"));
    assert!(run.checkpoints.iter().any(|c| c.name == "game-over" && c.screen_text.contains("PATROL OVER")));
    assert!(run.checkpoints.last().unwrap().metrics.iter().any(|(name, value)| name == "mode" && *value == 0));
    assert!(run.checkpoints.windows(2).any(|w| w[0].framebuffer != w[1].framebuffer), "gameplay must change rendered pixels");
}

#[test]
fn solar_real_machine_blocks_wall_collects_cell_and_wins() {
    let run = run_gameplay("solar-salvage-99").expect("Solar Salvage scripted TI gameplay");
    assert_eq!(run.title, "SOLAR SALVAGE 99");
    assert!(run.checkpoints.iter().any(|c| c.name == "wall-blocked"));
    assert!(run.checkpoints.iter().any(|c| c.name == "cell-collected"));
    assert!(run.checkpoints.iter().any(|c| c.name == "extraction-win" && c.screen_text.contains("SALVAGE RECOVERED")));
    assert!(run.checkpoints.last().unwrap().metrics.iter().any(|(name, value)| name == "mode" && *value == 0));
    assert!(run.checkpoints.windows(2).any(|w| w[0].framebuffer != w[1].framebuffer), "maze play must change rendered pixels");
}

#[test]
fn oilfield_real_machine_scores_distance_and_crashes_after_hazards() {
    let run = run_gameplay("oilfield-outrun").expect("Oilfield Outrun scripted TI gameplay");
    assert_eq!(run.title, "OILFIELD OUTRUN");
    assert!(run.checkpoints.iter().any(|c| c.name == "distance-scored"));
    assert!(run.checkpoints.iter().any(|c| c.name == "hazard-collision"));
    assert!(run.checkpoints.iter().any(|c| c.name == "crash-game-over" && c.screen_text.contains("OUT OF LIVES")));
    assert!(run.checkpoints.last().unwrap().metrics.iter().any(|(name, value)| name == "mode" && *value == 0));
    assert!(run.checkpoints.windows(2).any(|w| w[0].framebuffer != w[1].framebuffer), "driving must change rendered pixels");
}
