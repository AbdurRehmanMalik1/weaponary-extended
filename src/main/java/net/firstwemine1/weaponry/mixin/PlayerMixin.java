package net.firstwemine1.weaponry.mixin;

import java.util.UUID;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.firstwemine1.weaponry.combat.AttackContext;
import net.firstwemine1.weaponry.item.WeaponItem;
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

/**
 * Hooks into the player's melee attack: charge capture, the ability damage bonus, crit suppression,
 * minimum charge and armor crush.
 */
@Mixin(Player.class)
abstract class PlayerMixin implements AttackContext {
	@Unique
	private static final UUID ARMOR_CRUSH_UUID = UUID.fromString("b4f7d3c2-8e1a-4b6c-9d2e-3f4a5b6c7d8e");

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

	/**
	 * The ability damage bonus. This version has no {@code Item.getAttackDamageBonus}, so it is added to
	 * the first read of the attack damage attribute, before the charge scaling and the crit multiplier.
	 */
	@ModifyExpressionValue(method = "attack", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/world/entity/ai/attributes/Attribute;)D",
			ordinal = 0))
	private double weaponry$abilityDamage(double damage, @Local(argsOnly = true) Entity target) {
		Player self = (Player) (Object) this;
		if (self.getMainHandItem().getItem() instanceof WeaponItem) {
			WeaponItem weapon = (WeaponItem) self.getMainHandItem().getItem();
			return damage + weapon.type().ability().attackDamageBonus(weapon, self, target, (float) damage);
		}
		return damage;
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
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean weaponry$armorCrush(Entity target, DamageSource source, float amount, Operation<Boolean> original) {
		Player self = (Player) (Object) this;
		if (weaponry$charge < WeaponItem.abilityOf(self.getMainHandItem()).minimumCharge()) {
			return false;
		}
		WeaponItem weapon = self.getMainHandItem().getItem() instanceof WeaponItem ? (WeaponItem) self.getMainHandItem().getItem() : null;
		float pierce = weapon != null ? weapon.type().ability().armorPierce(weapon.material()) : 0;
		AttributeInstance armor = target instanceof LivingEntity && !self.level.isClientSide()
				? ((LivingEntity) target).getAttribute(Attributes.ARMOR)
				: null;
		if (pierce <= 0 || armor == null) {
			return original.call(target, source, amount);
		}
		armor.addTransientModifier(new AttributeModifier(ARMOR_CRUSH_UUID, "Armor crush", -pierce, AttributeModifier.Operation.MULTIPLY_TOTAL));
		try {
			return original.call(target, source, amount);
		} finally {
			armor.removeModifier(ARMOR_CRUSH_UUID);
		}
	}

	/**
	 * Shield-breaker weapons (halberd, hatchet) disable shields the way axes do. This version has no
	 * {@code canDisableShield()}; {@code Player.blockUsingShield} checks for an {@code AxeItem} itself.
	 */
	@Inject(method = "blockUsingShield", at = @At("TAIL"))
	private void weaponry$shieldBreaker(LivingEntity attacker, CallbackInfo ci) {
		Player self = (Player) (Object) this;
		if (self.isBlocking() && WeaponItem.abilityOf(attacker.getMainHandItem()).disablesShields()) {
			self.disableShield(true);
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
