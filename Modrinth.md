# ReviveByDeath

ReviveByDeath is a Minecraft 1.21.1 mod (available for both **Fabric** and **Forge**) that replaces the normal, sudden death screen with a dramatic, cinematic rewind sequence. When a player suffers fatal damage and meets the configured requirements, time rewinds, a custom cutscene plays, and the player is safely teleported back to their latest safe checkpoint.

This mod is designed from the ground up for balanced survival gameplay, multiplayer servers, and hardcore modpacks, offering deep customization, scaling progression costs, and visual timeline effects.

---

## Features

- **Cinematic Death Rewind**: Interrupts fatal damage with a customized sequence, rendering a client-side cutscene overlays and muting ambient noise.
- **Automatic Checkpoint Tracker**: Continuously checks and saves safe locations for each player.
- **Manual Bed Checkpoints**: Right-clicking a bed at any time of day or night registers an immediate safe checkpoint.
- **Death Rewind Armor Enchantment**: Survival-friendly enchantment that triggers the rewind.
- **Multiple Activation Modes**: Supports `enchantment`, `totem`, `both`, and `always` modes to fit any playstyle.
- **Scaling XP Level Costs**: Penalizes repeated rewinds by increasing the XP level cost, which resets when sleeping or dying normally.
- **Player Shadow**: Spawns a custom Zombie equipped with the player's armor and weapons at the point of death (0% drop rate to prevent item duplication).
- **Dimensional Rift**: Generates cosmetic cracks on the ground upon returning, which expand with repeated deaths and automatically revert to original blocks without harming player-built structures.
- **Post-Revive Protection**: Grants temporary invulnerability after returning to prevent instant death loops.
- **Debugging & OP Tools**: Commands for testing, checking status, viewing debug logs, and resetting costs.

---

## Detailed Mechanics

### 1. Checkpoint System

The server automatically tracks and updates each player's latest safe checkpoint in the background.

#### Checkpoint Saving Criteria:
A location is saved as a checkpoint only if the player is:
- Alive and on the ground.
- Not in lava, fire, or the void.
- Standing on a sturdy, solid, non-liquid block.
- Positioned in open space (feet and head blocks must have clear collision).

#### Saving Triggers:
- **Automatic**: The server performs checks at configurable intervals (default: every 5 minutes/6000 ticks if a checkpoint is active; every 0.5 seconds/10 ticks if looking for the first safe checkpoint).
- **Manual (Beds)**: Right-clicking any bed at any time (day or night) immediately attempts to save a manual checkpoint at the player's safe position, displaying a confirmation message.

---

### 2. Activation Modes

The mod's behavior depends on the configured `activationMode` in `config/revivebydeath.json`:

- **`enchantment`**: Requires at least one equipped piece of armor enchanted with **Death Rewind**.
- **`totem`**: Requires holding a Totem of Undying in either the main hand or off-hand.
- **`both`**: Allows either Death Rewind armor or a totem to trigger the rewind. The mod checks armor first: if the player wears enchanted armor but does not have enough XP levels, it automatically falls back to consuming a totem.
- **`always`**: Rewinds are always active on death, requiring no items or enchantments. Ideal for creative showcases or testing.

---

### 3. Death Rewind Enchantment

- **Type**: Level 1 armor enchantment.
- **Progression**: Tagged as `non_treasure` and `tradeable`, meaning it can be obtained via vanilla enchanting tables, villager trades, loot chests, and fishing.
- **Usage**: Only one equipped armor piece (helmet, chestplate, leggings, or boots) needs to be enchanted for the rewind to activate.

---

### 4. XP Cost Scaling

To balance the safety of the rewind, XP-based modes (`enchantment` and `always`) charge the player XP levels. 
*(Note: Totem-based rewinds consume the totem instead and are exempt from XP checks).*

- **Cost Formula**: The level cost grows with each successful rewind. By default, it starts at 6 levels and scales up based on the number of rewinds used:
  $$\text{XP Level Cost} = (\text{minimumXpLevelCost} + \text{uses} \times \text{xpLevelCostIncrease}) \times \text{xpLevelCostMultiplier}^{\text{uses}}$$
- **Exemptions**: If a player lacks the required XP levels, the rewind fails, and they suffer a normal vanilla death.
- **Resets**: The cumulative rewind count (and therefore the cost) resets back to the minimum when:
  - The player sleeps in a bed (if `resetCostOnSleep` is enabled).
  - The player dies a normal vanilla death (if `resetCostOnDeath` is enabled).
  - An admin runs the `/revivebydeath reset_cost` command.

---

### 5. Player Shadow

To represent the player's "past self" left behind in the timeline, a custom **Zombie** is spawned at the exact coordinates of death when a rewind is triggered:
- **Equipment**: Copies the exact armor and hand items the player held/wore at the moment of death.
- **Properties**: Custom-named `<Player Name>'s Shadow`, and given permanent **Speed II** and **Strength I** effects, and a 10-second **Glowing** effect for easy tracking.
- **Balancing**: Equipment drop rates are set to `0.0F` (0%) to prevent players from duplicating gear by slaying their own shadow. No player XP is retained by the shadow.

---

### 6. Dimensional Rift

When a player teleports back to their checkpoint, a rift in the timeline appears, cracking the floor blocks around them.

#### Block Mapping (Low-Survival-Utility):
To prevent players from obtaining rare resources or XP farms (e.g. from Crying Obsidian or Sculk), the rift uses safe, decorative blocks with very low survival value:
- `Stone Bricks` $\rightarrow$ `Cracked Stone Bricks`
- `Deepslate Bricks` $\rightarrow$ `Cracked Deepslate Bricks`
- `Deepslate Tiles` $\rightarrow$ `Cracked Deepslate Tiles`
- `Nether Bricks` $\rightarrow$ `Cracked Nether Bricks`
- `Polished Blackstone Bricks` $\rightarrow$ `Cracked Polished Blackstone Bricks`
- `Logs / Planks` (Wood) $\rightarrow$ `Basalt` (charred log appearance)
- `Stone / Cobblestone` $\rightarrow$ `Tuff`
- `Grass / Mycelium / Dirt` $\rightarrow$ `Coarse Dirt`
- `Sand` $\rightarrow$ `Gravel`
- Others $\rightarrow$ `Tuff`

#### Gradual Expansion:
The rift size and block density scale with the number of deaths at that checkpoint:
- **1st Revive**: Radius 1 block, 20% block replacement chance.
- **2nd Revive**: Radius 1 block, 35% block replacement chance.
- **3rd Revive**: Radius 2 blocks, 50% block replacement chance.
- **4th Revive**: Radius 2 blocks, 65% block replacement chance.
- **5th+ Revives**: Radius 3 blocks, 70% block replacement chance.

#### Smart Reversion & Visual Effects:
- **Automatic Cleanup**: When the checkpoint resets (sleeping in a bed, setting a new checkpoint location, dying a true death, or via admin commands), the rift is cleared.
- **Conflict Prevention**: The mod stores both the original block state and the placed block. It only reverts a block if it is still the rift block placed by the mod. If the player broke or modified the block, the mod skips it to preserve player edits.
- **Effects**: Crumbling particles (`PORTAL`) and sound (`DEEPSLATE_BREAK`) play when cracks appear. Sparkling magic particles (`WITCH`) and chime sounds (`AMETHYST_BLOCK_CHIME`) play when they revert.

---

## Configuration Reference

The configuration file is located at `config/revivebydeath.json` on the server/client:

| Key | Default Value | Description |
| :--- | :--- | :--- |
| `activationMode` | `"enchantment"` | Requirements to trigger rewind: `"enchantment"`, `"totem"`, `"both"`, or `"always"`. |
| `replaceVanillaTotem` | `true` | If true, replaces vanilla Totem of Undying mechanics with the cinematic rewind. |
| `consumeTotem` | `true` | Whether the Totem of Undying is consumed upon triggering a rewind. |
| `enchantmentArmorDamage` | `35` | Durability damage dealt to armor containing the Death Rewind enchantment. |
| `useXpCost` | `true` | If true, applies scaling XP level costs to non-totem rewinds. |
| `minimumXpLevelCost` | `6` | The base XP level cost for the first rewind. |
| `xpLevelCostIncrease` | `2` | Linear increase in level cost per rewind. |
| `xpLevelCostMultiplier` | `1.18` | Exponential multiplier scaling the level cost per rewind. |
| `resetCostOnSleep` | `true` | If true, sleeping in a bed resets the rewind counter and cost to the minimum. |
| `resetCostOnDeath` | `true` | If true, suffering a normal vanilla death resets the rewind counter and cost. |
| `enableCutscene` | `true` | Toggles the client-side cinematic rewind overlays and visual delay. |
| `cutsceneIntroFadeTicks`| `4` | Ticks for the screen to fade to red/black at the start of the sequence. |
| `cutsceneTicks` | `32` | Duration of the core PNG cutscene loop. |
| `cutsceneFrames` | `50` | Total number of frames in the custom PNG overlay sequence. |
| `returnEffectTicks` | `14` | Duration of the overlay fadeout after returning to the checkpoint. |
| `postReviveInvulnerabilityTicks` | `60` | Invulnerability duration (in ticks, 20 ticks = 1 second) upon returning. |
| `checkpointInitialCheckTicks` | `10` | Frequency of checks (in ticks) when searching for a player's first checkpoint. |
| `checkpointRecheckTicks` | `6000` | Frequency of checks (in ticks) when updating an active checkpoint. |
| `spawnPastShadow` | `true` | Toggles spawning the Zombie equipped with player gear at the point of death. |
| `createDimensionalRift` | `true` | Toggles ground-cracking and reversion around checkpoints. |

---

## Commands

### Player Commands
- `/revivebydeath status`: Displays the player's current checkpoint coordinates, active dimensions, and the next rewind cost in XP levels.
- `/revivebydeath log on`: Enables debug messages showing reasons why a rewind fails (e.g. unsafe ground, no checkpoint, insufficient XP).
- `/revivebydeath log off`: Disables debug messages.

### Operator Commands (Permission Level 2)
- `/revivebydeath mode <always|totem|enchantment|both>`: Dynamically overrides the activation mode.
- `/revivebydeath cutscene <on|off>`: Toggles the cutscene playback globally.
- `/revivebydeath give_book`: Gives the executing player an enchanted book with the Death Rewind enchantment.
- `/revivebydeath reset_cost [player]`: Resets the rewind counter and XP cost for the target player.

---

## Mod Verification & Installation

ReviveByDeath is fully verified and packaged for Minecraft 1.21.1 on both Fabric and Forge loaders. It must be installed on **both client and server** to ensure proper cutscene sync, overlays, custom sounds, and event handling.
