package net.firstwemine1.weaponry.bow;

import com.google.common.collect.ImmutableList;
import java.util.List;

import net.minecraft.world.entity.LivingEntity;

/**
 * Steady Aim: after 1 s at full draw, near-perfect accuracy and more zoom. Arm Fatigue: after 5 s at
 * full draw, accuracy worsens until the bow is re-drawn. No Horseback: can't be drawn while riding.
 */
public class LongbowAbility implements BowAbility {
	private static final int STEADY_AFTER = 20;
	private static final int FATIGUE_AFTER = 100;
	private static final float STEADY_SPREAD = 0.1f;
	private static final float FATIGUE_SPREAD_PER_TICK = 0.25f / 20;
	private static final float MAX_FATIGUE_SPREAD = 1.5f;
	private static final float STEADY_ZOOM = 0.1f;

	@Override
	public List<String> description() {
		return ImmutableList.of(
				"Steady Aim: hold a full draw for 1 s for near-perfect accuracy and extra zoom",
				"Arm Fatigue: after 5 s at full draw, accuracy worsens until you draw again",
				"No Horseback: can't be drawn while riding");
	}

	@Override
	public boolean canDraw(LivingEntity user) {
		return !user.isPassenger();
	}

	@Override
	public float spread(BowType type, LivingEntity user, int ticksAtFullDraw) {
		if (ticksAtFullDraw >= FATIGUE_AFTER) {
			return Math.min(MAX_FATIGUE_SPREAD, type.spread() + (ticksAtFullDraw - FATIGUE_AFTER) * FATIGUE_SPREAD_PER_TICK);
		}
		return steady(ticksAtFullDraw) ? STEADY_SPREAD : type.spread();
	}

	@Override
	public float extraZoom(int ticksAtFullDraw) {
		return steady(ticksAtFullDraw) ? STEADY_ZOOM : 0;
	}

	private static boolean steady(int ticksAtFullDraw) {
		return ticksAtFullDraw >= STEADY_AFTER && ticksAtFullDraw < FATIGUE_AFTER;
	}
}
