package net.firstwemine1.weaponry.test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.crossbow.HeavyCrossbowItem;
import net.firstwemine1.weaponry.crossbow.Magazine;
import net.firstwemine1.weaponry.crossbow.ModCrossbows;
import net.firstwemine1.weaponry.crossbow.RapidCrossbowItem;
import net.firstwemine1.weaponry.crossbow.RapidFiring;
import net.firstwemine1.weaponry.crossbow.WeaponCrossbow;
import net.firstwemine1.weaponry.item.ModComponents;
import net.firstwemine1.weaponry.mixin.AbstractArrowAccessor;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * Checks both crossbows against {@code pixel-art/weaponry/docs/crossbow-stats.md} in a real server.
 * Holding the attack key is client side, so these start firing through {@link RapidFiring#setFiring},
 * where the network packet lands; {@code CrossbowClientE2E} covers the key itself.
 */
public class CrossbowGameTest {
	private static final int LONG = 200;

	// ---- Heavy Crossbow ----

	@GameTest
	public void heavyStats(GameTestHelper helper) {
		check(new ItemStack(ModCrossbows.HEAVY).getMaxDamage() == 520, "durability 520");
		check(new ItemStack(ModCrossbows.HEAVY).getMaxStackSize() == 1, "doesn't stack");
		helper.succeed();
	}

	@GameTest
	public void heavyLoadTime(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		int[] expected = {32, 27, 22, 17};
		for (int level = 0; level < expected.length; level++) {
			ItemStack stack = new ItemStack(ModCrossbows.HEAVY);
			if (level > 0) {
				enchant(helper, stack, Enchantments.QUICK_CHARGE, level);
			}
			checkEquals(expected[level], CrossbowItem.getChargeDuration(stack, player), "load ticks at Quick Charge " + level);
		}
		checkEquals(25, CrossbowItem.getChargeDuration(new ItemStack(Items.CROSSBOW), player), "vanilla crossbow is untouched");
		finish(helper, player);
	}

	@GameTest
	public void heavyOnlyLoadsAtFullCharge(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, new ItemStack(ModCrossbows.HEAVY));
		player.getInventory().add(new ItemStack(Items.ARROW, 2));
		ModCrossbows.HEAVY.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		ModCrossbows.HEAVY.onUseTick(helper.getLevel(), player, stack, stack.getUseDuration(player) - 31);
		check(!CrossbowItem.isCharged(stack), "31 ticks isn't enough");
		checkEquals(2, count(player, Items.ARROW), "no arrow used yet");
		ModCrossbows.HEAVY.onUseTick(helper.getLevel(), player, stack, stack.getUseDuration(player) - 32);
		check(CrossbowItem.isCharged(stack), "32 ticks loads it");
		checkEquals(1, count(player, Items.ARROW), "one arrow used");
		finish(helper, player);
	}

	@GameTest(maxTicks = LONG)
	public void heavyBoltDamage(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, new ItemStack(ModCrossbows.HEAVY));
		Cow cow = target(helper, 4.5, 0, 1.5);
		loadHeavy(helper, player, stack, Items.ARROW);
		aim(player, cow);
		ModCrossbows.HEAVY.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

		AbstractArrow arrow = onlyArrow(helper, player);
		checkEquals(2.8, ((AbstractArrowAccessor) arrow).weaponry$baseDamage(), 0.001, "x1.4 base damage");
		checkEquals(3.15, arrow.getDeltaMovement().length(), 0.25, "vanilla arrow speed, give or take its spread");
		check(arrow.isCritArrow(), "player crossbow shots always crit");
		check(HeavyCrossbowItem.isHeavyBolt(arrow), "tagged as a heavy bolt");
		checkEquals(1, stack.getDamageValue(), "1 durability per shot");

		helper.succeedWhen(() -> {
			float damage = cow.getMaxHealth() - cow.getHealth();
			helper.assertTrue(damage > 0, "the bolt hasn't hit yet");
			check(damage >= 9 && damage <= 14, "damage 9-14, was " + damage);
			remove(helper, player);
		});
	}

	@GameTest(maxTicks = LONG)
	public void heavyBoltKnocksBackFurtherThanVanilla(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		Cow heavyTarget = target(helper, 4.5, 0, 1.5);
		Cow vanillaTarget = target(helper, 4.5, 0, 6.5);
		ServerPlayer other = shooter(helper, 1.5, 0, 6.5);
		double[] heavyPush = peakSpeed(helper, heavyTarget);
		double[] vanillaPush = peakSpeed(helper, vanillaTarget);

		ItemStack heavy = give(player, new ItemStack(ModCrossbows.HEAVY));
		loadHeavy(helper, player, heavy, Items.ARROW);
		aim(player, heavyTarget);
		ModCrossbows.HEAVY.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		fireVanillaCrossbow(helper, other, vanillaTarget);

		helper.runAfterDelay(15, () -> {
			check(vanillaPush[0] > 0, "the vanilla arrow should push its target");
			// Vanilla's hit is 0.4 blocks/tick; Punch II adds 2 x 0.6 on top.
			check(heavyPush[0] > vanillaPush[0] * 1.5, "heavy push " + heavyPush[0] + ", vanilla " + vanillaPush[0]);
			remove(helper, player);
			remove(helper, other);
			helper.succeed();
		});
	}

	/** §6 decision: Heavy Bolt is arrows only. A rocket loads slowly but flies and hurts as vanilla. */
	@GameTest
	public void heavyFireworksAreVanilla(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, new ItemStack(ModCrossbows.HEAVY));
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.FIREWORK_ROCKET));
		ModCrossbows.HEAVY.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		ModCrossbows.HEAVY.onUseTick(helper.getLevel(), player, stack, stack.getUseDuration(player) - 32);
		player.stopUsingItem();
		check(CrossbowItem.isCharged(stack), "loads a rocket in the same 32 ticks");

		ModCrossbows.HEAVY.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		List<FireworkRocketEntity> rockets = helper.getLevel().getEntitiesOfClass(FireworkRocketEntity.class, player.getBoundingBox().inflate(4));
		checkEquals(1, rockets.size(), "fires one rocket");
		check(rockets.get(0).entityTags().isEmpty(), "the rocket isn't a heavy bolt");
		checkEquals(0.0, helper.getLevel().getEntitiesOfClass(AbstractArrow.class, player.getBoundingBox().inflate(4)).size(), 0, "no arrow");
		checkEquals(3, stack.getDamageValue(), "rockets cost 3 durability, as vanilla");
		finish(helper, player);
	}

	@GameTest
	public void heavyLoadSoundsAreLowered(GameTestHelper helper) throws ReflectiveOperationException {
		Method method = CrossbowItem.class.getDeclaredMethod("getChargingSounds", ItemStack.class);
		method.setAccessible(true);

		ItemStack heavy = new ItemStack(ModCrossbows.HEAVY);
		check(method.invoke(ModCrossbows.HEAVY, heavy) == HeavyCrossbowItem.sounds(), "heavy uses its own sounds");

		CrossbowItem.ChargingSounds vanilla = (CrossbowItem.ChargingSounds) method.invoke(Items.CROSSBOW, new ItemStack(Items.CROSSBOW));
		check(vanilla.start().orElseThrow() == SoundEvents.CROSSBOW_LOADING_START, "vanilla's sounds are untouched");

		ItemStack quick = new ItemStack(ModCrossbows.HEAVY);
		enchant(helper, quick, Enchantments.QUICK_CHARGE, 1);
		CrossbowItem.ChargingSounds quickSounds = (CrossbowItem.ChargingSounds) method.invoke(ModCrossbows.HEAVY, quick);
		check(quickSounds != HeavyCrossbowItem.sounds(), "Quick Charge keeps its own sounds");
		helper.succeed();
	}

	// ---- Rapid Crossbow: stats ----

	@GameTest
	public void rapidStats(GameTestHelper helper) {
		ItemStack stack = new ItemStack(ModCrossbows.RAPID);
		check(stack.getMaxDamage() == 1600, "durability 1600");
		check(stack.getMaxStackSize() == 1, "doesn't stack");
		checkEquals(64, Magazine.CAPACITY, "magazine size");
		check(!ModCrossbows.RAPID.canDestroyBlock(new ItemStack(ModCrossbows.RAPID), null, helper.getLevel(), BlockPos.ZERO, null), "never breaks blocks");
		check(!ModCrossbows.RAPID.isBarVisible(stack), "no bar while empty");
		checkEquals(13, ModCrossbows.RAPID.getBarWidth(loaded(64, Items.ARROW)), "bar is full at 64");
		checkEquals(7, ModCrossbows.RAPID.getBarWidth(loaded(32, Items.ARROW)), "bar is half at 32");
		helper.succeed();
	}

	@GameTest
	public void rapidArrowStats(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, loaded(5, Items.ARROW));
		check(ModCrossbows.RAPID.fire(helper.getLevel(), player, stack), "fires");
		AbstractArrow arrow = onlyArrow(helper, player);
		checkEquals(0.15, ((AbstractArrowAccessor) arrow).weaponry$baseDamage(), 0.001, "base damage");
		checkEquals(3.15, arrow.getDeltaMovement().length(), 0.45, "arrow speed, give or take its spread");
		check(!arrow.isCritArrow(), "never crits");
		check(RapidCrossbowItem.isRapidArrow(arrow), "tagged as a rapid arrow");
		check(arrow.pickup == AbstractArrow.Pickup.ALLOWED, "a survival player's arrows can be picked up");
		checkEquals(4, RapidCrossbowItem.magazine(stack).count(), "1 arrow from the magazine");
		checkEquals(1, stack.getDamageValue(), "1 durability per arrow");
		finish(helper, player);
	}

	/** Like vanilla's crossbow, the shooter's movement isn't added to the arrow. Facing south, moving east. */
	@GameTest
	public void rapidArrowIgnoresShooterMovement(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, loaded(1, Items.ARROW));
		player.setDeltaMovement(3, 0, 0);
		ModCrossbows.RAPID.fire(helper.getLevel(), player, stack);
		checkEquals(0.0, onlyArrow(helper, player).getDeltaMovement().x, 0.5, "sideways speed");
		finish(helper, player);
	}

	@GameTest
	public void rapidFiresSpectralArrows(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, loaded(1, Items.SPECTRAL_ARROW));
		ModCrossbows.RAPID.fire(helper.getLevel(), player, stack);
		check(onlyArrow(helper, player) instanceof SpectralArrow, "the magazine's type is what flies");
		check(!ModCrossbows.RAPID.fire(helper.getLevel(), player, stack), "an empty magazine doesn't fire");
		finish(helper, player);
	}

	@GameTest
	public void rapidBreaksCleanlyWhileFiring(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, loaded(10, Items.ARROW));
		stack.setDamageValue(stack.getMaxDamage() - 1);
		RapidFiring.setFiring(player, true);
		helper.runAfterDelay(6, () -> {
			check(player.getMainHandItem().isEmpty(), "the crossbow broke on its last durability");
			check(!RapidFiring.isFiring(player), "and firing stopped");
			checkEquals(1.0, arrows(helper, player).size(), 0, "after one arrow");
			finish(helper, player);
		});
	}

	// ---- Rapid Crossbow: Gatling ----

	@GameTest(maxTicks = LONG)
	public void rapidFiresEveryFourTicks(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, loaded(6, Items.ARROW));
		RapidFiring.setFiring(player, true);
		List<Long> shots = new ArrayList<>();
		int[] last = {6};
		helper.onEachTick(() -> {
			int now = RapidCrossbowItem.magazine(stack).count();
			if (now != last[0]) {
				checkEquals(last[0] - 1, now, "one arrow per shot");
				last[0] = now;
				shots.add(helper.getTick());
			}
		});
		helper.runAfterDelay(40, () -> {
			checkEquals(6, shots.size(), "the whole magazine fired");
			for (int i = 1; i < shots.size(); i++) {
				checkEquals(RapidFiring.FIRE_INTERVAL, shots.get(i) - shots.get(i - 1), "ticks between shots");
			}
			check(RapidFiring.isFiring(player), "still held, just empty");
			finish(helper, player);
		});
	}

	/** The client can't raise the fire rate: start/stop spam every tick still shoots once per 4 ticks. */
	@GameTest(maxTicks = LONG)
	public void rapidFireRateIgnoresPacketSpam(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, loaded(64, Items.ARROW));
		boolean[] on = {false};
		helper.onEachTick(() -> {
			on[0] = !on[0];
			RapidFiring.setFiring(player, on[0]);
		});
		helper.runAfterDelay(40, () -> {
			int fired = 64 - RapidCrossbowItem.magazine(stack).count();
			check(fired >= 1 && fired <= 11, "at most 11 shots in 40 ticks, was " + fired);
			RapidFiring.setFiring(player, false);
			finish(helper, player);
		});
	}

	@GameTest(maxTicks = LONG)
	public void rapidStopsWhenReleasedOrSwitchedAway(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, loaded(64, Items.ARROW));
		RapidFiring.setFiring(player, true);
		helper.runAfterDelay(8, () -> RapidFiring.setFiring(player, false));
		int[] afterRelease = new int[1];
		helper.runAfterDelay(10, () -> afterRelease[0] = RapidCrossbowItem.magazine(stack).count());
		helper.runAfterDelay(20, () -> {
			check(afterRelease[0] < 64, "fired while held");
			checkEquals(afterRelease[0], RapidCrossbowItem.magazine(stack).count(), "no arrows after release");
			RapidFiring.setFiring(player, true);
			player.getInventory().setSelectedSlot(1);
		});
		helper.runAfterDelay(30, () -> {
			check(!RapidFiring.isFiring(player), "switching away stops firing");
			checkEquals(afterRelease[0], RapidCrossbowItem.magazine(stack).count(), "no arrows from another slot");
			finish(helper, player);
		});
	}

	@GameTest(maxTicks = LONG)
	public void rapidCantFireWhileReloading(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, loaded(10, Items.ARROW));
		player.getInventory().add(new ItemStack(Items.ARROW, 64));
		ModCrossbows.RAPID.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		RapidFiring.setFiring(player, true);
		int[] waited = {0};
		helper.onEachTick(() -> {
			if (waited[0]++ < 12) {
				player.doTick();
			}
		});
		helper.runAfterDelay(14, () -> {
			check(arrows(helper, player).isEmpty(), "no arrows fired while reloading");
			checkEquals(22, RapidCrossbowItem.magazine(stack).count(), "12 ticks of reloading");
			RapidFiring.setFiring(player, false);
			finish(helper, player);
		});
	}

	// ---- Rapid Crossbow: hits ----

	/** Vanilla's 10-tick hurt cooldown would cap the stream at 2 hits per second; every arrow must land. */
	@GameTest(maxTicks = LONG)
	public void rapidEveryArrowLandsForOneDamage(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, loaded(10, Items.ARROW));
		Cow cow = target(helper, 3.5, 0, 1.5);
		cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
		aim(player, cow);
		RapidFiring.setFiring(player, true);
		helper.runAfterDelay(70, () -> {
			checkEquals(0, RapidCrossbowItem.magazine(stack).count(), "all fired");
			checkEquals(10.0, cow.getMaxHealth() - cow.getHealth(), 0.001, "10 arrows at 1 damage each");
			checkEquals(0, cow.invulnerableTime, "no hurt cooldown left behind");
			RapidFiring.setFiring(player, false);
			finish(helper, player);
		});
	}

	@GameTest(maxTicks = LONG)
	public void otherArrowsKeepTheHurtCooldown(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, new ItemStack(ModCrossbows.HEAVY));
		Cow cow = target(helper, 3.5, 0, 1.5);
		loadHeavy(helper, player, stack, Items.ARROW);
		aim(player, cow);
		ModCrossbows.HEAVY.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		helper.succeedWhen(() -> {
			helper.assertTrue(cow.getHealth() < cow.getMaxHealth(), "not hit yet");
			check(cow.invulnerableTime > 0, "a normal hit starts the cooldown");
			remove(helper, player);
		});
	}

	@GameTest(maxTicks = LONG)
	public void rapidHalvesKnockback(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ServerPlayer other = shooter(helper, 1.5, 0, 6.5);
		Cow rapidTarget = target(helper, 4.5, 0, 1.5);
		Cow normalTarget = target(helper, 4.5, 0, 6.5);
		double[] rapidPush = peakSpeed(helper, rapidTarget);
		double[] normalPush = peakSpeed(helper, normalTarget);

		ItemStack rapid = give(player, loaded(1, Items.ARROW));
		aim(player, rapidTarget);
		ModCrossbows.RAPID.fire(helper.getLevel(), player, rapid);
		fireVanillaCrossbow(helper, other, normalTarget);

		helper.runAfterDelay(15, () -> {
			check(rapidPush[0] > 0, "it still pushes");
			check(rapidPush[0] < normalPush[0] * 0.75, "rapid push " + rapidPush[0] + ", normal " + normalPush[0]);
			remove(helper, player);
			remove(helper, other);
			helper.succeed();
		});
	}

	// ---- Rapid Crossbow: Crank Reload ----

	@GameTest
	public void rapidReloadsOneArrowPerTick(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, new ItemStack(ModCrossbows.RAPID));
		player.getInventory().add(new ItemStack(Items.ARROW, 64));
		player.getInventory().add(new ItemStack(Items.ARROW, 10));
		checkEquals(64, ticksToFill(helper, player, stack), "an empty magazine takes 3.2 s");
		checkEquals(64, RapidCrossbowItem.magazine(stack).count(), "full");
		checkEquals(10, count(player, Items.ARROW), "uses 64 arrows");
		check(!player.isUsingItem(), "stops when full");
		check(!ModCrossbows.RAPID.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "a full magazine won't start");
		finish(helper, player);
	}

	@GameTest
	public void rapidReloadScalesWithRoomLeft(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, loaded(32, Items.ARROW));
		player.getInventory().add(new ItemStack(Items.ARROW, 64));
		checkEquals(32, ticksToFill(helper, player, stack), "half a magazine takes 1.6 s");
		finish(helper, player);
	}

	@GameTest
	public void rapidKeepsWhatItLoadedWhenLetGo(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, new ItemStack(ModCrossbows.RAPID));
		player.getInventory().add(new ItemStack(Items.ARROW, 64));
		ModCrossbows.RAPID.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		for (int i = 0; i < 10; i++) {
			player.doTick();
		}
		player.releaseUsingItem();
		player.doTick();
		checkEquals(10, RapidCrossbowItem.magazine(stack).count(), "keeps the 10 it loaded");
		checkEquals(54, count(player, Items.ARROW), "and only used 10");
		finish(helper, player);
	}

	@GameTest
	public void rapidReloadStopsWhenOutOfArrows(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, new ItemStack(ModCrossbows.RAPID));
		check(!ModCrossbows.RAPID.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "no arrows, no reload");
		player.getInventory().add(new ItemStack(Items.ARROW, 5));
		checkEquals(5, ticksToFill(helper, player, stack), "stops after the last arrow");
		checkEquals(5, RapidCrossbowItem.magazine(stack).count(), "with what it has");
		check(!player.isUsingItem(), "and stops using");
		finish(helper, player);
	}

	@GameTest(maxTicks = LONG)
	public void rapidQuickChargeReloadTimes(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		// 3.2 s, 2.56 s, 1.92 s and 1.28 s, rounded up to whole ticks.
		int[] expected = {64, 52, 39, 26};
		for (int level = 0; level < expected.length; level++) {
			player.getInventory().clearContent();
			ItemStack stack = give(player, new ItemStack(ModCrossbows.RAPID));
			if (level > 0) {
				enchant(helper, stack, Enchantments.QUICK_CHARGE, level);
			}
			player.getInventory().add(new ItemStack(Items.ARROW, 64));
			checkEquals(expected[level], ticksToFill(helper, player, stack), "ticks to fill at Quick Charge " + level);
			checkEquals(64, RapidCrossbowItem.magazine(stack).count(), "full at Quick Charge " + level);
			checkEquals(0, count(player, Items.ARROW), "uses exactly 64 at Quick Charge " + level);
		}
		finish(helper, player);
	}

	// ---- Rapid Crossbow: which arrows it takes ----

	@GameTest
	public void rapidRefusesTippedArrows(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, new ItemStack(ModCrossbows.RAPID));
		player.getInventory().add(tipped(16));
		check(!ModCrossbows.RAPID.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "tipped arrows alone won't start a reload");
		player.setItemInHand(InteractionHand.OFF_HAND, tipped(16));
		player.getInventory().add(new ItemStack(Items.ARROW, 3));
		checkEquals(3, ticksToFill(helper, player, stack), "tipped arrows are skipped, even in the off hand");
		check(RapidCrossbowItem.magazine(stack).arrow() == Items.ARROW, "loaded the plain arrows");
		checkEquals(16, player.getOffhandItem().getCount(), "off hand untouched");
		checkEquals(16, count(player, Items.TIPPED_ARROW), "inventory tipped arrows untouched");
		finish(helper, player);
	}

	@GameTest
	public void rapidHoldsOneTypeAtATime(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, new ItemStack(ModCrossbows.RAPID));
		player.getInventory().add(new ItemStack(Items.SPECTRAL_ARROW, 4));
		player.getInventory().add(new ItemStack(Items.ARROW, 30));
		checkEquals(4, ticksToFill(helper, player, stack), "the first arrows found set the type");
		check(RapidCrossbowItem.magazine(stack).arrow() == Items.SPECTRAL_ARROW, "spectral");
		checkEquals(30, count(player, Items.ARROW), "plain arrows aren't mixed in");
		check(!ModCrossbows.RAPID.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "no more of that type: no reload");

		for (int i = 0; i < 4; i++) {
			ModCrossbows.RAPID.fire(helper.getLevel(), player, stack);
		}
		checkEquals(30, ticksToFill(helper, player, stack), "once empty it takes any type again");
		check(RapidCrossbowItem.magazine(stack).arrow() == Items.ARROW, "plain");
		finish(helper, player);
	}

	@GameTest
	public void rapidLoadsTheOffHandFirst(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, new ItemStack(ModCrossbows.RAPID));
		player.getInventory().add(new ItemStack(Items.ARROW, 10));
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SPECTRAL_ARROW, 2));
		checkEquals(2, ticksToFill(helper, player, stack), "off hand first");
		check(RapidCrossbowItem.magazine(stack).arrow() == Items.SPECTRAL_ARROW, "spectral from the off hand");
		finish(helper, player);
	}

	@GameTest
	public void rapidCreativeReloadIsFree(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		player.setGameMode(GameType.CREATIVE);
		ItemStack stack = give(player, new ItemStack(ModCrossbows.RAPID));
		checkEquals(64, ticksToFill(helper, player, stack), "loads with no arrows");
		ModCrossbows.RAPID.fire(helper.getLevel(), player, stack);
		check(onlyArrow(helper, player).pickup == AbstractArrow.Pickup.CREATIVE_ONLY, "free arrows can't be farmed");
		finish(helper, player);
	}

	@GameTest
	public void rapidMagazineSaves(GameTestHelper helper) {
		Magazine magazine = new Magazine(37, Items.SPECTRAL_ARROW);
		var json = Magazine.CODEC.encodeStart(JsonOps.INSTANCE, magazine).getOrThrow();
		check(Magazine.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().equals(magazine), "round trip through the save format");
		ItemStack saved = loaded(37, Items.SPECTRAL_ARROW);
		var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
		ItemStack parsed = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, saved).getOrThrow()).getOrThrow();
		check(RapidCrossbowItem.magazine(parsed).equals(magazine), "and on a saved item");
		helper.succeed();
	}

	// ---- Enchantments ----

	@GameTest
	public void rapidEnchantmentRules(GameTestHelper helper) {
		ItemStack rapid = new ItemStack(ModCrossbows.RAPID);
		check(allowed(helper, rapid, Enchantments.QUICK_CHARGE), "Quick Charge allowed");
		check(allowed(helper, rapid, Enchantments.PIERCING), "Piercing allowed");
		check(allowed(helper, rapid, Enchantments.UNBREAKING), "Unbreaking allowed");
		check(allowed(helper, rapid, Enchantments.MENDING), "Mending allowed");
		check(!allowed(helper, rapid, Enchantments.MULTISHOT), "Multishot refused");
		check(!allowed(helper, rapid, Enchantments.INFINITY), "Infinity refused");
		check(!allowed(helper, rapid, Enchantments.POWER), "Power refused");

		ItemStack heavy = new ItemStack(ModCrossbows.HEAVY);
		check(allowed(helper, heavy, Enchantments.QUICK_CHARGE), "Heavy: Quick Charge allowed");
		check(allowed(helper, heavy, Enchantments.PIERCING), "Heavy: Piercing allowed");
		check(allowed(helper, heavy, Enchantments.MULTISHOT), "Heavy: Multishot allowed");
		helper.succeed();
	}

	@GameTest
	public void rapidEnchantingTableNeverOffersMultishot(GameTestHelper helper) {
		var enchantments = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		List<EnchantmentInstance> offered = EnchantmentHelper.getAvailableEnchantmentResults(30, new ItemStack(ModCrossbows.RAPID), enchantments.listElements().map(h -> (Holder<Enchantment>) h));
		check(offered.stream().anyMatch(e -> e.enchantment().is(Enchantments.QUICK_CHARGE)), "the table offers Quick Charge");
		check(offered.stream().noneMatch(e -> e.enchantment().is(Enchantments.MULTISHOT)), "and never Multishot");
		List<EnchantmentInstance> heavy = EnchantmentHelper.getAvailableEnchantmentResults(30, new ItemStack(ModCrossbows.HEAVY), enchantments.listElements().map(h -> (Holder<Enchantment>) h));
		check(heavy.stream().anyMatch(e -> e.enchantment().is(Enchantments.MULTISHOT)), "the heavy crossbow can get it");
		helper.succeed();
	}

	@GameTest
	public void rapidAnvilRefusesMultishotBooks(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		check(anvilResult(helper, player, new ItemStack(ModCrossbows.RAPID), Enchantments.MULTISHOT).isEmpty(), "a Multishot book does nothing");
		ItemStack quick = anvilResult(helper, player, new ItemStack(ModCrossbows.RAPID), Enchantments.QUICK_CHARGE);
		check(EnchantmentHelper.getItemEnchantmentLevel(holder(helper, Enchantments.QUICK_CHARGE), quick) == 1, "a Quick Charge book works");
		finish(helper, player);
	}

	/** Even forced on with a command, Multishot can't multiply the stream. */
	@GameTest
	public void rapidForcedMultishotStillFiresOneArrow(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, loaded(5, Items.ARROW));
		enchant(helper, stack, Enchantments.MULTISHOT, 1);
		ModCrossbows.RAPID.fire(helper.getLevel(), player, stack);
		checkEquals(1.0, arrows(helper, player).size(), 0, "one arrow");
		checkEquals(4, RapidCrossbowItem.magazine(stack).count(), "one from the magazine");
		finish(helper, player);
	}

	@GameTest
	public void rapidPiercingApplies(GameTestHelper helper) {
		ServerPlayer player = shooter(helper);
		ItemStack stack = give(player, loaded(5, Items.ARROW));
		enchant(helper, stack, Enchantments.PIERCING, 4);
		ModCrossbows.RAPID.fire(helper.getLevel(), player, stack);
		checkEquals(4, onlyArrow(helper, player).getPierceLevel(), "Piercing IV");
		finish(helper, player);
	}

	// ---- Recipes and generated files ----

	@GameTest
	public void recipes(GameTestHelper helper) {
		Item s = Items.STICK;
		Item i = Items.IRON_INGOT;
		Item x = Items.STRING;
		Item h = Items.TRIPWIRE_HOOK;
		Item l = Items.LAPIS_LAZULI;
		Item d = Items.DISPENSER;
		Item o = Items.AIR;
		check(crafts(helper, s, i, s, x, h, x, i, s, i) == ModCrossbows.HEAVY, "heavy recipe");
		check(crafts(helper, l, i, l, x, h, x, o, d, o) == ModCrossbows.RAPID, "rapid recipe");
		check(crafts(helper, s, i, s, x, h, x, o, s, o) == Items.CROSSBOW, "vanilla's recipe still makes vanilla's crossbow");
		helper.succeed();
	}

	@GameTest
	public void generatedFilesExist(GameTestHelper helper) {
		JsonObject lang = json("assets/weaponry/lang/en_us.json");
		for (Item item : ModCrossbows.all()) {
			WeaponCrossbow crossbow = (WeaponCrossbow) item;
			checkEquals(crossbow.displayName(), lang.get(item.getDescriptionId()).getAsString(), "item name");
			List<String> lines = crossbow.description();
			for (int line = 0; line < lines.size(); line++) {
				checkEquals(lines.get(line), lang.get(ModCrossbows.tooltipKey(crossbow, line)).getAsString(), "tooltip line");
				check(lines.get(line).matches("^[A-Z][A-Za-z ]+( \\([a-z ]+\\))?: .+"), "tooltip starts with the ability's name: " + lines.get(line));
			}
			for (String frame : List.of("", "_pulling_0", "_pulling_1", "_pulling_2", "_arrow")) {
				check(exists("assets/weaponry/models/item/" + crossbow.id() + frame + ".json"), "model " + crossbow.id() + frame);
				check(exists("assets/weaponry/textures/item/" + crossbow.id() + frame + ".png"), "texture " + crossbow.id() + frame);
			}
		}
		checkEquals("Arrows: %s / %s", lang.get(RapidCrossbowItem.ARROWS_TOOLTIP_KEY).getAsString(), "magazine line");
		check(exists("assets/weaponry/models/item/heavy_crossbow_firework.json"), "the heavy crossbow has a firework frame");
		check(!exists("assets/weaponry/models/item/rapid_crossbow_firework.json"), "the rapid crossbow doesn't");
		checkEquals(3, json("assets/weaponry/sounds.json").size(), "three heavy load sounds");
		helper.succeed();
	}

	// ---- Helpers ----

	private static ServerPlayer shooter(GameTestHelper helper) {
		return shooter(helper, 1.5, 0, 1.5);
	}

	/** A real server player. Its first hotbar slot is the main hand, and it's in survival, so arrows run out. */
	private static ServerPlayer shooter(GameTestHelper helper, double x, double y, double z) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.getInventory().clearContent();
		Vec3 pos = helper.absoluteVec(new Vec3(x, y, z));
		player.snapTo(pos.x, pos.y, pos.z, 0, 0);
		return player;
	}

	private static Cow target(GameTestHelper helper, double x, double y, double z) {
		Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(x, y, z));
		cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
		cow.setHealth(200);
		return cow;
	}

	private static void aim(ServerPlayer player, Cow cow) {
		player.lookAt(EntityAnchorArgument.Anchor.EYES, cow.position().add(0, cow.getBbHeight() / 2, 0));
	}

	private static ItemStack give(ServerPlayer player, ItemStack stack) {
		player.getInventory().setSelectedSlot(0);
		player.getInventory().getNonEquipmentItems().set(0, stack);
		return stack;
	}

	private static ItemStack loaded(int count, Item arrow) {
		ItemStack stack = new ItemStack(ModCrossbows.RAPID);
		stack.set(ModComponents.MAGAZINE, new Magazine(count, arrow));
		return stack;
	}

	private static ItemStack tipped(int count) {
		ItemStack stack = new ItemStack(Items.TIPPED_ARROW, count);
		stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.HARMING));
		return stack;
	}

	private static void loadHeavy(GameTestHelper helper, ServerPlayer player, ItemStack stack, Item ammo) {
		player.getInventory().add(new ItemStack(ammo));
		ModCrossbows.HEAVY.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
		ModCrossbows.HEAVY.onUseTick(helper.getLevel(), player, stack, stack.getUseDuration(player) - CrossbowItem.getChargeDuration(stack, player));
		player.stopUsingItem();
		check(CrossbowItem.isCharged(stack), "loaded");
	}

	/** Holds use with the game's own item-use tick until it stops, and returns how long that took. */
	private static int ticksToFill(GameTestHelper helper, ServerPlayer player, ItemStack stack) {
		check(ModCrossbows.RAPID.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "reload should start");
		int ticks = 0;
		while (player.isUsingItem() && ticks < 200) {
			player.doTick();
			ticks++;
		}
		return ticks;
	}

	private static List<AbstractArrow> arrows(GameTestHelper helper, ServerPlayer player) {
		return helper.getLevel().getEntitiesOfClass(AbstractArrow.class, player.getBoundingBox().inflate(200), arrow -> arrow.getOwner() == player);
	}

	private static AbstractArrow onlyArrow(GameTestHelper helper, ServerPlayer player) {
		List<AbstractArrow> arrows = arrows(helper, player);
		checkEquals(1.0, arrows.size(), 0, "arrows in the world");
		return arrows.get(0);
	}

	private static int count(ServerPlayer player, Item item) {
		return player.getInventory().getNonEquipmentItems().stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
	}

	private static Holder<Enchantment> holder(GameTestHelper helper, ResourceKey<Enchantment> key) {
		return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}

	private static void enchant(GameTestHelper helper, ItemStack stack, ResourceKey<Enchantment> key, int level) {
		stack.enchant(holder(helper, key), level);
	}

	private static boolean allowed(GameTestHelper helper, ItemStack stack, ResourceKey<Enchantment> key) {
		return stack.canBeEnchantedWith(holder(helper, key), EnchantingContext.ACCEPTABLE);
	}

	private static ItemStack anvilResult(GameTestHelper helper, ServerPlayer player, ItemStack stack, ResourceKey<Enchantment> key) {
		AnvilMenu menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)));
		menu.getSlot(AnvilMenu.INPUT_SLOT).set(stack);
		menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(EnchantmentHelper.createBook(new EnchantmentInstance(holder(helper, key), 1)));
		menu.createResult();
		return menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
	}

	private static Item crafts(GameTestHelper helper, Item... grid) {
		ServerLevel level = helper.getLevel();
		List<ItemStack> stacks = new ArrayList<>();
		for (Item item : grid) {
			stacks.add(new ItemStack(item));
		}
		CraftingInput input = CraftingInput.of(3, 3, stacks);
		return level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
				.map(recipe -> recipe.value().assemble(input).getItem())
				.orElse(Items.AIR);
	}

	/** Tracks the fastest {@code entity} moves sideways from now on, in blocks per tick. */
	private static double[] peakSpeed(GameTestHelper helper, Cow entity) {
		double[] peak = {0};
		helper.onEachTick(() -> peak[0] = Math.max(peak[0], entity.getDeltaMovement().horizontalDistance()));
		return peak;
	}

	private static void fireVanillaCrossbow(GameTestHelper helper, ServerPlayer player, Cow cow) {
		ItemStack stack = give(player, new ItemStack(Items.CROSSBOW));
		stack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(new ItemStackTemplate(Items.ARROW)));
		aim(player, cow);
		((CrossbowItem) Items.CROSSBOW).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
	}

	private static boolean exists(String path) {
		return Weaponry.class.getClassLoader().getResource(path) != null;
	}

	private static JsonObject json(String path) {
		try (InputStream in = Weaponry.class.getClassLoader().getResourceAsStream(path)) {
			check(in != null, "missing " + path);
			return JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (java.io.IOException e) {
			throw new GameTestAssertException(Component.literal(path + ": " + e), 0);
		}
	}

	private static void remove(GameTestHelper helper, ServerPlayer player) {
		RapidFiring.setFiring(player, false);
		helper.getLevel().getServer().getPlayerList().remove(player);
	}

	private static void finish(GameTestHelper helper, ServerPlayer player) {
		remove(helper, player);
		helper.succeed();
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new GameTestAssertException(Component.literal(message), 0);
		}
	}

	private static void checkEquals(Object expected, Object actual, String message) {
		if (!String.valueOf(expected).equals(String.valueOf(actual))) {
			throw new GameTestAssertException(Component.literal(message + ": expected " + expected + ", was " + actual), 0);
		}
	}

	private static void checkEquals(double expected, double actual, double within, String message) {
		if (Math.abs(expected - actual) > within) {
			throw new GameTestAssertException(Component.literal(message + ": expected " + expected + ", was " + actual), 0);
		}
	}
}
