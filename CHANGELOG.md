# Changelog

## 0.1.0-beta.1

Initial public beta for Minecraft 1.21.1 Fabric.

### Added

- Cinematic death rewind sequence with custom PNG cutscene and SFX.
- Safe checkpoint restore system.
- Bed click checkpoint setting during day or night.
- Death Rewind armor enchantment.
- Activation modes: `enchantment`, `totem`, `both`, and `always`.
- XP level cost system for enchantment and always modes.
- Totem fallback in `both` mode when enchantment activation is blocked by XP.
- Configurable cutscene timing and cutscene on/off option.
- White return overlay and white revive particles.
- Post-revive invulnerability window to reduce death loops.
- Per-player debug log command: `/revivebydeath log on/off`.
- Player status command: `/revivebydeath status`.
- OP commands for mode switching, cutscene toggle, test book, and XP cost reset.

### Notes

- Requires Fabric API.
- Required on both client and server.
- No copyrighted anime/game/movie assets are included.
