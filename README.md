# Weaponry (Forge 1.16.5)

Forge mod for Minecraft **1.16.5** (Forge 36.2.42, Java 8, official mappings: Mojang method and field names, MCP class names). It adds 22 melee weapons in
5 material tiers, for 110 items in total. The original 10 plus the flail and morning star have their own abilities
(see [Abilities](#abilities)); the other 10 have stats only for now.

Ported from the `fabric/1.16.5` branch, with the Forge layer from `forge/1.18.2`; the gameplay is the same. See [Forge notes](#forge-notes) for how the
Fabric-only parts are done here.

## Side

`displayTest = MATCH_VERSION` and the Minecraft dependency is `BOTH`: Weaponry adds items, so it must be
installed on the server and on every client, with the same version.

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
designs from `pixel-art`. Of these, only the flail and morning star have abilities so far. The hook sword and rope
dart abilities are planned next (`../pixel-art/weaponry/docs/new-weapon-abilities.md`).

Tiers are wooden, stone, iron, golden and diamond. Damage bonus, enchantability, repair item and base
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
| Flail | **Over the Shield:** a full-charge hit on a raised shield isn't blocked; it deals 50% damage, and the shield takes no damage and isn't disabled. **Unwieldy:** hits below 50% charge do nothing. | **Wind-up:** hold use to spin the head. Each 0.5 s adds +1 damage to your next hit (max +3 at 1.5 s). Lost if you don't hit within 2 s of letting go. |
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

The material is planks, stone tool materials, iron ingot, gold ingot or diamond. The wooden club uses a **log**.

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
- `ability/` holds one class per weapon; `ability/Abilities` registers their Forge event listeners (ticks, the sabre
  parry, the two-handed off-hand block).
- `combat/CombatEvents` does the melee hooks (charge capture, crits, damage bonus, armor crush, minimum charge, hits
  through shields, armor wear) through Forge events;
  `combat/AttackContext` keeps the state of the attack in progress; `combat/Motion` tracks player and mount speed.
- `effect/` holds the status effects, `entity/` the thrown weapon, `client/` its renderer and the screen wobble.
- `src/generated/resources` holds the models, lang, recipes, recipe advancements and tags, carried over from the
  `fabric/1.16.5` branch, which carries the 1.18.2 datagen output (vanilla formats). Textures are in `src/main/resources/assets`.

## Testing

`config/weaponry.json` (written on first launch):

```json
{ "testChest": true }
```

When `testChest` is on, every world load places a row of named double chests ("Weaponry Test Chest",
"Weaponry Test Chest 2", ...) starting 2 blocks east of world spawn, holding one of each of the 110 weapons (54 per chest).
Later launches refill those same chests and clear anything else in them. It defaults
to **on in the dev environment** (`runClient`/`runServer`, config in `run/config/`) and **off** in a normally
installed jar, so the code can stay in releases. Weapons have durability, so they stack to 1 and each chest slot holds one weapon.

Commands: `/give @s weaponry:<material>_<weapon>`, e.g. `weaponry:iron_war_hammer`, `weaponry:golden_dagger`.

## Commands

Gradle 8.6 can't run on Java 22 or newer, so start it on JDK 21 (the mod itself is compiled for Java 8 through the
toolchain). In PowerShell: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"`.

```
./gradlew build        # jar in build/libs (reobfuscated)
./gradlew runClient    # play-test
./gradlew runServer
```

`runServer` doesn't pass console input through in ForgeGradle; use RCON (`enable-rcon` in `run/server.properties`)
or op yourself from a client to run commands.

## Forge notes

Differences from the `fabric/1.16.5` branch:

- **No mixins.** Forge fires events at every step of `Player.attack`, so `combat/CombatEvents` replaces the Fabric
  `PlayerMixin`: `AttackEntityEvent` captures the attack charge and sprint, `CriticalHitEvent` denies crits for the
  lance and forces the sabre's riposte crit (a real crit with the vanilla sound and particles), and
  `LivingHurtEvent` adds the ability damage bonus to the attack's target and lowers its armor for armor crush
  (`LivingDamageEvent`, after armor is applied, puts it back). It also replaces the parts of the Fabric
  `LivingEntityMixin` that change the hit itself: `LivingAttackEvent` cancels a flail hit below half charge (vanilla
  then plays the no-damage sound). There is no shield event in this version, so for a full-charge flail hit on a
  raised shield, `LivingAttackEvent` hides the target's `useItem` (public through `accesstransformer.cfg`) from the
  block check, and `LivingHurtEvent` puts it back and cuts the hit to the part that gets through.
- **Armor wear** (morning star). There is no hook on the armor durability call, so a `LivingHurtEvent` listener at
  the lowest priority, which sees the damage vanilla's `Inventory.hurtArmor` gets next, wears each armor piece by
  the difference: the same `max(1, damage / 4)` rule, with the damage multiplied.
- **Damage bonus timing.** The bonus is added in `LivingHurtEvent`, after the charge scaling and crit multiplier,
  so flat bonuses (mounted sabre +2, sprinting spear +1, the lance charge, the flail wind-up) apply in full; proportional ones
  (backstab, pike brace) scale the whole hit.
- **No reach.** Forge 1.16 has no entity reach attribute (only block reach), so melee reach is vanilla's 3
  blocks, as on the Fabric 1.16.5 branch; the halberd hook uses the halberd's listed 4 blocks.
- **Shield breaking** is `IForgeItem.canDisableShield`; **enchanting** is `canApplyAtEnchantingTable` (sword
  enchantments on every weapon, Efficiency/Fortune/Silk Touch on the hatchet), which covers the table, the anvil
  and `/enchant`. The hatchet also answers Forge's `AXE_DIG` tool action.
- **Sabre parry** is `LivingAttackEvent`; **two-handed** blocking is `PlayerInteractEvent.RightClickItem` /
  `RightClickBlock` / `EntityInteract`; ticks are `TickEvent`; the test chest is `ServerStartedEvent`.
- **Registration** goes through `DeferredRegister` (items, entity type, effects), so effects and the entity type are
  `RegistryObject`s resolved with `.get()`. Creative tabs aren't registered before 1.19.3: `ModItems.TAB` is a
  `CreativeModeTab` subclass and items join it through `Item.Properties.tab`. The item attribute map is built on first use, because
  Forge's reach attribute is registered separately.
- **Bleed damage.** Damage types aren't data-driven before 1.19.4, so bleed ticks use
  `new DamageSource("weaponry.bleed").bypassArmor()` (same death message key) and clear the hit cooldown
  (`invulnerableTime`) before they hurt. The main melee hit is recognised by the `"player"` message id. The sabre
  parry treats armor-bypassing damage as unblockable (there is no `#minecraft:bypasses_shield` tag).
- **Client:** renderers register in `EntityRenderersEvent.RegisterRenderers`; the Stagger/Daze wobble runs on
  `TickEvent.ClientTickEvent` and sets the nausea field, which is still `portalTime` here.
- `Entity.level` is a field and `onGround()` is `isOnGround()` in this version.
- **Forge 1.16 names:** events use `getPlayer()` / `getEntityLiving()`, the registries are `ForgeRegistries.ENTITIES`
  and `POTIONS` (effects), `RegistryObject` is in `net.minecraftforge.fml`, the server comes from
  `fml.server.ServerLifecycleHooks`, the test chest listens for `FMLServerStartedEvent`, and renderers register through
  `RenderingRegistry` in `FMLClientSetupEvent`.
- **MCP class names.** On 1.16, ForgeGradle's `official` mappings give methods and fields their Mojang names but keep
  MCP class names (`PlayerEntity`, `World`, `ItemStack`, `EffectInstance`, `RayTraceContext.BlockMode`...). The code was
  carried over from the Mojang-named Fabric branch by mapping each class through its obfuscated name
  (Mojang `client_mappings.txt` to MCPConfig `joined.tsrg`).
- **Java 8:** no `instanceof` patterns, records, switch expressions, `var` or `List.of`, as on the Fabric 1.16.5 branch.
- **Thrown weapons** spawn on the client through `NetworkHooks.getEntitySpawningPacket` (the 1.16 client only builds
  vanilla entity types from the vanilla spawn packet); the Fabric branch's custom spawn packet isn't needed.
- **Hatchet mining** asks a vanilla wooden axe whether it digs a block faster than by hand (there is no
  `#minecraft:mineable/axe` tag before 1.17), as on the Fabric 1.16.5 branch.
- **Dev runs:** each run declares `mods { weaponry { source sourceSets.main } }`, and classes and resources keep
  Gradle's separate output folders. Forge 1.16 takes the first dev mod path as resources and scans only the rest for
  classes, so the shared output folder the newer builds use would leave it with no mod to construct.
- `DamageSource`'s constructor and `bypassArmor()` are protected, so bleed uses a small subclass; text uses
  `TranslatableComponent`.
- **Build:** ForgeGradle 6 from `server-vein-miner`'s `forge/1.20.1`, set to Minecraft 1.16.5, without the shared class/resource output folder (see below). There is no datagen here; the generated JSON
  comes from the Fabric branch (see Layout).
