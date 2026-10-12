# Emulator gameplay evidence

These images are rendered from the Libre99 `Machine` framebuffer after source-built CTGs booted through the real console firmware. Key input uses `TiKey` and the emulated keyboard matrix. This is host-side emulator evidence, not Android UI/device capture.

## STARLIGHT PATROL

**PASS** — firing scored, steering changed lane, and asteroid collisions reached game over.

Input: `firmware: Space, then 2 to start the mounted cartridge → 2: fire → 3: move right into the threat lane → 4: advance threat and collide`

- `title`
  ![title](./starlight-patrol-title.png)
- `ready` — lane=1, threat=1, score=0, lives=3
  ![ready](./starlight-patrol-ready.png)

- `shot-scored` — lane=1, threat=2, score=10, lives=3
  ![shot-scored](./starlight-patrol-shot-scored.png)

- `collision` — lane=2, threat=0, score=10, lives=2
  ![collision](./starlight-patrol-collision.png)

- `game-over` — lane=2, threat=0, score=10, lives=0, mode=2
  ![game-over](./starlight-patrol-game-over.png)

- `restarted` — lane=1, threat=1, score=0, lives=3, mode=0
  ![restarted](./starlight-patrol-restarted.png)

## SOLAR SALVAGE 99

**PASS** — wall blocked movement, the cell was collected, and extraction reached the win state.

Input: `firmware: Space, then 2 to start the mounted cartridge → 2: move up → 3: attempt blocked passage → 4: move through maze → 3: move through maze → 3: move through maze → 4: move through maze → 3: move through maze → 3: move through maze`

- `title`
  ![title](./solar-salvage-99-title.png)
- `ready` — x=0, y=1, energy=8, cells=0
  ![ready](./solar-salvage-99-ready.png)

- `wall-blocked` — x=0, y=0, energy=6, cells=0
  ![wall-blocked](./solar-salvage-99-wall-blocked.png)

- `cell-collected` — x=4, y=2, energy=2, cells=1
  ![cell-collected](./solar-salvage-99-cell-collected.png)

- `extraction-win` — x=4, y=2, energy=2, cells=1, mode=1
  ![extraction-win](./solar-salvage-99-extraction-win.png)

- `restarted` — x=0, y=1, energy=8, cells=0, mode=0
  ![restarted](./solar-salvage-99-restarted.png)

## OILFIELD OUTRUN

**PASS** — clear-lane driving scored distance; a hazard collision removed a life and repeated hits ended the run.

Input: `firmware: Space, then 2 to start the mounted cartridge → 2: accelerate through clear lane → 2: accelerate through clear lane → 2: meet the rotating hazard`

- `title`
  ![title](./oilfield-outrun-title.png)
- `ready` — lane=1, hazard=2, distance=0, score=0, lives=3
  ![ready](./oilfield-outrun-ready.png)

- `distance-scored` — lane=1, hazard=1, distance=20, score=20, lives=3
  ![distance-scored](./oilfield-outrun-distance-scored.png)

- `hazard-collision` — lane=1, hazard=2, distance=30, score=20, lives=2
  ![hazard-collision](./oilfield-outrun-hazard-collision.png)

- `crash-game-over` — lane=1, hazard=2, distance=90, score=60, lives=0, mode=2
  ![crash-game-over](./oilfield-outrun-crash-game-over.png)

- `restarted` — lane=1, hazard=2, distance=0, score=0, lives=3, mode=0
  ![restarted](./oilfield-outrun-restarted.png)
