package net.firstwemine1.weaponry.ability;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.firstwemine1.weaponry.combat.Motion;
import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Hook Pull: use on a target in reach to pull it toward you. Hook Leap: use twice on the same
 * target within 0.3 s to pull yourself toward it instead. Wall Brake: hold use while falling next
 * to a wall to hook into it and slow down gradually.
 *
 * <p>The first click can't know whether a second one follows, so the server holds it for the
 * double-click window and only then pulls. Holding use down repeats it every 4 ticks, which also
 * counts as a double click.
 */
public class HookSwordAbility implements WeaponAbility {
	private static final int DOUBLE_CLICK_TICKS = 6;
	/** Shared by both moves. */
	private static final int PULL_COOLDOWN = 30;
	private static final int LEAP_COOLDOWN = 60;
	private static final double PULL_SPEED = 0.6;
	private static final double PULL_LIFT = 0.25;
	/** The leap stops about this far short of the target, so you land in melee range. */
	private static final double LEAP_STOP_SHORT = 1.0;
	/** Horizontal blocks travelled per unit of launch speed: about 9 ticks airborne at 0.91 air drag. */
	private static final double LEAP_TRAVEL_PER_SPEED = 6.0;
	private static final double LEAP_MAX_SPEED = 0.9;
	private static final double LEAP_LIFT = 0.3;

	private static final int BRAKE_TICKS = 72000;
	/** How far past the player's side a block still counts as the wall. */
	private static final double WALL_DISTANCE = 0.5;
	/** Speed taken off per tick, on top of cancelling gravity. Blocks per tick. */
	private static final double BRAKE = 0.12;
	/** The brake never slows a fall below this, so you slide down instead of hanging. 4 blocks/s. */
	private static final double SLIDE_SPEED = 0.2;
	private static final double GRAVITY = 0.08;
	private static final double AIR_DRAG = 0.98;
	private static final int TICKS_PER_DURABILITY = 10;

	private record Pending(LivingEntity target, InteractionHand hand, long due) {
	}

	/** Server-side: a first click waiting to see whether a second one follows. */
	private static final Map<Player, Pending> PENDING = new WeakHashMap<>();

	@Override
	public List<String> description() {
		return List.of(
				"Hook Pull (use): pull a target toward you",
				"Hook Leap (double use): pull yourself toward the target",
				"Wall Brake (hold use while falling beside a wall): hook in and slow the fall");
	}

	@Override
	public InteractionResultHolder<ItemStack> onUse(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		LivingEntity target = Abilities.targetInReach(player);
		if (target != null) {
			if (!level.isClientSide()) {
				click(player, target, hand);
			}
			return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
		}
		if (airborne(player) && wallBeside(player) != null) {
			player.startUsingItem(hand);
			return InteractionResultHolder.consume(stack);
		}
		return InteractionResultHolder.pass(stack);
	}

	@Override
	public int useDuration() {
		return BRAKE_TICKS;
	}

	/**
	 * Wall brake, every tick use is held. The player moves itself, so the slowdown runs on its own
	 * client; fall damage is worked out on the server, so that side lowers the stored fall distance
	 * to match the current speed.
	 */
	@Override
	public void onUseTick(ItemStack stack, Level level, LivingEntity user, int remainingTicks) {
		if (!(user instanceof Player player)) {
			return;
		}
		if (!airborne(player)) {
			player.stopUsingItem();
			return;
		}
		BlockPos wall = wallBeside(player);
		if (wall == null) {
			return;
		}
		if (level.isClientSide()) {
			// isControlledByLocalInstance() is false for the client's own player in this version.
			if (player.isLocalPlayer()) {
				brake(player);
			}
			return;
		}

		double down = Math.max(0, -Motion.velocity(player).y);
		// The height a fall from rest needs to reach this speed (ignoring drag, so slightly lenient).
		player.fallDistance = Math.min(player.fallDistance, (float) (down * down / (2 * GRAVITY)));
		if (down <= SLIDE_SPEED * 0.5) {
			return;
		}
		int braked = BRAKE_TICKS - remainingTicks;
		if (braked % TICKS_PER_DURABILITY == 0) {
			stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(player.getUsedItemHand()));
		}
		if (braked % 2 == 0 && level instanceof ServerLevel server) {
			scrape(server, player, wall, braked);
		}
	}

	private static void brake(Player player) {
		Vec3 motion = player.getDeltaMovement();
		if (motion.y >= -SLIDE_SPEED) {
			return;
		}
		// Gravity and drag are applied after this tick's use; this floor leaves exactly the slide speed.
		double floor = -SLIDE_SPEED / AIR_DRAG + GRAVITY;
		player.setDeltaMovement(motion.x, Math.min(motion.y + BRAKE + GRAVITY, floor), motion.z);
	}

	private static void scrape(ServerLevel level, Player player, BlockPos wall, int braked) {
		BlockState state = level.getBlockState(wall);
		Vec3 contact = new Vec3(
				Mth.clamp(player.getX(), wall.getX(), wall.getX() + 1.0),
				player.getY() + 1.0,
				Mth.clamp(player.getZ(), wall.getZ(), wall.getZ() + 1.0));
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
				contact.x, contact.y, contact.z, 4, 0.05, 0.2, 0.05, 0.05);
		if (braked % 4 == 0) {
			level.playSound(null, contact.x, contact.y, contact.z,
					state.getSoundType().getHitSound(), SoundSource.PLAYERS, 0.8f, 0.7f);
		}
	}

	/** In the air, not swimming, gliding, flying or climbing: the brake has something to stop. */
	private static boolean airborne(Player player) {
		return !player.isOnGround() && !player.isInWater() && !player.isInLava() && !player.isFallFlying()
				&& !player.getAbilities().flying && !player.onClimbable() && !player.isPassenger();
	}

	/** A solid block right beside the player, at leg or chest height, or {@code null}. */
	private static BlockPos wallBeside(Player player) {
		double reach = player.getBbWidth() / 2 + WALL_DISTANCE;
		for (double height : new double[] {0.2, 1.2}) {
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				BlockPos pos = new BlockPos(
						player.getX() + direction.getStepX() * reach,
						player.getY() + height,
						player.getZ() + direction.getStepZ() * reach);
				if (!player.level.getBlockState(pos).getCollisionShape(player.level, pos).isEmpty()) {
					return pos;
				}
			}
		}
		return null;
	}

	private static void click(Player player, LivingEntity target, InteractionHand hand) {
		long now = player.level.getGameTime();
		Pending first = PENDING.remove(player);
		if (first != null && first.target() == target && now <= first.due()) {
			leap(player, target, hand);
			return;
		}
		PENDING.put(player, new Pending(target, hand, now + DOUBLE_CLICK_TICKS));
	}

	/** Runs the pull for every first click whose double-click window ran out. */
	static void tick(MinecraftServer server) {
		Iterator<Map.Entry<Player, Pending>> it = PENDING.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Player, Pending> entry = it.next();
			Player player = entry.getKey();
			Pending pending = entry.getValue();
			if (player.level.getGameTime() <= pending.due()) {
				continue;
			}
			it.remove();
			if (stillValid(player, pending)) {
				pull(player, pending.target(), pending.hand());
			}
		}
	}

	/** The target may have died, moved away, or the player switched items during the window. */
	private static boolean stillValid(Player player, Pending pending) {
		LivingEntity target = pending.target();
		// No reach attribute in this version: the hook sword's listed reach, plus slack for the wait.
		double range = WeaponType.HOOK_SWORD.reach() + 1.0;
		return player.isAlive() && target.isAlive() && target.level == player.level
				&& player.distanceToSqr(target) <= range * range
				&& WeaponItem.is(player.getItemInHand(pending.hand()), WeaponType.HOOK_SWORD);
	}

	private static void pull(Player player, LivingEntity target, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		float strength = stack.getItem() instanceof WeaponItem weapon ? weapon.abilityStrength() : 1.0f;
		Abilities.pull(target, player, PULL_SPEED * strength, PULL_LIFT);
		player.level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0f, 0.8f);
		stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(hand));
		Abilities.cooldown(player, WeaponType.HOOK_SWORD, PULL_COOLDOWN);
	}

	/** Launches the player toward the target. No fall protection: leaping off a ledge falls normally. */
	private static void leap(Player player, LivingEntity target, InteractionHand hand) {
		Vec3 offset = target.position().subtract(player.position());
		Vec3 flat = offset.multiply(1, 0, 1);
		double distance = flat.length() - LEAP_STOP_SHORT;
		if (distance > 0.1) {
			Vec3 launch = flat.normalize().scale(Math.min(LEAP_MAX_SPEED, distance / LEAP_TRAVEL_PER_SPEED));
			double lift = LEAP_LIFT + Mth.clamp(offset.y * 0.1, -0.2, 0.3);
			player.setDeltaMovement(launch.x, lift, launch.z);
			// A player moves itself; hurtMarked sends the new motion to its client.
			player.hurtMarked = true;
		}
		player.level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.0f, 0.7f);
		player.getItemInHand(hand).hurtAndBreak(2, player, broken -> broken.broadcastBreakEvent(hand));
		Abilities.cooldown(player, WeaponType.HOOK_SWORD, LEAP_COOLDOWN);
	}
}
