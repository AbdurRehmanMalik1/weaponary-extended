package net.firstwemine1.weaponry.test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.crossbow.Magazine;
import net.firstwemine1.weaponry.crossbow.ModCrossbows;
import net.firstwemine1.weaponry.crossbow.RapidCrossbowItem;
import net.firstwemine1.weaponry.item.ModComponents;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.HitResult;

/**
 * What server game tests can't reach: the real attack and use keys, the client mixins, the item model
 * frames, tooltips and sounds. Fabric API has no client game test support on 1.21.1, so this scripts a
 * real client through a singleplayer world, one step per tick. Only active in {@code ./gradlew runClientE2E}
 * (which opens a game window), where it exits with status 1 if any check fails. Screenshots of each
 * pose land in {@code build/run/clientE2E/screenshots} for looking at.
 */
public class CrossbowClientE2E implements ClientModInitializer {
	private static final int HOLD_TICKS = 40;
	private static final int STEP_TIMEOUT = 600;
	private static final int SYNC_TICKS = 4;

	private final Deque<Step> steps = new ArrayDeque<>();
	private final List<String> failures = new ArrayList<>();
	private Minecraft client;
	private int stepTicks;

	private record Step(String name, BooleanSupplier done) {
	}

	@Override
	public void onInitializeClient() {
		if (!Boolean.getBoolean("weaponry.clientE2E")) {
			return;
		}
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			// On the first tick: item stacks can't be built until their components are bound.
			if (client == null) {
				script();
			}
			client = mc;
			tick();
		});
	}

	private void tick() {
		Step step = steps.peek();
		if (step == null) {
			finish();
			return;
		}
		stepTicks++;
		try {
			if (step.done().getAsBoolean()) {
				steps.poll();
				stepTicks = 0;
			} else if (stepTicks > STEP_TIMEOUT) {
				fail("timed out: " + step.name());
				steps.poll();
				stepTicks = 0;
			}
		} catch (RuntimeException e) {
			Weaponry.LOGGER.error("Client E2E step {} threw", step.name(), e);
			fail(step.name() + " threw " + e);
			steps.poll();
			stepTicks = 0;
		}
	}

	private void finish() {
		if (failures.isEmpty()) {
			Weaponry.LOGGER.info("CLIENT E2E PASSED");
			client.stop();
		} else {
			failures.forEach(failure -> Weaponry.LOGGER.error("CLIENT E2E FAILURE: {}", failure));
			Weaponry.LOGGER.error("CLIENT E2E FAILED: {} checks", failures.size());
			Runtime.getRuntime().halt(1);
		}
	}

	private void script() {
		step("open a world", this::openWorld);
		wait("the world to load", () -> client.player != null && client.level != null && client.screen == null
				&& client.getSingleplayerServer() != null && !client.getSingleplayerServer().getPlayerList().getPlayers().isEmpty());
		// The rest builds item stacks, which can't be made until a world has bound their components.
		run("plan the rest", this::inWorld);
	}

	private void inWorld() {
		run("build the test range", this::buildRange);
		pause(20);
		run("check the player looks at the wall", () -> check(client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK, "crosshair on the stone"));

		// The control: if an empty hand doesn't start breaking the block, the checks below prove nothing.
		run("empty hand clicks", () -> press(client.options.keyAttack));
		pause(3);
		run("empty hand breaks blocks", () -> check(client.gameMode.isDestroying(), "control: an empty hand starts breaking the block"));
		run("let go", () -> release(client.options.keyAttack));
		pause(5);

		twoHandedSwitch();
		rapidFiring();
		rapidReload();
		tooltips();
		heavyFrames();
		sounds();
		run("back to first person", () -> client.options.setCameraType(CameraType.FIRST_PERSON));
	}

	/** The bug a player reported: pressing a hotbar key to take up a greatsword deleted a stack in the off hand. */
	private void twoHandedSwitch() {
		give(ItemStack.EMPTY, new ItemStack(ModItems.variants(WeaponType.GREATSWORD).iterator().next()));
		run("torches in the off hand", () -> withServerPlayer(player -> {
			player.getInventory().setItem(Inventory.SLOT_OFFHAND, new ItemStack(Items.TORCH, 32));
			player.inventoryMenu.broadcastChanges();
		}));
		pause(5);
		run("off hand holds the torches", () -> check(client.player.getOffhandItem().is(Items.TORCH), "control: the torches reached the off hand"));
		run("press hotbar 2", () -> press(client.options.keyHotbarSlots[1]));
		pause(10);
		run("the torches moved", () -> {
			release(client.options.keyHotbarSlots[1]);
			Inventory inventory = client.player.getInventory();
			check(client.player.getMainHandItem().getItem() instanceof WeaponItem, "the hotbar key took up the greatsword");
			check(client.player.getOffhandItem().isEmpty(), "the off hand is clear");
			check(inventory.countItem(Items.TORCH) == 32, "all 32 torches are in the inventory, found " + inventory.countItem(Items.TORCH));
		});
		run("press hotbar 1", () -> press(client.options.keyHotbarSlots[0]));
		pause(3);
		run("let go", () -> release(client.options.keyHotbarSlots[0]));
	}

	private void rapidFiring() {
		give(loaded(64, Items.ARROW));
		// Long enough for the hand to finish raising the new item, for the screenshot.
		pause(20);
		run("rapid sprite while loaded", () -> checkSprite("rapid_crossbow_arrow"));
		run("rapid first person screenshot", () -> screenshot("rapid-loaded-first-person"));
		pause(2);
		run("press attack", () -> press(client.options.keyAttack));
		int[] held = {0};
		step("hold attack", () -> {
			check(!client.gameMode.isDestroying(), "holding attack with it never breaks blocks");
			check(client.player.attackAnim == 0, "and never swings");
			return ++held[0] >= HOLD_TICKS;
		});
		run("rapid firing screenshot", () -> screenshot("rapid-firing-first-person"));
		run("firing while held", () -> {
			int fired = 64 - RapidCrossbowItem.magazine(serverPlayer().getMainHandItem()).count();
			// 5 per second, less a little for the packet's trip.
			check(fired >= 8 && fired <= 11, "40 ticks of holding attack fires about 10 arrows, was " + fired);
			check(serverPlayer().getMainHandItem().getDamageValue() == fired, "1 durability per arrow");
			check(client.level.getBlockState(wall()).is(Blocks.STONE), "the wall is still there");
		});
		run("let go of attack", () -> release(client.options.keyAttack));
		pause(SYNC_TICKS);
		int[] afterRelease = new int[1];
		run("remember the count", () -> afterRelease[0] = RapidCrossbowItem.magazine(serverPlayer().getMainHandItem()).count());
		pause(12);
		run("no shots after release", () -> check(RapidCrossbowItem.magazine(serverPlayer().getMainHandItem()).count() == afterRelease[0], "stops when attack is let go"));
		run("client sees the count", () -> check(RapidCrossbowItem.magazine(client.player.getMainHandItem()).count() == afterRelease[0], "the magazine syncs to the client"));
	}

	private void rapidReload() {
		give(new ItemStack(ModCrossbows.RAPID), new ItemStack(Items.ARROW, 64));
		pause(SYNC_TICKS);
		run("rapid sprite while empty", () -> checkSprite("rapid_crossbow"));
		run("hold use", () -> hold(client.options.keyUse));
		List<String> seen = new ArrayList<>();
		wait("the magazine to fill", () -> {
			if (client.player.isUsingItem() && (seen.isEmpty() || !seen.getLast().equals(sprite()))) {
				seen.add(sprite());
			}
			return RapidCrossbowItem.magazine(client.player.getMainHandItem()).isFull();
		});
		run("reload frames", () -> check(seen.contains(id("rapid_crossbow_pulling_0")) && seen.contains(id("rapid_crossbow_pulling_1")),
				"frames follow the magazine, saw " + seen));
		pause(SYNC_TICKS);
		run("reload stops itself", () -> {
			check(!client.player.isUsingItem(), "the client stops reloading at 64, with use still held");
			check(!serverPlayer().isUsingItem(), "the server too");
			check(!serverPlayer().getInventory().hasAnyMatching(stack -> stack.is(Items.ARROW)), "64 arrows used");
			checkSprite("rapid_crossbow_arrow");
		});
		run("let go of use", () -> release(client.options.keyUse));
		pause(SYNC_TICKS);
		run("third person", () -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		pause(5);
		run("rapid third person screenshot", () -> screenshot("rapid-loaded-third-person"));
		pause(3);
		run("first person", () -> client.options.setCameraType(CameraType.FIRST_PERSON));
	}

	private void tooltips() {
		give(loaded(37, Items.ARROW));
		pause(SYNC_TICKS);
		run("rapid tooltip", () -> {
			String text = tooltip();
			check(text.contains("Arrows: 37 / 64"), "magazine line, was " + text);
			check(text.contains("Gatling (hold attack): "), "Gatling line, was " + text);
			check(text.contains("Crank Reload (hold use): "), "Crank Reload line, was " + text);
		});
		give(new ItemStack(ModCrossbows.HEAVY));
		pause(SYNC_TICKS);
		run("heavy tooltip", () -> {
			String text = tooltip();
			check(text.contains("Heavy Bolt: "), "Heavy Bolt line, was " + text);
			check(text.contains("Slow Crank (use): "), "Slow Crank line, was " + text);
		});
	}

	private void heavyFrames() {
		give(new ItemStack(ModCrossbows.HEAVY), new ItemStack(Items.ARROW, 4));
		pause(SYNC_TICKS);
		run("heavy idle sprite", () -> checkSprite("heavy_crossbow"));
		run("heavy first person screenshot", () -> screenshot("heavy-idle-first-person"));

		// Let go early: it doesn't load.
		run("hold use briefly", () -> hold(client.options.keyUse));
		pause(20);
		run("heavy first load frame", () -> check(sprite().contains("heavy_crossbow_pulling_"), "a load frame while cranking, was " + sprite()));
		run("let go early", () -> release(client.options.keyUse));
		pause(SYNC_TICKS);
		run("20 ticks isn't enough", () -> check(!CrossbowItem.isCharged(serverPlayer().getMainHandItem()), "doesn't load early"));

		// A full load through every frame.
		run("hold use", () -> hold(client.options.keyUse));
		List<String> seen = new ArrayList<>();
		int[] held = {0};
		step("crank", () -> {
			if (seen.isEmpty() || !seen.getLast().equals(sprite())) {
				seen.add(sprite());
			}
			return ++held[0] >= 36;
		});
		run("let go after a full load", () -> release(client.options.keyUse));
		pause(SYNC_TICKS);
		run("load frames", () -> {
			for (int frame = 0; frame < 3; frame++) {
				check(seen.contains(id("heavy_crossbow_pulling_" + frame)), "frame " + frame + ", saw " + seen);
			}
		});
		run("loaded", () -> {
			check(CrossbowItem.isCharged(client.player.getMainHandItem()), "loaded on the client");
			checkSprite("heavy_crossbow_arrow");
		});
		run("heavy loaded first person screenshot", () -> screenshot("heavy-loaded-first-person"));
		pause(2);
		run("third person", () -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		pause(5);
		run("heavy third person screenshot", () -> screenshot("heavy-loaded-third-person"));
		pause(3);
		run("first person", () -> client.options.setCameraType(CameraType.FIRST_PERSON));

		// Use fires it, through the normal click.
		run("fire", () -> press(client.options.keyUse));
		pause(2);
		run("let go", () -> release(client.options.keyUse));
		pause(SYNC_TICKS);
		run("fired", () -> {
			check(!CrossbowItem.isCharged(serverPlayer().getMainHandItem()), "the shot empties it");
			check(serverPlayer().getMainHandItem().getDamageValue() == 1, "and costs 1 durability");
		});

		// The firework frame.
		run("give a rocket", () -> withServerPlayer(player -> {
			player.getInventory().clearContent();
			player.getInventory().setItem(0, new ItemStack(ModCrossbows.HEAVY));
			player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.FIREWORK_ROCKET));
		}));
		pause(SYNC_TICKS);
		run("hold use for the rocket", () -> hold(client.options.keyUse));
		pause(36);
		run("let go of the rocket", () -> release(client.options.keyUse));
		pause(SYNC_TICKS);
		run("firework frame", () -> checkSprite("heavy_crossbow_firework"));
	}

	private void sounds() {
		run("sounds resolve", () -> {
			for (String sound : List.of("loading_start", "loading_middle", "loading_end")) {
				var events = client.getSoundManager().getSoundEvent(Weaponry.id("item.heavy_crossbow." + sound));
				check(events != null, "sounds.json defines " + sound);
				check(events.getWeight() > 0, sound + " points at real sound files");
			}
		});
	}

	// ---- Steps ----

	private void step(String name, BooleanSupplier done) {
		steps.add(new Step(name, done));
	}

	private void wait(String name, BooleanSupplier condition) {
		step(name, condition);
	}

	private void run(String name, Runnable action) {
		step(name, () -> {
			action.run();
			return true;
		});
	}

	private void pause(int ticks) {
		int[] left = {ticks};
		step("wait " + ticks + " ticks", () -> --left[0] <= 0);
	}

	private void give(ItemStack... stacks) {
		run("give items", () -> withServerPlayer(player -> {
			player.getInventory().clearContent();
			player.getInventory().setSelectedSlot(0);
			for (int slot = 0; slot < stacks.length; slot++) {
				player.getInventory().setItem(slot, stacks[slot].copy());
			}
			player.inventoryMenu.broadcastChanges();
		}));
	}

	// ---- Scenery ----

	private boolean openWorld() {
		if (!(client.screen instanceof TitleScreen)) {
			return false;
		}
		LevelSettings settings = new LevelSettings("clientE2E" + System.currentTimeMillis(), GameType.SURVIVAL, new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
		client.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(1, false, false),
				WorldPresets::createNormalWorldDimensions, client.screen);
		return true;
	}

	/** A stone floor high above the terrain, with a stone block at eye level right in front of the player. */
	private void buildRange() {
		ServerPlayer player = serverPlayer();
		BlockPos center = rangeCenter(player);
		for (int x = -3; x <= 3; x++) {
			for (int z = -3; z <= 3; z++) {
				player.level().setBlockAndUpdate(center.offset(x, -1, z), Blocks.STONE.defaultBlockState());
			}
		}
		player.level().setBlockAndUpdate(wall(), Blocks.STONE.defaultBlockState());
		player.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
		player.connection.teleport(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0, 0);
		forceMouseGrabbed();
	}

	private BlockPos rangeCenter(ServerPlayer player) {
		BlockPos spawn = player.blockPosition();
		return new BlockPos(spawn.getX(), 200, spawn.getZ());
	}

	private BlockPos wall() {
		return rangeCenter(serverPlayer()).offset(0, 1, 2);
	}

	/**
	 * Vanilla only continues an attack while the mouse is grabbed, which needs the window to have focus.
	 * Without this, a run with the window in the background wouldn't exercise the block-breaking check.
	 */
	private void forceMouseGrabbed() {
		try {
			Field grabbed = MouseHandler.class.getDeclaredField("mouseGrabbed");
			grabbed.setAccessible(true);
			grabbed.setBoolean(client.mouseHandler, true);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	private ServerPlayer serverPlayer() {
		return client.getSingleplayerServer().getPlayerList().getPlayers().get(0);
	}

	private void withServerPlayer(Consumer<ServerPlayer> action) {
		client.getSingleplayerServer().execute(() -> action.accept(serverPlayer()));
	}

	private static ItemStack loaded(int count, Item arrow) {
		ItemStack stack = new ItemStack(ModCrossbows.RAPID);
		stack.set(ModComponents.MAGAZINE, new Magazine(count, arrow));
		return stack;
	}

	// ---- Input and reading the client's state ----

	private void press(KeyMapping key) {
		KeyMapping.set(key.getDefaultKey(), true);
		KeyMapping.click(key.getDefaultKey());
	}

	private void hold(KeyMapping key) {
		KeyMapping.set(key.getDefaultKey(), true);
	}

	private void release(KeyMapping key) {
		KeyMapping.set(key.getDefaultKey(), false);
	}

	/** The texture the item model picks right now, through the same overrides that draw it in hand. */
	private String sprite() {
		ItemStack stack = client.player.getMainHandItem();
		ItemStackRenderState state = new ItemStackRenderState();
		client.getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.GUI, client.level, client.player, 0);
		return state.pickParticleMaterial(RandomSource.create(0)).sprite().contents().name().toString();
	}

	private static String id(String texture) {
		return Weaponry.id("item/" + texture).toString();
	}

	private void checkSprite(String texture) {
		check(sprite().equals(id(texture)), "expected sprite " + id(texture) + ", was " + sprite());
	}

	private String tooltip() {
		ItemStack stack = client.player.getMainHandItem();
		StringBuilder text = new StringBuilder();
		for (Component line : stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL)) {
			text.append(line.getString()).append('\n');
		}
		return text.toString();
	}

	private void screenshot(String name) {
		Screenshot.grab(new File("."), name + ".png", client.getMainRenderTarget(), 1, message -> {
		});
	}

	private void check(boolean condition, String message) {
		if (!condition) {
			fail(message);
		}
	}

	private void fail(String message) {
		// A check that fails every tick of a hold should only be listed once.
		if (!failures.contains(message)) {
			Weaponry.LOGGER.error("CLIENT E2E FAILURE: {}", message);
			failures.add(message);
		}
	}
}
