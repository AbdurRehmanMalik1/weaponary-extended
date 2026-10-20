package net.firstwemine1.weaponry.ability;

import java.util.List;

import net.firstwemine1.weaponry.combat.AttackContext;
import net.firstwemine1.weaponry.effect.ModEffects;
import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponMaterial;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import com.google.common.collect.ImmutableMultimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Armor Crush: ignores half the target's armor. Stagger: full-charge hits stagger the target for
 * 1 s. Overhead Smash: a full-charge hit while sneaking knocks back everything around the target
 * and stuns it all briefly.
 */
public class WarHammerAbility implements WeaponAbility {
	private static final float ARMOR_PIERCE = 0.5f;
	private static final double SMASH_RADIUS = 2.5;
	private static final double SMASH_KNOCKBACK = 0.8;
	private static final int STAGGER_TICKS = 20;
	private static final int STUN_TICKS = 15;

	@Override
	public List<String> description() {
		return List.of(
				"Armor Crush: ignores half of the target's armor",
				"Stagger: full-charge hits Stagger the target",
				"Overhead Smash (sneak + full-charge hit): smash the ground, knocking back and Stunning nearby enemies");
	}

	@Override
	public void addAttributes(ImmutableMultimap.Builder<Attribute, AttributeModifier> builder, WeaponMaterial material) {
		ClubAbility.addStoneKnockback(builder, material);
	}

	@Override
	public float armorPierce(WeaponMaterial material) {
		// Design sheet twists: a crude wooden head crushes half as well, gold abilities are stronger.
		float pierce = ARMOR_PIERCE * material.abilityStrength();
		return material == WeaponMaterial.WOODEN ? pierce * 0.5f : pierce;
	}

	@Override
	public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (!AttackContext.fullCharge(attacker) || !(attacker.level() instanceof ServerLevel level)) {
			return;
		}
		float strength = stack.getItem() instanceof WeaponItem weapon ? weapon.abilityStrength() : 1.0f;
		target.addEffect(new MobEffectInstance(ModEffects.STAGGER.get(), Math.round(STAGGER_TICKS * strength)), attacker);
		if (attacker.isShiftKeyDown()) {
			smash(level, target, attacker, strength);
		}
	}

	private static void smash(ServerLevel level, LivingEntity center, LivingEntity attacker, float strength) {
		int stunTicks = Math.round(STUN_TICKS * strength);
		center.addEffect(new MobEffectInstance(ModEffects.STUN.get(), stunTicks), attacker);
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, center.getBoundingBox().inflate(SMASH_RADIUS))) {
			if (other == attacker || other == center || attacker.isAlliedTo(other)
					|| other instanceof ArmorStand || other.distanceToSqr(center) > SMASH_RADIUS * SMASH_RADIUS) {
				continue;
			}
			// knockback() pushes away from the given direction, so pass the vector toward the center.
			other.knockback(SMASH_KNOCKBACK * strength, center.getX() - other.getX(), center.getZ() - other.getZ());
			other.addEffect(new MobEffectInstance(ModEffects.STUN.get(), stunTicks), attacker);
		}

		BlockState ground = level.getBlockState(center.getOnPos());
		if (!ground.isAir()) {
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
					center.getX(), center.getY(), center.getZ(), 40, SMASH_RADIUS * 0.4, 0.1, SMASH_RADIUS * 0.4, 0.15);
		}
		level.playSound(null, center.getX(), center.getY(), center.getZ(),
				SoundEvents.ANVIL_LAND, attacker.getSoundSource(), 0.5f, 0.6f);
	}
}
