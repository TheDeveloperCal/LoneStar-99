# Pinned console-GROM builder

This directory vendors the pure-Rust `libre99-gpl` assembler and its GROM
verification tests from Libre99 commit
`6c86f10bae65f4cc3bdc13289e14bb20af03ab61` (see `UPSTREAM_REVISION`). This is
the same revision recorded by the existing firmware import. Its dependencies
are the matching `libre99-asm` and `libre99-core` source already in this project;
no network access or third-party Cargo package is required.

From the project root, run:

```sh
./tools/build-console-grom.sh
```

The script assembles `app/src/main/rust/firmware-source/console-grom.gpl`,
splices the pinned font/keymap and the LS-99 Texas emblem, writes
`app/src/main/rust/assets/console-grom.bin`, then runs the GPL crate's tests,
including the staleness gate against that exact bundled image. Preserve the
upstream license and copyright headers; see `LICENSE.md` in this directory and
`app/src/main/rust/LIBRE99_LICENSE.md`.
