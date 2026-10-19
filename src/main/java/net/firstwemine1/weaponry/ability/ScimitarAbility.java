package net.firstwemine1.weaponry.ability;

import java.util.List;

import net.firstwemine1.weaponry.combat.AttackContext;
import net.firstwemine1.weaponry.effect.BleedEffect;
import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Draw Cut: the vanilla sword sweep with about 1.5x the area. Same trigger as vanilla: a full-charge,
 * grounded, non-sprinting, non-critical hit. Everything the sweep cuts, the main target included,
 * starts to Bleed.
 */
public class ScimitarAbility implements WeaponAbility {
	/** Vanilla inflates the target's box by (1, 0.25, 1) and caps at 3 blocks from the player. */
	private static final double AREA_SCALE = 1.5;
	private static final double MAX_DISTANCE_SQR = 9.0 * AREA_SCALE;

	@Override
	public List<String> description() {
		return List.of("Draw Cut: wide sweep on full-charge hits; the sweep causes Bleed");
	}

	@Override
	public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!(attacker instanceof Player player) || !(player.level() instanceof ServerLevel level)
				|| !(stack.getItem() instanceof WeaponItem weapon)) {
			return;
		}
		// onGround also rules out a jump crit; a forced crit (none on this weapon) would not sweep either.
		if (!AttackContext.fullCharge(player) || AttackContext.sprintHit(player) || !player.onGround()) {
			return;
		}

		DamageSource source = player.damageSources().playerAttack(player);
		float sweepDamage = (1.0f + (float) player.getAttributeValue(Attributes.SWEEPING_DAMAGE_RATIO)
				* (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE)) * weapon.abilityStrength();
		var area = target.getBoundingBox().inflate(AREA_SCALE, 0.25 * AREA_SCALE, AREA_SCALE);
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (other == player || other == target || player.isAlliedTo(other)
					|| other instanceof ArmorStand stand && stand.isMarker()
					|| player.distanceToSqr(other) >= MAX_DISTANCE_SQR) {
				continue;
			}
			float damage = EnchantmentHelper.modifyDamage(level, stack, other, source, sweepDamage);
			Abilities.knockbackFrom(other, player, 0.4);
			if (other.hurt(source, damage)) {
				BleedEffect.apply(other, player, weapon.material());
			}
			EnchantmentHelper.doPostAttackEffects(level, other, source);
		}
		BleedEffect.apply(target, player, weapon.material());
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(), 1.0f, 1.0f);
		player.sweepAttack();
	}
}
