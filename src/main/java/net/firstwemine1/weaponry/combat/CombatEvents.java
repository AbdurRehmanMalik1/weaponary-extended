package net.firstwemine1.weaponry.combat;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import net.firstwemine1.weaponry.ability.SabreAbility;
import net.firstwemine1.weaponry.item.WeaponAbility;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
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
 *   <li>{@code LivingAttackEvent}: a hit below the weapon's minimum charge does nothing; a hit that gets
 *       past a raised shield hides the shield from the block check that follows.</li>
 *   <li>{@code LivingHurtEvent} on the attack's target: the shield comes back, the ability damage bonus,
 *       the part of the hit that got past the shield, and armor crush lowers the target's armor (it is
 *       applied after this event). Last of all, armor wear adds to the durability the hit costs.</li>
 *   <li>{@code LivingDamageEvent}: armor crush puts the armor back.</li>
 * </ol>
 */
public final class CombatEvents {
	private static final UUID ARMOR_CRUSH_UUID = UUID.fromString("b4f7d3c2-8e1a-4b6c-9d2e-3f4a5b6c7d8e");
	private static final float CRIT_MULTIPLIER = 1.5f;

	/** Targets whose armor is crushed for the hit in progress, until LivingDamageEvent restores it. */
	private static final Map<LivingEntity, ModifiableAttributeInstance> CRUSHED = new WeakHashMap<>();
	/** Targets hit through their shield, with the fraction of the damage that gets through. */
	private static final Map<LivingEntity, Float> THROUGH_SHIELD = new WeakHashMap<>();
	/** The raised shield of a target hit through it, put back once the block check is past. */
	private static final Map<LivingEntity, ItemStack> HIDDEN_SHIELD = new WeakHashMap<>();

	private CombatEvents() {
	}

	public static void register() {
		MinecraftForge.EVENT_BUS.addListener(CombatEvents::onAttack);
		MinecraftForge.EVENT_BUS.addListener(CombatEvents::onCriticalHit);
		MinecraftForge.EVENT_BUS.addListener(CombatEvents::onLivingAttack);
		MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, CombatEvents::onPassShield);
		MinecraftForge.EVENT_BUS.addListener(CombatEvents::onHurt);
		MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, CombatEvents::onArmorWear);
		MinecraftForge.EVENT_BUS.addListener(CombatEvents::onDamage);
		MinecraftForge.EVENT_BUS.addListener(CombatEvents::onLivingUpdate);
		MinecraftForge.EVENT_BUS.addListener(CombatEvents::onServerTick);
	}

	private static void onAttack(AttackEntityEvent event) {
		AttackContext.begin(event.getPlayer(), event.getTarget());
	}

	private static void onCriticalHit(CriticalHitEvent event) {
		PlayerEntity player = event.getPlayer();
		if (!(player.getMainHandItem().getItem() instanceof WeaponItem)) {
			return;
		}
		WeaponAbility ability = ((WeaponItem) player.getMainHandItem().getItem()).type().ability();
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
		if (!(source.getEntity() instanceof PlayerEntity) || source.getDirectEntity() != source.getEntity()
				|| !"player".equals(source.getMsgId())) {
			return;
		}
		PlayerEntity player = (PlayerEntity) source.getEntity();
		if (AttackContext.isMainHit(player, event.getEntityLiving())
				&& AttackContext.charge(player) < WeaponItem.abilityOf(player.getMainHandItem()).minimumCharge()) {
			event.setCanceled(true);
		}
	}

	/**
	 * Over the shield: a hit the shield would block deals part of its damage instead of none. There is no
	 * shield event in this version, so when the hit would be blocked, the target's raised shield
	 * ({@code useItem}, public through the access transformer) is hidden until {@code LivingHurtEvent},
	 * which {@code hurt} reaches after its block check. Lowest priority, so a cancelled hit hides nothing.
	 */
	private static void onPassShield(LivingAttackEvent event) {
		LivingEntity target = event.getEntityLiving();
		LivingEntity attacker = meleeAttacker(event.getSource());
		if (attacker == null || target.level.isClientSide() || event.getAmount() <= 0
				|| !target.isDamageSourceBlocked(event.getSource())) {
			return;
		}
		float pass = Math.min(1, WeaponItem.abilityOf(attacker.getMainHandItem()).shieldPassThrough(attacker));
		if (pass <= 0) {
			return;
		}
		THROUGH_SHIELD.put(target, pass);
		HIDDEN_SHIELD.put(target, target.useItem);
		target.useItem = ItemStack.EMPTY;
		target.level.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.CHAIN_HIT, target.getSoundSource(), 1.0f, 0.8f);
	}

	private static void onHurt(LivingHurtEvent event) {
		DamageSource source = event.getSource();
		LivingEntity target = event.getEntityLiving();
		showShield(target);
		Float pass = THROUGH_SHIELD.remove(target);
		if (!(source.getEntity() instanceof PlayerEntity) || source.getDirectEntity() != source.getEntity()
				|| !"player".equals(source.getMsgId())) {
			throughShield(event, pass);
			return;
		}
		PlayerEntity player = (PlayerEntity) source.getEntity();
		if (player.level.isClientSide() || !(player.getMainHandItem().getItem() instanceof WeaponItem)
				|| !AttackContext.takeMainHit(player, target)) {
			throughShield(event, pass);
			return;
		}
		WeaponItem weapon = (WeaponItem) player.getMainHandItem().getItem();
		WeaponAbility ability = weapon.type().ability();
		float bonus = ability.attackDamageBonus(weapon, player, target, event.getAmount());
		if (bonus != 0) {
			event.setAmount(Math.max(0, event.getAmount() + bonus));
		}
		throughShield(event, pass);

		float pierce = ability.armorPierce(weapon.material());
		ModifiableAttributeInstance armor = target.getAttribute(Attributes.ARMOR);
		if (pierce > 0 && armor != null && armor.getModifier(ARMOR_CRUSH_UUID) == null) {
			armor.addTransientModifier(new AttributeModifier(ARMOR_CRUSH_UUID, "Armor crush", -pierce,
					AttributeModifier.Operation.MULTIPLY_TOTAL));
			CRUSHED.put(target, armor);
		}
	}

	private static void throughShield(LivingHurtEvent event, Float pass) {
		if (pass != null) {
			event.setAmount(event.getAmount() * pass);
		}
	}

	/**
	 * Armor wear: the hit costs the target's armor more durability. Only players' armor wears in vanilla,
	 * through {@code PlayerInventory.hurtArmor}, which takes {@code max(1, damage / 4)} from each piece. Runs
	 * last, so the amount is the one vanilla's own call right after this event gets; this adds the rest.
	 */
	private static void onArmorWear(LivingHurtEvent event) {
		DamageSource source = event.getSource();
		LivingEntity attacker = meleeAttacker(source);
		float amount = event.getAmount();
		if (!(event.getEntityLiving() instanceof PlayerEntity) || attacker == null || amount <= 0
				|| source.isBypassArmor() || !(attacker.getMainHandItem().getItem() instanceof WeaponItem)) {
			return;
		}
		WeaponItem weapon = (WeaponItem) attacker.getMainHandItem().getItem();
		float wear = weapon.type().ability().armorWearMultiplier(weapon.material());
		int vanilla = Math.max(1, (int) (amount / 4));
		int total = Math.max(1, (int) (amount * wear / 4));
		if (total > vanilla) {
			((PlayerEntity) event.getEntityLiving()).inventory.hurtArmor(source, (total - vanilla) * 4);
		}
	}

	private static void onDamage(LivingDamageEvent event) {
		restoreArmor(event.getEntityLiving());
	}

	/**
	 * A hit can stop before {@code LivingHurtEvent} (hurt cooldown, a later cancel). The shield must be back
	 * before the target's tick, or vanilla sees it switched items and lowers it.
	 */
	private static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
		if (!HIDDEN_SHIELD.isEmpty()) {
			showShield(event.getEntityLiving());
		}
	}

	/** In case a hit was cancelled between the events, nothing keeps crushed armor or a shield pass past the tick. */
	private static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		THROUGH_SHIELD.clear();
		if (!HIDDEN_SHIELD.isEmpty()) {
			for (LivingEntity entity : HIDDEN_SHIELD.keySet().toArray(new LivingEntity[0])) {
				showShield(entity);
			}
		}
		if (!CRUSHED.isEmpty()) {
			for (LivingEntity entity : CRUSHED.keySet().toArray(new LivingEntity[0])) {
				restoreArmor(entity);
			}
		}
	}

	private static void showShield(LivingEntity entity) {
		ItemStack shield = HIDDEN_SHIELD.remove(entity);
		if (shield != null && entity.useItem.isEmpty()) {
			entity.useItem = shield;
		}
	}

	private static void restoreArmor(LivingEntity entity) {
		ModifiableAttributeInstance armor = CRUSHED.remove(entity);
		if (armor != null) {
			armor.removeModifier(ARMOR_CRUSH_UUID);
		}
	}

	/**
	 * The attacker of a plain melee hit (no projectiles or thorns), or {@code null}. There are no damage types
	 * yet: melee hits are the "player" and "mob" sources.
	 */
	private static LivingEntity meleeAttacker(DamageSource source) {
		Entity attacker = source.getEntity();
		if (("player".equals(source.getMsgId()) || "mob".equals(source.getMsgId()))
				&& attacker instanceof LivingEntity && source.getDirectEntity() == attacker) {
			return (LivingEntity) attacker;
		}
		return null;
	}
}
