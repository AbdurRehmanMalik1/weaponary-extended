package net.firstwemine1.weaponry.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.firstwemine1.weaponry.crossbow.HeavyCrossbowItem;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Heavy crossbow: its load time, its lower load sounds and its Heavy Bolt arrows. */
@Mixin(CrossbowItem.class)
abstract class CrossbowItemMixin {
	@ModifyReturnValue(method = "getChargeDuration", at = @At("RETURN"))
	private static int weaponry$slowCrank(int ticks, ItemStack stack) {
		return stack.getItem() instanceof HeavyCrossbowItem ? ticks + HeavyCrossbowItem.EXTRA_CHARGE_TICKS : ticks;
	}

	/** Quick Charge has its own, faster sounds, which stay as they are. */
	@ModifyExpressionValue(method = "getStartSound", at = @At(value = "FIELD",
			target = "Lnet/minecraft/sounds/SoundEvents;CROSSBOW_LOADING_START:Lnet/minecraft/sounds/SoundEvent;"))
	private SoundEvent weaponry$startSound(SoundEvent sound) {
		return (Object) this instanceof HeavyCrossbowItem ? HeavyCrossbowItem.LOADING_START : sound;
	}

	@ModifyExpressionValue(method = "onUseTick", at = @At(value = "FIELD",
			target = "Lnet/minecraft/sounds/SoundEvents;CROSSBOW_LOADING_MIDDLE:Lnet/minecraft/sounds/SoundEvent;"))
	private SoundEvent weaponry$middleSound(SoundEvent sound) {
		return (Object) this instanceof HeavyCrossbowItem ? HeavyCrossbowItem.LOADING_MIDDLE : sound;
	}

	@ModifyExpressionValue(method = "releaseUsing", at = @At(value = "FIELD",
			target = "Lnet/minecraft/sounds/SoundEvents;CROSSBOW_LOADING_END:Lnet/minecraft/sounds/SoundEvent;"))
	private SoundEvent weaponry$endSound(SoundEvent sound) {
		return (Object) this instanceof HeavyCrossbowItem ? HeavyCrossbowItem.LOADING_END : sound;
	}

	/** Only arrows: a rocket's damage comes from its explosion. */
	@ModifyReturnValue(method = "getArrow", at = @At("RETURN"))
	private static AbstractArrow weaponry$heavyBolt(AbstractArrow arrow, net.minecraft.world.level.Level level, net.minecraft.world.entity.LivingEntity shooter, ItemStack crossbow, ItemStack ammo) {
		if (crossbow.getItem() instanceof HeavyCrossbowItem) {
			arrow.setBaseDamage(arrow.getBaseDamage() * HeavyCrossbowItem.DAMAGE);
			arrow.setKnockback(HeavyCrossbowItem.PUNCH);
			arrow.addTag(HeavyCrossbowItem.HEAVY_BOLT);
		}
		return arrow;
	}
}
