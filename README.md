# Weaponry (Forge 1.19.2)

Forge mod for Minecraft **1.19.2** (Forge 43.5.2, Java 17, official Mojang mappings). It adds 22 melee weapons, 21 of them in
5 material tiers. The rope dart is a utility weapon, so it comes in one version only (`weaponry:rope_dart`, iron).
That makes 106 weapon items, plus four bows (see [Bows](#bows)). The original 10 plus the rapier, war pick, flail, morning star, hook sword and rope dart
have their own abilities (see [Abilities](#abilities)); the other 6 have stats only for now.

Ported from the `fabric/1.19.2` branch (through `forge/1.20.1`); the gameplay is the same. See [Forge notes](#forge-notes) for how the
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

Each ability's name (in bold) is also the first word of its in-game tooltip line, so players can refer to it by name.

| Weapon | Passive | Active |
|---|---|---|
| Dagger | **Backstab:** ×1.5 damage when hitting a target from behind (within 60° of straight behind it). **Lacerate:** critical hits cause **Bleed**. | **Throw:** like the spear. Melee damage +1, and it causes **Bleed**. |
| Sabre | **Cavalry:** +2 damage while mounted. | **Riposte:** use to parry for 0.5 s (1 s cooldown). Parrying a frontal melee hit cancels it, and your next hit within 3 s is a guaranteed crit. |
| Scimitar | **Draw Cut:** full-charge, grounded, non-sprinting hits sweep, with about 1.5× the vanilla sweep area. Everything the sweep hits, the main target included, gets **Bleed**. | |
| Spear | Reach 4. **Charge:** +1 damage on a sprinting hit. | **Throw:** hold use (at least 0.5 s) and release. Melee damage +2. It drops where it lands; pick it up again. |
| Pike | Reach 5. **Too Close:** half damage against targets under 1.5 blocks away. | **Brace:** sneak and stand still. Hits on targets charging at you (2+ blocks/s, on foot or mounted) do double damage and dismount riders. |
| Lance | **Stiff Grip:** no critical hits. | **Couched Charge:** hit while riding a mount moving at 4+ blocks/s. Bonus damage = 0.8 × mount speed (blocks/s), max +15, plus heavy knockback and the target is dismounted. Costs 3 durability. |
| Halberd | Reach 4. **Shield Breaker:** disables shields like an axe. | **Hook:** use on a target in reach (2 s cooldown, 1 durability). Pulls it toward you, pulls it off its mount, and knocks a raised shield aside. |
| Hatchet | **Shield Breaker:** disables shields. Mines wood at axe speed and takes Efficiency, Fortune and Silk Touch. | **Throw:** like the spear, but short range and spinning. Melee damage +1. |
| War Hammer | **Armor Crush:** ignores 50% of the target's armor. **Stagger:** full-charge hits cause **Stagger** for 1 s. | **Overhead Smash:** a full-charge hit while sneaking knocks back everything within 2.5 blocks of the target, and everything it hits (target included) gets **Stun** for 0.75 s. |
| Club | +1 knockback. **Daze:** full-charge hits have a 20% chance to cause **Daze** for 2 s. | |
| Rapier | | **Thrust:** use on a target up to 1 block past your reach (4.25 blocks) for full damage +2, whatever your attack charge (2.5 s cooldown, 1 durability). Resets your attack charge. |
| War Pick | | **Flip:** use to turn it round (0.5 s cooldown). Flipped, it mines like a pickaxe of its tier at 80% speed, 1 durability per block, and takes Efficiency, Fortune and Silk Touch. Use again to flip it back. It still fights the same either way. |
| Flail | **Over the Shield:** a full-charge hit on a raised shield isn't blocked; it deals 50% damage, and the shield takes no damage and isn't disabled. **Unwieldy:** hits below 50% charge do nothing. | **Wind-up:** hold use to spin the head. Each 0.5 s adds +1 damage to your next hit (max +3 at 1.5 s). Lost if you don't hit within 2 s of letting go. |
| Hook Sword | | **Hook Pull:** use on a target in reach to pull it toward you (1.5 s cooldown, 1 durability). **Hook Leap:** use twice on the same target within 0.3 s to pull yourself toward it, stopping about 1 block short (3 s cooldown, 2 durability). A single use waits out that 0.3 s before pulling. **Wall Brake:** hold use while falling with a block right beside you to hook into it. It slows the fall gradually (0.12 blocks/tick per tick) down to a 4 blocks/s slide, and fall damage follows your speed: from high up you land unhurt, just above the ground you still take most of it. 1 durability per 0.5 s. |
| Rope Dart | Never dropped: the dart is tied to the rope dart in your hand. **Reel In:** use again to reel it in; sneak + use lets go in any state. Switching items or the rope running out (12 blocks) also reels it in. | **Throw:** hold use (at least 0.5 s) and release; melee damage −1. **Tether:** hitting a living target leashes it for 5 s, holding it within 12 blocks; hold use to drag it in (1 durability per 0.5 s). Iron golems, ravagers and wardens are too heavy, so dragging pulls you to them; the wither and ender dragon shrug the dart off. A tethered player breaks free by hitting you, or when you hit them for 4+; past 16 blocks the rope snaps. **Grapple:** hitting a block anchors the rope (1 durability). You swing on it; hold jump to climb, sneak to lower. No fall protection: climbing up starts a fresh fall like a jump does, but a drop the rope catches still counts when you land. |
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

## Bows

Four bows, one version each (no material tiers): `weaponry:selfbow`, `longbow`, `recurve_bow` and `horse_bow`.
They shoot vanilla arrows and take the same enchantments as vanilla's bow. Each draws over its own draw time, with
the vanilla power curve stretched to fit, and has its own three-frame draw animation and zoom. Source:
`../pixel-art/weaponry/docs/bow-stats.md` (stats §3, abilities §5, recipes §6).

| Bow | Draw | Full damage | Spread | Durability | Abilities |
|---|---:|---:|---:|---:|---|
| *Vanilla bow* | *1.0 s* | *6* | *1.0* | *384* | |
| Selfbow | 0.8 s | 5 | 1.25 | 256 | **Light Draw:** walk at 50% speed while drawing (vanilla 20%). **Rough Arrows:** 15% of its arrows can't be picked up again. |
| Longbow | 1.5 s | 8 | 0.6 | 384 | **Steady Aim:** after 1 s at full draw, spread 0.1 and extra zoom. **Arm Fatigue:** after 5 s at full draw, spread grows 0.25/s up to 1.5 until you draw again. **No Horseback:** can't be drawn while riding. |
| Recurve Bow | 1.0 s | 7 | 0.9 | 432 | **Quick Nock:** drawing again within 1 s of a shot starts 25% drawn. **Snap Shot:** +1 damage if released within 0.25 s of full draw. **Dry-fire Strain:** shots below half draw cost 2 durability. |
| Horse Bow | 0.6 s | 5 | 1.5 | 336 | **Horse Archer:** while riding, a 25% faster draw (0.45 s) and +1 damage. **Light Arrows:** half knockback and half Punch. **Spread While Moving:** spread x1.2 when shooting on the move on foot. |

Arrow speed is x0.9 / x1.3 / x1.15 / x1 vanilla's, which sets range (about 99 / 178 / 147 / 117 blocks). Damage is
`ceil(arrow speed x base damage)` as in vanilla; flat bonuses (Snap Shot, Horse Archer) are added to base damage as
`bonus / arrow speed`, so they come out as about +1.

Recipes (S = stick, X = string, L = leather, I = iron ingot, G = gold ingot), all in vanilla's bow layout:

```
Selfbow   Longbow   Recurve Bow   Horse Bow
.SX       SSX       .SX           GSX
S..       L.X       I.X           S.X
.SX       SSX       .SX           GSX
```

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
- `ability/` holds one class per weapon; `ability/Abilities` registers their Forge event listeners (ticks, the sabre
  parry, the two-handed off-hand block).
- `combat/CombatEvents` does the melee hooks (charge capture, crits, damage bonus, armor crush, minimum charge, hits
  through shields, armor wear) through Forge events;
  `combat/AttackContext` keeps the state of the attack in progress; `combat/Motion` tracks player and mount speed.
- `effect/` holds the status effects, `entity/` the thrown weapon and the rope dart's dart, `client/` their renderers
  and the screen wobble. The rope's pull on its own thrower runs on that player's client, like all player movement.
- `bow/` holds the bows: `BowType` (stats), `WeaponBowItem` (draw and shoot, extends vanilla's `BowItem`),
  `BowAbility` and one ability class per bow, `BowState` (Quick Nock timing). `ModBows` registers them. The client
  (`client/WeaponryClient`) registers each bow's `pull`/`pulling` properties against its own draw time, and sets the
  draw zoom and the walk speed while drawing. Horse bow arrows carry the `weaponry.light_arrow` entity tag, which
  `combat/CombatEvents` uses to halve their knockback.
- `src/generated/resources` holds the models, lang, recipes, recipe advancements and tags, carried over from the
  `fabric/1.19.2` branch's datagen output (vanilla formats). Textures are in `src/main/resources/assets`.

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

Gradle 8.6 can't run on Java 22 or newer, so start it on JDK 21 (the mod itself is compiled for Java 17 through the
toolchain). In PowerShell: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"`.

```
./gradlew build        # jar in build/libs (reobfuscated)
./gradlew runClient    # play-test
./gradlew runServer
```

`runServer` doesn't pass console input through in ForgeGradle; use RCON (`enable-rcon` in `run/server.properties`)
or op yourself from a client to run commands.

## Forge notes

Differences from the `fabric/1.19.2` branch:

- **No mixins.** Forge fires events at every step of `Player.attack`, so `combat/CombatEvents` replaces the Fabric
  `PlayerMixin`: `AttackEntityEvent` captures the attack charge and sprint, `CriticalHitEvent` denies crits for the
  lance and forces the sabre's riposte crit (a real crit with the vanilla sound and particles), and
  `LivingHurtEvent` adds the ability damage bonus to the attack's target and lowers its armor for armor crush
  (`LivingDamageEvent`, after armor is applied, puts it back). It also replaces the parts of the Fabric
  `LivingEntityMixin` that change the hit itself: `LivingAttackEvent` cancels a flail hit below half charge (vanilla
  then plays the no-damage sound), and cancelling `ShieldBlockEvent` lets a full-charge flail hit past a raised
  shield, with `LivingHurtEvent` cutting it to the part that gets through.
- **Armor wear** (morning star). There is no hook on the armor durability call, so a `LivingHurtEvent` listener at
  the lowest priority, which sees the damage vanilla's `Inventory.hurtArmor` gets next, wears each armor piece by
  the difference: the same `max(1, damage / 4)` rule, with the damage multiplied.
- **Damage bonus timing.** The bonus is added in `LivingHurtEvent`, after the charge scaling and crit multiplier,
  so flat bonuses (mounted sabre +2, sprinting spear +1, the lance charge, the flail wind-up) apply in full; proportional ones
  (backstab, pike brace) scale the whole hit.
- **Reach works.** Forge 1.19.2 has an entity reach attribute (`forge:attack_range`, read with
  `getAttackRange()`), so weapons get their listed reach (the Fabric 1.19.2 branch can't). Abilities that reach for a target (the halberd hook, the hook sword pull,
  `Abilities.targetInReach`) use the player's reach too, as 1.21.1 does.
- **Shield breaking** is `IForgeItem.canDisableShield`; **enchanting** is `canApplyAtEnchantingTable` (sword
  enchantments on every weapon, Efficiency/Fortune/Silk Touch on the hatchet and war pick), which covers the table,
  the anvil and `/enchant`. The hatchet also answers Forge's `AXE_DIG` tool action.
- **War pick flip.** The flipped state is a `weaponry:flipped` boolean in the stack's NBT (there are no item
  components), and the client registers the `weaponry:flipped` model property from it in `FMLClientSetupEvent`.
  Flipped, `getDestroySpeed` uses `#minecraft:mineable/pickaxe` at the tier's speed ×0.8, Forge's stack-aware
  `isCorrectToolForDrops(ItemStack, BlockState)` applies the `DiggerItem` drop rule with that tag (the Fabric branch
  needs `ItemStackMixin` for this), and it answers the `PICKAXE_DIG` tool action.
- **Bows.** `WeaponBowItem.releaseUsing` is Forge's `BowItem.releaseUsing` copied step by step with the bow's speed,
  spread, damage and abilities put in, so `ArrowLooseEvent` (fired with the bow's own draw ticks), `ArrowItem.isInfinite`
  and `customArrow` still work; `use` is Forge's, so `ArrowNockEvent` fires too. Power, Punch, Flame and Infinity apply
  as vanilla does (Power isn't scaled by the bow's damage), and bows take the bow enchantments by being `BowItem`s.
  The Fabric client mixins become events: `ComputeFovModifierEvent` for the draw zoom, and `MovementInputUpdateEvent`
  (which fires just before vanilla's 20% slowdown) for the walk speed while drawing.
- **Light Arrows** (horse bow). The knockback in `hurt` and the arrow's Punch push both scale with
  `1 − knockback resistance`, so `LivingAttackEvent` raises the target's knockback resistance by a transient modifier
  that halves that factor, and it comes off on the target's next tick (`LivingTickEvent`). That halves both at once,
  Punch included, where the Fabric branch needs a mixin on each (Punch is a whole-number level here).
- **Rapier thrust** adds Sharpness and the like through `EnchantmentHelper.getDamageBonus` and sets Fire Aspect
  itself, as `Player.attack` does in this version.
- **Sabre parry** and the rope dart's PvP way out are `LivingAttackEvent`; **two-handed** blocking is `PlayerInteractEvent.RightClickItem` /
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
- **Access transformer.** Climbing the rope dart's rope reads `LivingEntity.jumping`, made public by
  `META-INF/accesstransformer.cfg` (the Fabric branch uses a mixin accessor).
- `Entity.level` is a field and `onGround()` is `isOnGround()` in this version.
- **Build:** ForgeGradle 6 from `server-vein-miner`'s `forge/1.20.1`, set to Minecraft 1.19.2. There is no datagen here; the generated JSON
  comes from the Fabric branch (see Layout).
