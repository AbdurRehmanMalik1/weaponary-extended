package net.firstwemine1.weaponry.bow;

import java.util.List;

import net.firstwemine1.weaponry.combat.Motion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;

/**
 * Horse Archer: faster draw and +1 damage while riding. Light Arrows: half knockback and half Punch
 * (applied by {@code CombatEvents} to arrows tagged {@link #LIGHT_ARROW}). Spread While Moving:
 * wider spread when moving on foot.
 */
public class HorseBowAbility implements BowAbility {
	/** Entity tag on arrows shot from it; {@code CombatEvents} halves their knockback. */
	public static final String LIGHT_ARROW = "weaponry.light_arrow";
	private static final float MOUNTED_DRAW = 0.75f;
	private static final float MOUNTED_DAMAGE = 1.0f;
	private static final float MOVING_SPREAD = 1.2f;
	/** Blocks per tick. Walking while drawing a bow is about 0.04. */
	private static final double MOVING_SPEED = 0.01;

	@Override
	public List<String> description() {
		return List.of(
				"Horse Archer: while riding, a 25% faster draw and +1 damage",
				"Light Arrows: half knockback, and Punch is halved",
				"Spread While Moving: 20% wider spread when shooting on the move on foot");
	}

	@Override
	public int drawTicks(BowType type, LivingEntity user) {
		return user.isPassenger() ? Math.round(type.drawTicks() * MOUNTED_DRAW) : type.drawTicks();
	}

	@Override
	public float bonusDamage(LivingEntity user, int ticksAtFullDraw) {
		return user.isPassenger() ? MOUNTED_DAMAGE : 0;
	}

	@Override
	public float spread(BowType type, LivingEntity user, int ticksAtFullDraw) {
		boolean moving = !user.isPassenger() && Motion.horizontalSpeed(user) > MOVING_SPEED;
		return moving ? type.spread() * MOVING_SPREAD : type.spread();
	}

	@Override
	public void onArrow(AbstractArrow arrow, LivingEntity user) {
		arrow.addTag(LIGHT_ARROW);
	}

	/** Whether an arrow was shot from a horse bow. */
	public static boolean isLight(Entity arrow) {
		return arrow instanceof AbstractArrow && arrow.getTags().contains(LIGHT_ARROW);
	}
}
