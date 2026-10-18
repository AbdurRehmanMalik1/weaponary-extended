package net.firstwemine1.weaponry.ability;

import net.firstwemine1.weaponry.combat.Motion;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponMaterial;
import net.firstwemine1.weaponry.item.WeaponType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Event wiring for abilities, plus small helpers they share. */
public final class Abilities {
	private Abilities() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Motion.tick(server);
			server.getPlayerList().getPlayers().forEach(TwoHanded::tick);
			HookSwordAbility.tick(server);
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(SabreAbility::allowDamage);

		UseItemCallback.EVENT.register(TwoHanded::onUseItem);
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> TwoHanded.onUse(player, hand));
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> TwoHanded.onUse(player, hand));
	}

	/** Puts every material of a weapon type on cooldown, so swapping tiers doesn't skip it. */
	static void cooldown(Player player, WeaponType type, int ticks) {
		for (WeaponMaterial material : WeaponMaterial.values()) {
			player.getCooldowns().addCooldown(ModItems.get(type, material).getDefaultInstance(), ticks);
		}
	}

	/** Knock {@code target} back along the attacker's facing, the way vanilla melee knockback does. */
	static void knockbackFrom(LivingEntity target, LivingEntity attacker, double strength) {
		float yaw = attacker.getYRot() * Mth.DEG_TO_RAD;
		knockback(target, attacker, strength, Mth.sin(yaw), -Mth.cos(yaw));
	}

	/**
	 * {@code LivingEntity.knockback} with the attacker's melee damage source and no damage, which this
	 * version passes along with every knockback. Pushes away from ({@code xd}, {@code zd}).
	 */
	static void knockback(LivingEntity target, LivingEntity attacker, double strength, double xd, double zd) {
		DamageSource source = attacker instanceof Player player
				? player.damageSources().playerAttack(player)
				: attacker.damageSources().mobAttack(attacker);
		target.knockback(strength, xd, zd, source, 0.0f);
	}

	/** Launch {@code target} horizontally toward {@code toward}, reduced by its knockback resistance. */
	static void pull(LivingEntity target, Entity toward, double speed, double lift) {
		double resistance = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
		Vec3 pull = toward.position().subtract(target.position()).multiply(1, 0, 1);
		if (pull.lengthSqr() > 1.0e-6 && resistance < 1.0) {
			pull = pull.normalize().scale(speed * (1.0 - resistance));
			target.push(pull.x, lift, pull.z);
			// push() only syncs to watchers; a player moves itself, so its own client needs the motion too.
			target.syncVelocity = true;
		}
	}

	/** The living entity under the crosshair within the player's reach, not through walls. */
	static LivingEntity targetInReach(Player player) {
		double range = player.entityInteractionRange();
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1.0f).scale(range));
		HitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (block.getType() != HitResult.Type.MISS) {
			end = block.getLocation();
		}
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end,
				player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0),
				entity -> entity instanceof LivingEntity && !entity.isSpectator() && entity.isPickable(),
				eye.distanceToSqr(end));
		Entity entity = hit != null ? hit.getEntity() : null;
		return entity instanceof LivingEntity living ? living : null;
	}
}
