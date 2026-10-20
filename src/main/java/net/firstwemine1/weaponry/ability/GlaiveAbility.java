package net.firstwemine1.weaponry.ability;

import com.google.common.collect.ImmutableList;
import java.util.List;

import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Arc: a vanilla-sized sweep on full-charge hits, out to the glaive's reach. */
public class GlaiveAbility implements WeaponAbility {
	@Override
	public List<String> description() {
		return ImmutableList.of("Arc: weak sweep on full-charge hits");
	}

	@Override
	public void onHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		weakSweep(stack, target, attacker);
	}

	/** The vanilla sword sweep, reaching as far as the weapon does instead of 3 blocks. Shared with the greatsword. */
	static void weakSweep(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		WeaponItem weapon = Sweep.weaponFor(stack, attacker);
		if (weapon == null) {
			return;
		}
		Player player = (Player) attacker;
		Sweep.sweep((ServerLevel) player.level, player, target, stack, 1.0, weapon.type().reach(),
				Sweep.vanillaDamage(player) * weapon.abilityStrength(), other -> {
				});
	}
}
