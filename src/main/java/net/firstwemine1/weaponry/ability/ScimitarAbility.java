package net.firstwemine1.weaponry.ability;

import com.google.common.collect.ImmutableList;
import java.util.List;

import net.firstwemine1.weaponry.combat.AttackContext;
import net.firstwemine1.weaponry.effect.BleedEffect;
import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.util.math.AxisAlignedBB;

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
		return ImmutableList.of("Draw Cut: wide sweep on full-charge hits; the sweep causes Bleed");
	}

	@Override
	public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!(attacker instanceof PlayerEntity) || !(attacker.level instanceof ServerWorld)
				|| !(stack.getItem() instanceof WeaponItem)) {
			return;
		}
		PlayerEntity player = (PlayerEntity) attacker;
		ServerWorld level = (ServerWorld) attacker.level;
		WeaponItem weapon = (WeaponItem) stack.getItem();
		// onGround also rules out a jump crit; a forced crit (none on this weapon) would not sweep either.
		if (!AttackContext.fullCharge(player) || AttackContext.sprintHit(player) || !player.isOnGround()) {
			return;
		}

		DamageSource source = DamageSource.playerAttack(player);
		float sweepDamage = (1.0f + EnchantmentHelper.getSweepingDamageRatio(player)
				* (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE)) * weapon.abilityStrength();
		AxisAlignedBB area = target.getBoundingBox().inflate(AREA_SCALE, 0.25 * AREA_SCALE, AREA_SCALE);
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (other == player || other == target || player.isAlliedTo(other)
					|| other instanceof ArmorStandEntity && ((ArmorStandEntity) other).isMarker()
					|| player.distanceToSqr(other) >= MAX_DISTANCE_SQR) {
				continue;
			}
			Abilities.knockbackFrom(other, player, 0.4);
			if (other.hurt(source, sweepDamage)) {
				BleedEffect.apply(other, player, weapon.material());
			}
		}
		BleedEffect.apply(target, player, weapon.material());
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(), 1.0f, 1.0f);
		player.sweepAttack();
	}
}
