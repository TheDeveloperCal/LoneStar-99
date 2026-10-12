# Bundled clean-room firmware

The `.bin` firmware embedded by the Android JNI crate is built from the three
source files in this directory, copied from Libre99 upstream commit `6c86f10`:

- `console-rom.asm` builds `assets/console-rom.bin`.
- `console-grom.gpl` builds `assets/console-grom.bin`.
- `disk-dsr.asm` builds `assets/disk-dsr.bin`.

These are Libre99 original clean-room firmware sources under the same Modified
MIT License with Commons Clause as the core (see `../LIBRE99_LICENSE.md`). No
TI console ROM/GROM, disk DSR, or commercial software image is bundled. TI
BASIC proper remains unavailable without user-provided authentic TI firmware.
