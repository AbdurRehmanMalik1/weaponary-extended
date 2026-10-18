package net.firstwemine1.weaponry.ability;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import net.firstwemine1.weaponry.entity.RopeDart;
import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Throw the dart on a 12-block rope (hold use at least 0.5 s). A hit on a living target tethers it
 * for 5 s; holding use then drags it in. A hit on a block anchors the rope to climb. Using it again
 * reels the dart in; sneak + use lets go in any state. See {@link RopeDart}.
 */
public class RopeDartAbility implements WeaponAbility {
	private static final int USE_DURATION = 72000;
	private static final int MIN_CHARGE_TICKS = 10;
	private static final float THROW_DAMAGE_OFFSET = -1.0f;
	private static final float POWER = 2.0f;

	/** Players whose current use is a drag, not a throw charge (both sides), so releasing it never throws. */
	private static final Set<Player> DRAGGING = Collections.newSetFromMap(new WeakHashMap<>());

	@Override
	public List<String> description() {
		return ImmutableList.of(
				"Hold use to throw the dart on a 12-block rope",
				"Hit a target: tether it for 5 s, hold use to drag it in",
				"Hit a block: hang on the rope, jump to climb, sneak to lower",
				"Use again to reel in; sneak + use to let go");
	}

	@Override
	public InteractionResultHolder<ItemStack> onUse(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		RopeDart dart = RopeDart.of(player);
		if (dart != null) {
			if (dart.isTethered() && !player.isShiftKeyDown()) {
				// Hold to drag; the dart reads the use state each tick.
				DRAGGING.add(player);
				player.startUsingItem(hand);
				return InteractionResultHolder.consume(stack);
			}
			if (!level.isClientSide()) {
				dart.reelIn();
			}
			return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
		}
		if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
			return InteractionResultHolder.fail(stack);
		}
		DRAGGING.remove(player);
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public UseAnim useAnimation() {
		return UseAnim.SPEAR;
	}

	@Override
	public int useDuration() {
		return USE_DURATION;
	}

	@Override
	public void onReleaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingTicks) {
		if (!(user instanceof Player) || !(stack.getItem() instanceof WeaponItem)) {
			return;
		}
		Player player = (Player) user;
		WeaponItem weapon = (WeaponItem) stack.getItem();
		// Releasing a drag (even one whose tether already ended), or a throw not charged long enough.
		if (DRAGGING.remove(player) || RopeDart.of(player) != null || USE_DURATION - remainingTicks < MIN_CHARGE_TICKS) {
			return;
		}
		if (!level.isClientSide()) {
			float damage = Math.max(1.0f, weapon.attackDamage() + THROW_DAMAGE_OFFSET);
			RopeDart dart = new RopeDart(level, player, stack, damage);
			dart.shootFromRotation(player, player.xRot, player.yRot, 0.0f, POWER, 1.0f);
			level.addFreshEntity(dart);
			level.playSound(null, dart, SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.0f, 0.6f);
		}
		player.awardStat(Stats.ITEM_USED.get(weapon));
	}
}
