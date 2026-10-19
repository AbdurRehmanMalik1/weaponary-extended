package net.firstwemine1.weaponry.bow;

import java.util.List;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

/** Light Draw: walk faster while drawing. Rough Arrows: some arrows can't be picked up again. */
public class SelfbowAbility implements BowAbility {
	private static final float WALK_SPEED = 0.5f;
	private static final float LOST_ARROW_CHANCE = 0.15f;

	@Override
	public List<String> description() {
		return List.of(
				"Light Draw: walk at 50% speed while drawing (other bows: 20%)",
				"Rough Arrows: 15% of its arrows can't be picked up again");
	}

	@Override
	public float walkSpeedWhileDrawing() {
		return WALK_SPEED;
	}

	@Override
	public void onArrow(AbstractArrow arrow, LivingEntity user) {
		// Arrows that already can't be picked up (Infinity, creative) are left alone.
		if (arrow.pickup == AbstractArrow.Pickup.ALLOWED && user.getRandom().nextFloat() < LOST_ARROW_CHANCE) {
			arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
		}
	}
}
