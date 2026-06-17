# ReviveByDeath

ReviveByDeath is a Minecraft 1.21.1 mod available for both **Fabric** and **Forge** that replaces fatal moments with a short, cinematic rewind sequence, restoring the player to their latest safe checkpoint.

All visual and audio assets in this project are original placeholders/custom assets. Do not add copyrighted anime, game, movie, logo, quote, or sound content unless you have explicit rights to use and monetize it.

---

## Requirements

- **Minecraft**: `1.21.1`
- **Fabric Version**: Requires Fabric Loader `0.19.3` or newer and Fabric API.
- **Forge Version**: Requires Forge `52.1.0` or newer.
- **Java**: `21`

ReviveByDeath is required on both client and server.

---

## Features

### 1. Cinematic death rewind cutscene
If the player meets the revive requirements, death is interrupted, a short cutscene plays, and the player returns to their latest safe checkpoint.

### 2. Player Shadow
When the rewind sequence starts, a custom Zombie representing the player's shadow spawns at the death location:
- Equipped with the exact armor and weapons the player was holding/wearing.
- Custom-named `<Player>'s Shadow` with permanent Speed II, Strength I, and Glowing for 10 seconds.
- Drop rate set to `0.0F` to prevent duplication of gear.
- Can be toggled on/off in the configuration (`spawnPastShadow`).

### 3. Dimensional Rift
When returning to the checkpoint, a cosmic rift/timeline fracture cracks the floor blocks around it:
- **Low Survival Utility**: Avoids high-value blocks like Crying Obsidian or Sculk. Replaces common floor blocks with low-utility equivalents:
  - `Stone Bricks` -> `Cracked Stone Bricks`
  - `Deepslate Bricks` -> `Cracked Deepslate Bricks`
  - `Deepslate Tiles` -> `Cracked Deepslate Tiles`
  - `Nether Bricks` -> `Cracked Nether Bricks`
  - `Polished Blackstone Bricks` -> `Cracked Polished Blackstone Bricks`
  - `Logs / Planks` (Wood) -> `Basalt` (charred log appearance)
  - `Stone / Cobblestone` -> `Tuff`
  - `Grass / Dirt` -> `Coarse Dirt`
  - `Sand` -> `Gravel`
  - Fallback -> `Tuff`
- **Gradual Expansion**: The rift grows in radius and density if the player repeatedly dies and returns to the same active checkpoint.
- **Smart Reversion & No Conflict**: When the checkpoint is reset (e.g. setting a new checkpoint, sleeping in a bed, true vanilla death, or admin command), all rift blocks automatically revert back to their original states. If a player has broken or modified a block in the meantime, the mod is smart enough to leave it alone.
- Includes visual Portal particles/crumbling sound on crack placement, and Witch particles/clock chime sound right before reversion.
- Can be toggled on/off in the configuration (`createDimensionalRift`).

---

## Activation Modes

`activationMode` controls what is required to trigger a rewind.

- `enchantment`: Requires at least one equipped armor piece with Death Rewind. This is the default survival-friendly mode.
- `totem`: Requires a Totem of Undying in either hand. If `replaceVanillaTotem` is true, the mod replaces the vanilla totem behavior with the cinematic rewind.
- `both`: Allows either Death Rewind armor or a totem. Armor enchantment is checked first, so a player wearing Death Rewind will not consume a totem.
- `always`: No item requirement. Useful for testing or showcases, not recommended for balanced survival.

---

## Death Rewind Enchantment

Death Rewind is a level 1 armor enchantment available through vanilla-style progression:
- Enchanting table, because it is tagged as `minecraft:non_treasure`.
- Villager trading, because it is tagged as `minecraft:tradeable`.
- OP test command: `/revivebydeath give_book`.

Only one equipped armor piece needs the enchantment to activate rewind.

---

## Checkpoints

The server automatically tracks each player's latest safe checkpoint. A checkpoint is saved only when the player is:
- alive;
- on the ground;
- not in lava, fire, or the void;
- standing on a sturdy non-liquid block;
- in enough open space for feet and head.

Players can also right-click a bed during day or night to manually save a checkpoint.

---

## XP Cost

When `useXpCost` is true, enchantment and `always` rewinds cost XP levels. Totem rewinds consume the totem instead, so they can still work when the player does not have enough XP.

The cost resets when the player sleeps if `resetCostOnSleep` is true. It also resets after a normal death if `resetCostOnDeath` is true.

---

## Configuration

Server config path: `config/revivebydeath.json`

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
  "checkpointRecheckTicks": 6000,
  "spawnPastShadow": true,
  "createDimensionalRift": true
}
```

---

## Commands

Player commands:
```mcfunction
/revivebydeath status
/revivebydeath log on
/revivebydeath log off
```

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

---

## Build & Project Structure

The project is split into two modules:
- `/fabric`: The Fabric loader implementation.
- `/forge`: The Forge loader implementation.

To compile each version, go into its directory and run the gradle wrapper:

For Fabric:
```powershell
cd fabric
.\gradlew.bat clean build
```

For Forge:
```powershell
cd forge
.\gradlew.bat clean build
```

The compiled jars are located under `<module>/build/libs/`.

---

## License

This project is licensed under the MIT License.
