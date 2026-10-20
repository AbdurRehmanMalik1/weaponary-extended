package net.firstwemine1.weaponry.bow;

import java.util.List;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;

/**
 * A bow's benefits and drawbacks ({@code bow-stats.md} §5). Each {@link BowType} holds one;
 * {@link WeaponBowItem} and the mixins call these hooks. Every hook defaults to vanilla behaviour.
 *
 * <p>{@code ticksAtFullDraw} is how long the bow has been held at full draw, or negative if it isn't
 * fully drawn.
 */
public interface BowAbility {
	/** Vanilla's walk speed factor while using an item (LocalPlayer). */
	float VANILLA_WALK_SPEED = 0.2f;

	/** English tooltip lines, each starting with the ability's name; datagen turns them into lang keys. */
	List<String> description();

	/** Whether the bow can be drawn (and shot) right now. */
	default boolean canDraw(LivingEntity user) {
		return true;
	}

	/** Ticks to full draw for this user right now. */
	default int drawTicks(BowType type, LivingEntity user) {
		return type.drawTicks();
	}

	/** Draw ticks a new draw starts with, already drawn. Called when the draw starts. */
	default int headStart(BowType type, LivingEntity user) {
		return 0;
	}

	/** Walk speed factor while drawing (client side). */
	default float walkSpeedWhileDrawing() {
		return VANILLA_WALK_SPEED;
	}

	/** Inaccuracy of a shot. Server side, at release. */
	default float spread(BowType type, LivingEntity user, int ticksAtFullDraw) {
		return type.spread();
	}

	/** Extra zoom while drawing, on top of the normal bow zoom (0.15 at full draw). Client side. */
	default float extraZoom(int ticksAtFullDraw) {
		return 0;
	}

	/** Flat damage added to the shot (in half-hearts). */
	default float bonusDamage(LivingEntity user, int ticksAtFullDraw) {
		return 0;
	}

	/** Tweak an arrow just before it's shot (pickup rule, tags). Server side. */
	default void onArrow(AbstractArrow arrow, LivingEntity user) {
	}

	/** Durability on top of the usual 1 per arrow. */
	default int extraDurability(float power) {
		return 0;
	}
}
