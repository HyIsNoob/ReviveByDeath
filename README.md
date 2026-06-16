# ReviveByDeath

ReviveByDeath is a Fabric mod for Minecraft 1.21.1 that replaces fatal moments with a short cinematic rewind sequence, then restores the player to their latest safe checkpoint.

All visual and audio assets in this project are original placeholders/custom assets. Do not add copyrighted anime, game, movie, logo, quote, or sound content unless you have explicit rights to use and monetize it.

## Requirements

- Minecraft 1.21.1
- Fabric Loader 0.19.3 or newer
- Fabric API
- Java 21

ReviveByDeath is required on both client and server.

The server controls checkpoints, XP costs, activation requirements, and revive logic. The client is required for the cutscene PNG sequence, screen overlay, client-side particles, and cutscene audio handling.

## Activation Modes

`activationMode` controls what is required to trigger a rewind.

- `enchantment`: Requires at least one equipped armor piece with Death Rewind. This is the default survival-friendly mode.
- `totem`: Requires a Totem of Undying in either hand. If `replaceVanillaTotem` is true, the mod replaces the vanilla totem behavior with the cinematic rewind.
- `both`: Allows either Death Rewind armor or a totem. Armor enchantment is checked first, so a player wearing Death Rewind will not consume a totem.
- `always`: No item requirement. Useful for testing or showcases, not recommended for balanced survival.

## Death Rewind Enchantment

Death Rewind is a level 1 armor enchantment.

It is available through vanilla-style progression:

- Enchanting table, because it is tagged as `minecraft:non_treasure`.
- Villager trading, because it is tagged as `minecraft:tradeable`.
- OP test command: `/revivebydeath give_book`.

Only one equipped armor piece needs the enchantment to activate rewind.

## Checkpoints

The server automatically tracks each player's latest safe checkpoint.

A checkpoint is saved only when the player is:

- alive;
- on the ground;
- not in lava, fire, or the void;
- standing on a sturdy non-liquid block;
- in enough open space for feet and head.

Players can also click a bed during day or night to manually save a checkpoint at their current safe position.

After a checkpoint exists, automatic checkpoint checks are spaced by `checkpointRecheckTicks`. The default is `6000` ticks, about 5 minutes.

## XP Cost

When `useXpCost` is true, enchantment and `always` rewinds cost XP levels. Totem rewinds consume the totem instead, so they can still work when the player does not have enough XP.

Default balance:

```json
"minimumXpLevelCost": 6,
"xpLevelCostIncrease": 2,
"xpLevelCostMultiplier": 1.18
```

The cost resets when the player sleeps if `resetCostOnSleep` is true. It also resets after a normal death if `resetCostOnDeath` is true.

## Cutscene

The included cutscene uses a PNG sequence at:

```text
assets/revivebydeath/textures/cutscene/heart_grasp_0000.png
```

Current timing defaults:

```json
"enableCutscene": true,
"cutsceneIntroFadeTicks": 4,
"cutsceneTicks": 32,
"cutsceneFrames": 50,
"returnEffectTicks": 14,
"postReviveInvulnerabilityTicks": 60
```

Set `enableCutscene` to `false` to skip the cinematic delay. The rewind logic still works, but the player is restored almost immediately.

During the cinematic, most environment sound categories are muted client-side so weather, mobs, and ambience do not cover the custom cutscene audio. Player sound is not muted, so the initial damage sound can still be heard.

## Commands

Player command:

```mcfunction
/revivebydeath status
/revivebydeath log on
/revivebydeath log off
```

`log on` enables per-player debug messages that explain why rewind did not trigger, such as missing checkpoints, unsafe checkpoints, or failed activation requirements.

OP-only commands:

```mcfunction
/revivebydeath mode always
/revivebydeath mode totem
/revivebydeath mode enchantment
/revivebydeath mode both
/revivebydeath cutscene on
/revivebydeath cutscene off
/revivebydeath give_book
/revivebydeath reset_cost
```

## Configuration

Server config path:

```text
config/revivebydeath.json
```

Recommended default:

```json
{
  "activationMode": "enchantment",
  "replaceVanillaTotem": true,
  "consumeTotem": true,
  "enchantmentArmorDamage": 35,
  "useXpCost": true,
  "minimumXpLevelCost": 6,
  "xpLevelCostIncrease": 2,
  "xpLevelCostMultiplier": 1.18,
  "resetCostOnSleep": true,
  "resetCostOnDeath": true,
  "enableCutscene": true,
  "cutsceneIntroFadeTicks": 4,
  "cutsceneTicks": 32,
  "cutsceneFrames": 50,
  "returnEffectTicks": 14,
  "postReviveInvulnerabilityTicks": 60,
  "checkpointInitialCheckTicks": 10,
  "checkpointRecheckTicks": 6000
}
```

## Test Checklist

- Build with `.\gradlew.bat build`.
- Install the mod on both client and server.
- Test `activationMode` values: `enchantment`, `totem`, `both`, and `always`.
- Test deaths from fall damage, mobs, lava, fire, and void.
- Confirm the player returns to the latest safe checkpoint only when activation and XP cost pass.
- Confirm vanilla death screen appears normally when rewind requirements are not met.
- Confirm `/revivebydeath status` works for players and OP-only commands require permission level 2.
- Confirm `/revivebydeath log on` works for normal players and prints useful failure reasons.
- Test `enableCutscene: false` for players who want immediate rewind.

## Known Notes

- The mod is designed for client and server installation.
- `always` activation mode is intentionally powerful and best kept for testing.
- If the cutscene is disabled, gameplay still works without the visual delay.

## Build

```powershell
.\gradlew.bat build
```

The built jar is written to:

```text
build/libs/revivebydeath-<version>.jar
```

## License

This project is licensed under the MIT License.
