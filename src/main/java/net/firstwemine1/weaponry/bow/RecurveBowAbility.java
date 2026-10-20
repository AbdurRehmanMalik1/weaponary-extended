package net.firstwemine1.weaponry.bow;

import com.google.common.collect.ImmutableList;
import java.util.List;

import net.minecraft.world.entity.LivingEntity;

/**
 * Quick Nock: drawing again soon after a shot starts partly drawn. Snap Shot: releasing right as the
 * draw fills does +1 damage. Dry-fire Strain: weak shots cost extra durability.
 */
public class RecurveBowAbility implements BowAbility {
	private static final int QUICK_NOCK_WINDOW = 20;
	private static final float QUICK_NOCK_POWER = 0.25f;
	private static final int SNAP_WINDOW = 5;
	private static final float SNAP_DAMAGE = 1.0f;
	private static final float STRAIN_BELOW = 0.5f;

	@Override
	public List<String> description() {
		return ImmutableList.of(
				"Quick Nock: draw again within 1 s of a shot to start 25% drawn",
				"Snap Shot: release within 0.25 s of a full draw for +1 damage",
				"Dry-fire Strain: shots below half draw cost 2 durability");
	}

	@Override
	public int headStart(BowType type, LivingEntity user) {
		return BowState.ticksSinceShot(user) <= QUICK_NOCK_WINDOW
				? WeaponBowItem.ticksForPower(QUICK_NOCK_POWER, drawTicks(type, user))
				: 0;
	}

	@Override
	public float bonusDamage(LivingEntity user, int ticksAtFullDraw) {
		return ticksAtFullDraw >= 0 && ticksAtFullDraw <= SNAP_WINDOW ? SNAP_DAMAGE : 0;
	}

	@Override
	public int extraDurability(float power) {
		return power < STRAIN_BELOW ? 1 : 0;
	}
}
