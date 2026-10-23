package net.firstwemine1.weaponry.ability;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.util.math.vector.Vector3d;

/**
 * Hook Pull: use on a target in reach to pull it toward you. Hook Leap: use twice on the same
 * target within 0.3 s to pull yourself toward it instead.
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

	private static final class Pending {
		private final LivingEntity target;
		private final Hand hand;
		private final long due;

		Pending(LivingEntity target, Hand hand, long due) {
			this.target = target;
			this.hand = hand;
			this.due = due;
		}

		LivingEntity target() {
			return target;
		}

		Hand hand() {
			return hand;
		}

		long due() {
			return due;
		}
	}

	/** Server-side: a first click waiting to see whether a second one follows. */
	private static final Map<PlayerEntity, Pending> PENDING = new WeakHashMap<>();

	@Override
	public List<String> description() {
		return ImmutableList.of(
				"Use: hook a target toward you",
				"Double use: pull yourself toward the target");
	}

	@Override
	public ActionResult<ItemStack> onUse(World level, PlayerEntity player, Hand hand) {
		ItemStack stack = player.getItemInHand(hand);
		LivingEntity target = Abilities.targetInReach(player);
		if (target == null) {
			return ActionResult.pass(stack);
		}
		if (!level.isClientSide()) {
			click(player, target, hand);
		}
		return ActionResult.sidedSuccess(stack, level.isClientSide());
	}

	private static void click(PlayerEntity player, LivingEntity target, Hand hand) {
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
		Iterator<Map.Entry<PlayerEntity, Pending>> it = PENDING.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<PlayerEntity, Pending> entry = it.next();
			PlayerEntity player = entry.getKey();
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
	private static boolean stillValid(PlayerEntity player, Pending pending) {
		LivingEntity target = pending.target();
		// No reach attribute in this version: the hook sword's listed reach, plus slack for the wait.
		double range = WeaponType.HOOK_SWORD.reach() + 1.0;
		return player.isAlive() && target.isAlive() && target.level == player.level
				&& player.distanceToSqr(target) <= range * range
				&& WeaponItem.is(player.getItemInHand(pending.hand()), WeaponType.HOOK_SWORD);
	}

	private static void pull(PlayerEntity player, LivingEntity target, Hand hand) {
		ItemStack stack = player.getItemInHand(hand);
		float strength = stack.getItem() instanceof WeaponItem ? ((WeaponItem) stack.getItem()).abilityStrength() : 1.0f;
		Abilities.pull(target, player, PULL_SPEED * strength, PULL_LIFT);
		player.level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.FISHING_BOBBER_RETRIEVE, SoundCategory.PLAYERS, 1.0f, 0.8f);
		stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(hand));
		Abilities.cooldown(player, WeaponType.HOOK_SWORD, PULL_COOLDOWN);
	}

	/** Launches the player toward the target. No fall protection: leaping off a ledge falls normally. */
	private static void leap(PlayerEntity player, LivingEntity target, Hand hand) {
		Vector3d offset = target.position().subtract(player.position());
		Vector3d flat = offset.multiply(1, 0, 1);
		double distance = flat.length() - LEAP_STOP_SHORT;
		if (distance > 0.1) {
			Vector3d launch = flat.normalize().scale(Math.min(LEAP_MAX_SPEED, distance / LEAP_TRAVEL_PER_SPEED));
			double lift = LEAP_LIFT + MathHelper.clamp(offset.y * 0.1, -0.2, 0.3);
			player.setDeltaMovement(launch.x, lift, launch.z);
			// A player moves itself; hurtMarked sends the new motion to its client.
			player.hurtMarked = true;
		}
		player.level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FISHING_BOBBER_THROW, SoundCategory.PLAYERS, 1.0f, 0.7f);
		player.getItemInHand(hand).hurtAndBreak(2, player, broken -> broken.broadcastBreakEvent(hand));
		Abilities.cooldown(player, WeaponType.HOOK_SWORD, LEAP_COOLDOWN);
	}
}
