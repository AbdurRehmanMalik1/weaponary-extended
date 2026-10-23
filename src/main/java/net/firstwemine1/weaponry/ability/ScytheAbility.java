package net.firstwemine1.weaponry.ability;

import com.google.common.collect.ImmutableList;
import java.util.List;

import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Reap: a sweep on full-charge hits with about 1.5x the vanilla area, dealing half the hit's full-charge damage
 * (at least the vanilla sweep damage). Reaches as far as the scythe does, slightly shorter than the glaive.
 */
public class ScytheAbility implements WeaponAbility {
	private static final double AREA_SCALE = 1.5;
	private static final float DAMAGE_SHARE = 0.5f;

	@Override
	public List<String> description() {
		return ImmutableList.of("Reap: wide sweep on full-charge hits, dealing half the hit's damage");
	}

	@Override
	public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		WeaponItem weapon = Sweep.weaponFor(stack, attacker);
		if (weapon == null) {
			return;
		}
		PlayerEntity player = (PlayerEntity) attacker;
		float damage = Math.max(Sweep.vanillaDamage(player),
				DAMAGE_SHARE * (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE));
		Sweep.sweep((ServerWorld) player.level, player, target, stack, AREA_SCALE, weapon.type().reach(),
				damage * weapon.abilityStrength(), other -> {
				});
	}
}
