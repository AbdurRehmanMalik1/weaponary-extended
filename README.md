# Weaponry (NeoForge 1.21.1)

NeoForge mod for Minecraft **1.21.1** (NeoForge 21.1.238, Java 21). It adds 22 melee weapons, 21 of them in
5 material tiers. The rope dart is a utility weapon, so it comes in one version only (`weaponry:rope_dart`, iron).
That makes 106 items in total. The original 10 plus the rapier, war pick, flail, morning star, hook sword and rope dart
have their own abilities (see [Abilities](#abilities)); the other 6 have stats only for now.

Ported from the `fabric/1.21.1` branch; the gameplay is the same. See [NeoForge notes](#neoforge-notes) for how the
Fabric-only parts are done here.

## Side

`neoforge_minecraft_side=BOTH`: Weaponry adds items, so it must be installed on the server and on every client.

## Weapons

| Weapon | Dmg vs sword | Speed | Reach | Durability × | Two-handed* |
|---|---:|---:|---:|---:|---|
| Dagger | −2 | 2.4 | 2.5 | 0.75 | |
| Sabre | −1 | 2.0 | 3.0 | 1.0 | |
| Scimitar | 0 | 1.6 | 3.0 | 1.0 | yes |
| Spear | −1 | 1.4 | 4.0 | 1.0 | |
| Pike | 0 | 1.0 | 5.0 | 1.2 | yes |
| Lance | −2 | 0.8 | 4.5 | 0.6 | yes |
| Halberd | +2 | 0.9 | 4.0 | 1.2 | yes |
| Hatchet | +1 | 1.1 | 2.75 | 1.0 | |
| War Hammer | +3 | 0.7 | 3.0 | 1.5 | yes |
| Club | −1 | 1.2 | 3.0 | 1.5 | |
| Rapier | −1 | 2.0 | 3.25 | 0.8 | |
| Hook Sword | −1 | 1.8 | 3.0 | 1.0 | |
| Greatsword | +3 | 0.9 | 3.5 | 1.3 | yes |
| Battle Axe | +4 | 0.8 | 3.0 | 1.2 | yes |
| War Pick | +2 | 0.9 | 3.0 | 1.25 | |
| Glaive | +1 | 1.0 | 4.5 | 1.2 | yes |
| Scythe | +2 | 0.8 | 4.0 | 1.0 | yes |
| Mace | +1 | 1.1 | 3.0 | 1.5 | |
| Morning Star | +2 | 1.0 | 3.0 | 1.25 | |
| Flail | +3 | 0.8 | 3.0 | 1.0 | |
| Gauntlets | −3 | 2.6 | 2.5 | 1.0 | |
| Rope Dart | −2 | 1.6 | 4.5 | 0.75 | |

The last 12 (rapier to rope dart) are newer: their stats are a first pass and their textures are the tentative
designs from `pixel-art`. Of these, the rapier, war pick, flail, morning star, hook sword and rope dart have abilities so far
(`../pixel-art/weaponry/docs/new-weapon-abilities.md`).

Tiers are wooden, stone, iron, golden and diamond (the rope dart is iron only; see `WeaponType.materials()`). Damage bonus, enchantability, repair item and base
durability all come from the vanilla tier. Source: `../pixel-art/weaponry/docs/weapon-stats.md`.

\* Needs both hands. While it's in the main hand, whatever is in the off hand (a shield, a torch...) is moved
into your inventory, and a two-handed weapon put in the off hand is moved out too. The action bar says what moved.
If the inventory is full, the item stays in the off hand but is blocked: no shield, no dual wielding, no off-hand
item use, and a raised shield is lowered.

## Abilities

From `../pixel-art/weaponry/docs/weapon-stats.md` section 4. "Full charge" means the attack cooldown is over 90%,
the same threshold vanilla uses for sweeps and crits.

| Weapon | Passive | Active |
|---|---|---|
| Dagger | **Backstab:** ×1.5 damage when hitting a target from behind (within 60° of straight behind it). Critical hits cause **Bleed**. | **Throw:** like the spear. Melee damage +1, and it causes **Bleed**. |
| Sabre | +2 damage while mounted. | **Riposte:** use to parry for 0.5 s (1 s cooldown). Parrying a frontal melee hit cancels it, and your next hit within 3 s is a guaranteed crit. |
| Scimitar | **Draw Cut:** full-charge, grounded, non-sprinting hits sweep, with about 1.5× the vanilla sweep area. Everything the sweep hits, the main target included, gets **Bleed**. | |
| Spear | Reach 4. +1 damage on a sprinting hit. | **Throw:** hold use (at least 0.5 s) and release. Melee damage +2. It drops where it lands; pick it up again. |
| Pike | Reach 5. Half damage against targets under 1.5 blocks away. | **Brace:** sneak and stand still. Hits on targets charging at you (2+ blocks/s, on foot or mounted) do double damage and dismount riders. |
| Lance | No critical hits. | **Couched Charge:** hit while riding a mount moving at 4+ blocks/s. Bonus damage = 0.8 × mount speed (blocks/s), max +15, plus heavy knockback and the target is dismounted. Costs 3 durability. |
| Halberd | Reach 4. Disables shields like an axe. | **Hook:** use on a target in reach (2 s cooldown, 1 durability). Pulls it toward you, pulls it off its mount, and knocks a raised shield aside. |
| Hatchet | Disables shields. Mines wood at axe speed and takes Efficiency, Fortune and Silk Touch. | **Throw:** like the spear, but short range and spinning. Melee damage +1. |
| War Hammer | **Armor Crush:** ignores 50% of the target's armor. Full-charge hits cause **Stagger** for 1 s. | **Overhead Smash:** a full-charge hit while sneaking knocks back everything within 2.5 blocks of the target, and everything it hits (target included) gets **Stun** for 0.75 s. |
| Club | +1 knockback. **Daze:** full-charge hits have a 20% chance to cause **Daze** for 2 s. | |
| Rapier | | **Thrust:** use on a target up to 1 block past your reach (4.25 blocks) for full damage +2, whatever your attack charge (2.5 s cooldown, 1 durability). Resets your attack charge. |
| War Pick | | **Flip:** use to turn it round (0.5 s cooldown). Flipped, it mines like a pickaxe of its tier at 80% speed, 1 durability per block, and takes Efficiency, Fortune and Silk Touch. Use again to flip it back. It still fights the same either way. |
| Flail | **Over the Shield:** a full-charge hit on a raised shield isn't blocked; it deals 50% damage, and the shield takes no damage and isn't disabled. **Unwieldy:** hits below 50% charge do nothing. | **Wind-up:** hold use to spin the head. Each 0.5 s adds +1 damage to your next hit (max +3 at 1.5 s). Lost if you don't hit within 2 s of letting go. |
| Hook Sword | | **Hook Pull:** use on a target in reach to pull it toward you (1.5 s cooldown, 1 durability). **Hook Leap:** use twice on the same target within 0.3 s to pull yourself toward it, stopping about 1 block short (3 s cooldown, 2 durability). A single use waits out that 0.3 s before pulling. **Wall Brake:** hold use while falling with a block right beside you to hook into it. It slows the fall gradually (0.12 blocks/tick per tick) down to a 4 blocks/s slide, and fall damage follows your speed: from high up you land unhurt, just above the ground you still take most of it. 1 durability per 0.5 s. |
| Rope Dart | Never dropped: the dart is tied to the rope dart in your hand. Use again to reel it in; sneak + use lets go in any state. Switching items or the rope running out (12 blocks) also reels it in. | **Throw:** hold use (at least 0.5 s) and release; melee damage −1. **Tether:** hitting a living target leashes it for 5 s, holding it within 12 blocks; hold use to drag it in (1 durability per 0.5 s). Iron golems, ravagers and wardens are too heavy, so dragging pulls you to them; the wither and ender dragon shrug the dart off. A tethered player breaks free by hitting you, or when you hit them for 4+; past 16 blocks the rope snaps. **Grapple:** hitting a block anchors the rope (1 durability). You swing on it; hold jump to climb, sneak to lower. No fall protection: climbing up starts a fresh fall like a jump does, but a drop the rope catches still counts when you land. |
| Morning Star | **Puncture:** full-charge hits cause **Bleed**. **Mail-breaker:** hits cost the target's armor twice the durability (only players' armor wears in vanilla). | |

Material twists:

- **Wooden:** half the armor crush; Bleed lasts 2 s.
- **Stone:** +1 knockback on the club and war hammer.
- **Golden:** ability numbers × 1.5 (bonus damage, effect durations, daze chance, hook pull, smash knockback, armor crush,
  flail shield pass-through 75% and wind-up +1.5 per step, morning star armor wear ×2.5).
- **Diamond:** +1 throw damage; Bleed lasts 6 s.

Status effects (`effect/ModEffects`), each with its own icon:

| Effect | Does | From |
|---|---|---|
| **Bleed** | 1 damage per second per stack for 4 s, ignoring armor and hit cooldown. Each new application adds a stack (max 3, shown as Bleed II/III) and refreshes the duration. Undead and golems are immune. | Dagger crits and throws, scimitar sweeps, morning star |
| **Stagger** | A short, sharp screen lurch for a staggered player. No slowdown. | War hammer full-charge hits |
| **Stun** | 90% slower, nearly rooted. | War hammer smash |
| **Daze** | 15% slower (like Slowness I), and a dazed player's view wobbles like Nausea. | Club |

The icons are drawn in `../pixel-art/weaponry/designs/effects/` (16×16). `python scripts/copy_effect_icons.py`
copies them to `textures/mob_effect/` with a 1 px transparent border, because the game draws effect icons at 18×18.
Bleed damage uses the `weaponry:bleed` damage type (`data/weaponry/damage_type/`), tagged to bypass armor and
hit cooldown.

Also not done yet: the lance's "−1 speed tier on foot" (it is already the slowest weapon), and the brace doesn't lock movement.

## Recipes (M = material, S = stick, C = chain, X = string, L = leather)

The material is planks, stone tool materials, iron ingot, gold ingot or diamond. The wooden club uses a **log**, and the
rope dart (iron only) always takes an iron ingot.

```
Dagger   Sabre    Scimitar  Spear    Pike     Lance    Halberd  Hatchet  War Hammer  Club
M        ..M      .MM       ..M      .MM      ..M      MMM      MM       MMM         .M
S        .M.      .M.       .S.      .SM      MM.      .S.      MS       MSM         S.
         S..      S..       S..      S..      S..      S..               .S.

Rapier   Hook Sword  Greatsword  Battle Axe  War Pick  Glaive  Scythe  Mace  Morning Star  Flail  Gauntlets  Rope Dart
..M      MM          .M.         M.M         MMM       ..M     MMM     .MM   M.M           ..M    MMM        ..M
.M.      M.          MMM         MSM         MS.       .SM     M.S     .MM   .M.           .C.    .L.        .X.
MS.      S.          .S.         .S.         .S.       S..     ..S     S..   S..           S..               X..
```

## Layout

- `item/WeaponType` holds the per-weapon stats and abilities, `item/WeaponMaterial` the tiers. `item/WeaponItem` is the
  single item class; `item/ModItems` registers the items and the creative tab through `DeferredRegister`.
- `ability/` holds one class per weapon; `ability/Abilities` registers their NeoForge event listeners.
- The mixins are the same as on Fabric (`weaponry.mixins.json`, declared in `neoforge.mods.toml`): `mixin/PlayerMixin`
  captures the attack charge, turns off crits for the lance, cancels hits below a weapon's minimum charge (flail) and
  applies armor crush; `mixin/LivingEntityMixin` lets the flail hit through shields and makes the morning star wear armor;
  `mixin/LivingEntityAccessor` reads whether the jump key is held, for climbing the rope dart's rope.
- `effect/` holds the status effects, `entity/` the thrown weapon and the rope dart's dart, `client/` their renderers
  and the screen wobble (`WeaponryClient` is the client-only `@Mod` entry point). The rope's pull on its own thrower
  runs on that player's client, like all player movement.
- `src/generated/resources` holds the models, lang, recipes, advancements and tags: the `fabric/1.21.1` branch's
  datagen output (vanilla formats). Textures and the bleed damage type are in `src/main/resources`.

## Testing

`config/weaponry.json` (written on first launch):

```json
{ "testChest": true }
```

When `testChest` is on, every world load places a row of named double chests ("Weaponry Test Chest",
"Weaponry Test Chest 2", ...) starting 2 blocks east of world spawn, holding one of each of the 106 weapons (54 per chest).
Later launches refill those same chests and clear anything else in them. It defaults
to **on in the dev environment** (`runClient`/`runServer`, config in `run/config/`) and **off** in a normally
installed jar, so the code can stay in releases. Weapons have durability, so they stack to 1 and each chest slot holds one weapon.

Commands: `/give @s weaponry:<material>_<weapon>`, e.g. `weaponry:iron_war_hammer`, `weaponry:golden_dagger`.

## Commands

```
./gradlew build        # jar in build/libs
./gradlew runClient    # play-test
./gradlew runServer
```

`runServer` doesn't pass console input through; use RCON (`enable-rcon` in `run/server.properties`) or op yourself from
a client to run commands.

## NeoForge notes

Differences from the `fabric/1.21.1` branch:

- **Same mixins.** NeoForge runs Minecraft with Mojang names, so `PlayerMixin` applies unchanged; NeoForge ships
  MixinExtras. The config is declared with `[[mixins]]` in `neoforge.mods.toml`.
- **Registration** goes through `DeferredRegister` (items via `DeferredRegister.Items.registerItem`, which also sets
  the item id, the creative tab, the entity type and the effects). Effects are `DeferredHolder`s, which are `Holder`s,
  so effect code is unchanged; the entity type is resolved with `.get()`.
- **Events:** the server tick is `ServerTickEvent.Post`, the sabre parry and the rope dart's PvP way out are
  `LivingIncomingDamageEvent`, two-handed blocking is `PlayerInteractEvent.RightClickItem` / `RightClickBlock` /
  `EntityInteract`, and the test chest listens for `ServerStartedEvent`.
- **Client:** `WeaponryClient` registers the renderers in `EntityRenderersEvent.RegisterRenderers` and runs the
  Stagger/Daze wobble on `ClientTickEvent.Post`.
- **War pick flip:** the `weaponry:flipped` data component is registered through `DeferredRegister.DataComponents`,
  and the client registers the `weaponry:flipped` model property in `FMLClientSetupEvent`. Mining runs on the vanilla
  tool component, as on Fabric.
- **Config:** `config/weaponry.json` lives in `FMLPaths.CONFIGDIR`; the test chest defaults to on outside production.
- **Build:** ModDevGradle from `server-vein-miner`'s NeoForge branch. There is no datagen; the generated JSON comes from
  the Fabric branch (see Layout).
- **Shield breaking** uses NeoForge's `IItemExtension.canDisableShield` (vanilla `LivingEntity.canDisableShield` asks the item), so `LivingEntityMixin` leaves out the Fabric shield-breaker hook. Its flail hook wraps the `isDamageSourceBlocked` call that NeoForge passes to `LivingShieldBlockEvent`, so a hit over the shield is simply not blocked.

