package net.firstwemine1.weaponry.item;

import com.google.common.collect.ImmutableList;
import java.util.List;

import com.google.common.collect.ImmutableMultimap;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.world.World;

/**
 * A weapon's passive and active abilities. Each {@link WeaponType} holds one instance;
 * {@link WeaponItem} and {@code CombatEvents} forward to these hooks. Every hook defaults to "no effect".
 *
 * <p>Abilities scale with {@link WeaponMaterial#abilityStrength()} where the design sheet says so.
 */
public interface WeaponAbility {
	WeaponAbility NONE = new WeaponAbility() {
	};

	/** English tooltip lines; datagen turns them into {@code tooltip.weaponry.<type>.<n>}. */
	default List<String> description() {
		return ImmutableList.of();
	}

	/** Extra attribute modifiers on top of damage, speed and reach. */
	default void addAttributes(ImmutableMultimap.Builder<Attribute, AttributeModifier> builder, WeaponMaterial material) {
	}

	/** Mines {@code #minecraft:mineable/axe} like an axe of the weapon's tier. */
	default boolean choppingTool() {
		return false;
	}

	/** Takes the digging enchantments (Efficiency, Fortune, Silk Touch). */
	default boolean diggerEnchantable() {
		return choppingTool();
	}

	/** Disables a blocking shield for 5 s, like an axe. */
	default boolean disablesShields() {
		return false;
	}

	/** Whether a jumping hit can be a critical hit. */
	default boolean allowsCrits() {
		return true;
	}

	/**
	 * Attack charge (0-1) below which a player's melee hit does nothing at all, as if it missed.
	 * 0 = always hits (vanilla).
	 */
	default float minimumCharge() {
		return 0;
	}

	/**
	 * Fraction (0-1) of a melee hit's damage that gets past a raised shield. 0 = blocked as normal.
	 * Above 0 the shield doesn't block the hit at all: it isn't damaged or disabled.
	 */
	default float shieldPassThrough(LivingEntity attacker) {
		return 0;
	}

	/** Multiplier on the armor durability a melee hit costs the target (1 = vanilla). */
	default float armorWearMultiplier(WeaponMaterial material) {
		return 1;
	}

	/** Fraction (0-1) of the target's armor ignored by a player's melee hit. */
	default float armorPierce(WeaponMaterial material) {
		return 0;
	}

	/**
	 * Extra melee damage on the attack's own target, added from {@code LivingHurtEvent} (see
	 * {@code CombatEvents}). {@code damage} is the hit's damage at that point: charge, crit and
	 * enchantments included, armor not yet applied. Runs on the server only.
	 */
	default float attackDamageBonus(WeaponItem weapon, LivingEntity attacker, Entity target, float damage) {
		return 0;
	}

	/** Called server-side after a melee hit lands. */
	default void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
	}

	/** Called server-side when this weapon, thrown, hits a living target and damages it. */
	default void onThrownHit(ItemStack stack, LivingEntity target, LivingEntity owner) {
	}

	/** Right-click. Return {@code pass} to fall through to vanilla behaviour. */
	default ActionResult<ItemStack> onUse(World level, PlayerEntity player, Hand hand) {
		return ActionResult.pass(player.getItemInHand(hand));
	}

	default UseAction useAnimation() {
		return UseAction.NONE;
	}

	/** How long the item can be held in use, in ticks. */
	default int useDuration() {
		return 0;
	}

	/** Every tick while the item is held in use, on both sides. {@code remainingTicks} counts down from {@link #useDuration()}. */
	default void onUseTick(ItemStack stack, World level, LivingEntity user, int remainingTicks) {
	}

	/** Use released before {@link #useDuration()} ran out. */
	default void onReleaseUsing(ItemStack stack, World level, LivingEntity user, int remainingTicks) {
	}

	/** Use held for the full {@link #useDuration()}. */
	default void onFinishUsing(ItemStack stack, World level, LivingEntity user) {
	}
}
