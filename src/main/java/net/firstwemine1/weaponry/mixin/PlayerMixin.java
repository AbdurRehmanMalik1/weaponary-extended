package net.firstwemine1.weaponry.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.combat.AttackContext;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hooks into the player's melee attack: charge capture, crit suppression, minimum charge and armor crush. */
@Mixin(Player.class)
abstract class PlayerMixin implements AttackContext {
	@Unique
	private static final ResourceLocation ARMOR_CRUSH_ID = Weaponry.id("armor_crush");

	@Unique
	private float weaponry$charge = 1.0f;
	@Unique
	private boolean weaponry$sprintHit;
	@Unique
	private boolean weaponry$forcedCrit;
	@Unique
	private boolean weaponry$crit;

	@Inject(method = "attack", at = @At("HEAD"))
	private void weaponry$captureAttack(Entity target, CallbackInfo ci) {
		Player self = (Player) (Object) this;
		weaponry$charge = self.getAttackStrengthScale(0.5f);
		weaponry$sprintHit = self.isSprinting();
		weaponry$forcedCrit = false;
		weaponry$crit = false;
	}

	/** {@code isPassenger()} is only read by the crit check, so reporting "riding" cancels the crit. */
	@ModifyExpressionValue(method = "attack",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isPassenger()Z"))
	private boolean weaponry$blockCrit(boolean passenger) {
		return passenger || !WeaponItem.abilityOf(((Player) (Object) this).getMainHandItem()).allowsCrits();
	}

	/**
	 * Minimum charge: a weak hit with some weapons does nothing (vanilla then plays the no-damage sound).
	 * Armor crush: the target loses part of its armor for the main hit only.
	 */
	@WrapOperation(method = "attack",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean weaponry$armorCrush(Entity target, DamageSource source, float amount, Operation<Boolean> original) {
		Player self = (Player) (Object) this;
		if (weaponry$charge < WeaponItem.abilityOf(self.getMainHandItem()).minimumCharge()) {
			return false;
		}
		float pierce = self.getMainHandItem().getItem() instanceof WeaponItem weapon
				? weapon.type().ability().armorPierce(weapon.material())
				: 0;
		AttributeInstance armor = target instanceof LivingEntity living && !self.level().isClientSide()
				? living.getAttribute(Attributes.ARMOR)
				: null;
		if (pierce <= 0 || armor == null) {
			return original.call(target, source, amount);
		}
		armor.addTransientModifier(new AttributeModifier(ARMOR_CRUSH_ID, -pierce, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		try {
			return original.call(target, source, amount);
		} finally {
			armor.removeModifier(ARMOR_CRUSH_ID);
		}
	}

	@Override
	public float weaponry$charge() {
		return weaponry$charge;
	}

	@Override
	public boolean weaponry$sprintHit() {
		return weaponry$sprintHit;
	}

	@Override
	public boolean weaponry$forcedCrit() {
		return weaponry$forcedCrit;
	}

	@Override
	public void weaponry$setForcedCrit(boolean forced) {
		weaponry$forcedCrit = forced;
	}

	@Override
	public boolean weaponry$crit() {
		return weaponry$crit;
	}

	@Override
	public void weaponry$setCrit(boolean crit) {
		weaponry$crit = crit;
	}
}
