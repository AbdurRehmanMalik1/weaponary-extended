package net.firstwemine1.weaponry.combat;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.firstwemine1.weaponry.ability.SabreAbility;
import net.firstwemine1.weaponry.bow.HorseBowAbility;
import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;

/**
 * The melee hooks that the Fabric branches do with mixins on {@code Player.attack} and
 * {@code LivingEntity.hurt}, done with Forge's events instead. In order, within one attack:
 * <ol>
 *   <li>{@code AttackEntityEvent}: capture the attack charge and sprint ({@link AttackContext}).</li>
 *   <li>{@code CriticalHitEvent}: no crits with the lance; a sabre riposte forces one.</li>
 *   <li>{@code LivingAttackEvent}: a hit below the weapon's minimum charge does nothing.</li>
 *   <li>{@code ShieldBlockEvent}: a weapon that hits through shields stops the block.</li>
 *   <li>{@code LivingHurtEvent} on the attack's target: the ability damage bonus, the part of the hit that
 *       got past the shield, and armor crush lowers the target's armor (it is applied after this event).
 *       Last of all, armor wear adds to the durability the hit costs.</li>
 *   <li>{@code LivingDamageEvent}: armor crush puts the armor back.</li>
 * </ol>
 * Light Arrows (horse bow) work on every hit by one of its arrows: see {@link #onLightArrow}.
 */
public final class CombatEvents {
	private static final UUID ARMOR_CRUSH_UUID = UUID.fromString("b4f7d3c2-8e1a-4b6c-9d2e-3f4a5b6c7d8e");
	private static final UUID LIGHT_ARROW_UUID = UUID.fromString("5d0c8e3b-7a41-4f62-9b1e-2c3d4e5f6a7b");
	private static final float CRIT_MULTIPLIER = 1.5f;

	/** Targets whose armor is crushed for the hit in progress, until LivingDamageEvent restores it. */
	private static final Map<LivingEntity, AttributeInstance> CRUSHED = new WeakHashMap<>();
	/** Targets hit through their shield, with the fraction of the damage that gets through. */
	private static final Map<LivingEntity, Float> THROUGH_SHIELD = new WeakHashMap<>();
	/** Targets of a light arrow, with their lowered knockback until their next tick. */
	private static final Map<LivingEntity, AttributeInstance> LIGHTENED = new WeakHashMap<>();

	private CombatEvents() {
	}

	public static void register() {
		MinecraftForge.EVENT_BUS.addListener(CombatEvents::onAttack);
		MinecraftForge.EVENT_BUS.addListener(CombatEvents::onCriticalHit);
		MinecraftForge.EVENT_BUS.addListener(CombatEvents::onLivingAttack);
		MinecraftForge.EVENT_BUS.addListener(CombatEvents::onShieldBlock);
		MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, CombatEvents::onLightArrow);
		MinecraftForge.EVENT_BUS.addListener(CombatEvents::onLivingTick);
		MinecraftForge.EVENT_BUS.addListener(CombatEvents::onHurt);
		MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, CombatEvents::onArmorWear);
		MinecraftForge.EVENT_BUS.addListener(CombatEvents::onDamage);
		MinecraftForge.EVENT_BUS.addListener(CombatEvents::onServerTick);
	}

	private static void onAttack(AttackEntityEvent event) {
		AttackContext.begin(event.getEntity(), event.getTarget());
	}

	private static void onCriticalHit(CriticalHitEvent event) {
		Player player = event.getEntity();
		if (!(player.getMainHandItem().getItem() instanceof WeaponItem weapon)) {
			return;
		}
		WeaponAbility ability = weapon.type().ability();
		if (!ability.allowsCrits()) {
			event.setResult(Event.Result.DENY);
			return;
		}
		if (ability instanceof SabreAbility && !event.isVanillaCritical() && event.getTarget() instanceof LivingEntity
				&& !player.level.isClientSide() && SabreAbility.consumeRiposte(player)) {
			event.setResult(Event.Result.ALLOW);
			event.setDamageModifier(CRIT_MULTIPLIER);
		}
		boolean crit = event.getResult() == Event.Result.ALLOW
				|| event.getResult() == Event.Result.DEFAULT && event.isVanillaCritical();
		if (crit) {
			AttackContext.markCrit(player);
		}
	}

	/** Minimum charge: a weak hit with some weapons does nothing (vanilla then plays the no-damage sound). */
	private static void onLivingAttack(LivingAttackEvent event) {
		DamageSource source = event.getSource();
		if (source.getEntity() instanceof Player player && source.getDirectEntity() == player
				&& "player".equals(source.getMsgId()) && AttackContext.isMainHit(player, event.getEntity())
				&& AttackContext.charge(player) < WeaponItem.abilityOf(player.getMainHandItem()).minimumCharge()) {
			event.setCanceled(true);
		}
	}

	/** Over the shield: a hit the shield would block deals part of its damage instead of none. */
	private static void onShieldBlock(ShieldBlockEvent event) {
		LivingEntity attacker = meleeAttacker(event.getDamageSource());
		LivingEntity target = event.getEntity();
		if (attacker == null) {
			return;
		}
		float pass = Math.min(1, WeaponItem.abilityOf(attacker.getMainHandItem()).shieldPassThrough(attacker));
		if (pass <= 0) {
			return;
		}
		event.setCanceled(true);
		THROUGH_SHIELD.put(target, pass);
		target.level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.CHAIN_HIT, target.getSoundSource(), 1.0f, 0.8f);
	}

	/**
	 * Light Arrows: a horse bow's arrow knocks its target back half as far, Punch included. The knockback
	 * in {@code hurt} and the arrow's Punch push afterwards both scale with {@code 1 - knockback resistance},
	 * so the target's resistance is raised to halve that, from the start of the hit to its next tick.
	 * Lowest priority, so a hit another listener cancels changes nothing.
	 */
	private static void onLightArrow(LivingAttackEvent event) {
		LivingEntity target = event.getEntity();
		if (target.level.isClientSide() || !HorseBowAbility.isLight(event.getSource().getDirectEntity())) {
			return;
		}
		AttributeInstance resistance = target.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
		if (resistance == null || resistance.getModifier(LIGHT_ARROW_UUID) != null || resistance.getValue() >= 1) {
			return;
		}
		resistance.addTransientModifier(new AttributeModifier(LIGHT_ARROW_UUID, "Light arrow",
				(1 - resistance.getValue()) * 0.5, AttributeModifier.Operation.ADDITION));
		LIGHTENED.put(target, resistance);
	}

	private static void onLivingTick(LivingEvent.LivingTickEvent event) {
		if (!LIGHTENED.isEmpty()) {
			AttributeInstance resistance = LIGHTENED.remove(event.getEntity());
			if (resistance != null) {
				resistance.removeModifier(LIGHT_ARROW_UUID);
			}
		}
	}

	private static void onHurt(LivingHurtEvent event) {
		DamageSource source = event.getSource();
		LivingEntity target = event.getEntity();
		Float pass = THROUGH_SHIELD.remove(target);
		if (!(source.getEntity() instanceof Player player) || source.getDirectEntity() != player
				|| !"player".equals(source.getMsgId()) || player.level.isClientSide()
				|| !(player.getMainHandItem().getItem() instanceof WeaponItem weapon)
				|| !AttackContext.takeMainHit(player, target)) {
			if (pass != null) {
				event.setAmount(event.getAmount() * pass);
			}
			return;
		}
		WeaponAbility ability = weapon.type().ability();
		float bonus = ability.attackDamageBonus(weapon, player, target, event.getAmount());
		if (bonus != 0) {
			event.setAmount(Math.max(0, event.getAmount() + bonus));
		}
		if (pass != null) {
			event.setAmount(event.getAmount() * pass);
		}

		float pierce = ability.armorPierce(weapon.material());
		AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
		if (pierce > 0 && armor != null && armor.getModifier(ARMOR_CRUSH_UUID) == null) {
			armor.addTransientModifier(new AttributeModifier(ARMOR_CRUSH_UUID, "Armor crush", -pierce,
					AttributeModifier.Operation.MULTIPLY_TOTAL));
			CRUSHED.put(target, armor);
		}
	}

	/**
	 * Armor wear: the hit costs the target's armor more durability. Only players' armor wears in vanilla,
	 * through {@code Inventory.hurtArmor}, which takes {@code max(1, damage / 4)} from each piece. Runs
	 * last, so the amount is the one vanilla's own call right after this event gets; this adds the rest.
	 */
	private static void onArmorWear(LivingHurtEvent event) {
		DamageSource source = event.getSource();
		LivingEntity attacker = meleeAttacker(source);
		float amount = event.getAmount();
		if (!(event.getEntity() instanceof Player target) || attacker == null || amount <= 0
				|| source.isBypassArmor()
				|| !(attacker.getMainHandItem().getItem() instanceof WeaponItem weapon)) {
			return;
		}
		float wear = weapon.type().ability().armorWearMultiplier(weapon.material());
		int vanilla = Math.max(1, (int) (amount / 4));
		int total = Math.max(1, (int) (amount * wear / 4));
		if (total > vanilla) {
			target.getInventory().hurtArmor(source, (total - vanilla) * 4, Inventory.ALL_ARMOR_SLOTS);
		}
	}

	private static void onDamage(LivingDamageEvent event) {
		restoreArmor(event.getEntity());
	}

	/** In case a hit was cancelled between the events, nothing keeps crushed armor or a shield pass past the tick. */
	private static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		THROUGH_SHIELD.clear();
		if (!CRUSHED.isEmpty()) {
			for (LivingEntity entity : CRUSHED.keySet().toArray(new LivingEntity[0])) {
				restoreArmor(entity);
			}
		}
	}

	private static void restoreArmor(LivingEntity entity) {
		AttributeInstance armor = CRUSHED.remove(entity);
		if (armor != null) {
			armor.removeModifier(ARMOR_CRUSH_UUID);
		}
	}

	/** The attacker of a plain melee hit (no projectiles or thorns), or {@code null}. */
	private static LivingEntity meleeAttacker(DamageSource source) {
		if (("player".equals(source.getMsgId()) || "mob".equals(source.getMsgId()))
				&& source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker) {
			return attacker;
		}
		return null;
	}
}
