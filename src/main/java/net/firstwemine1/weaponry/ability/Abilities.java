package net.firstwemine1.weaponry.ability;

import java.util.Optional;

import net.firstwemine1.weaponry.combat.Motion;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponMaterial;
import net.firstwemine1.weaponry.item.WeaponType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
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

		UseItemCallback.EVENT.register(TwoHanded::onUseItem);
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> TwoHanded.onUse(player, hand));
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> TwoHanded.onUse(player, hand));
	}

	/** Puts every material of a weapon type on cooldown, so swapping tiers doesn't skip it. */
	static void cooldown(Player player, WeaponType type, int ticks) {
		for (WeaponMaterial material : WeaponMaterial.values()) {
			player.getCooldowns().addCooldown(ModItems.get(type, material), ticks);
		}
	}

	/** Knock {@code target} back along the attacker's facing, the way vanilla melee knockback does. */
	static void knockbackFrom(LivingEntity target, Entity attacker, double strength) {
		float yaw = attacker.yRot * ((float) Math.PI / 180.0f);
		target.knockback((float) strength, Mth.sin(yaw), -Mth.cos(yaw));
	}

	/** Launch {@code target} horizontally toward {@code toward}, reduced by its knockback resistance. */
	static void pull(LivingEntity target, Entity toward, double speed, double lift) {
		double resistance = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
		Vec3 pull = toward.position().subtract(target.position()).multiply(1, 0, 1);
		if (pull.lengthSqr() > 1.0e-6 && resistance < 1.0) {
			pull = pull.normalize().scale(speed * (1.0 - resistance));
			target.push(pull.x, lift, pull.z);
			target.hurtMarked = true;
		}
	}

	/**
	 * The living entity under the crosshair within the held weapon's reach, not through walls. There is
	 * no reach attribute in this version, so it uses the weapon's listed reach (halberd 4, hook sword 3).
	 */
	static LivingEntity targetInReach(Player player) {
		return targetInReach(player, 0);
	}

	/** As {@link #targetInReach(Player)}, reaching {@code extraReach} blocks further. */
	static LivingEntity targetInReach(Player player, double extraReach) {
		double range = (player.getMainHandItem().getItem() instanceof WeaponItem
				? ((WeaponItem) player.getMainHandItem().getItem()).type().reach()
				: 3.0) + extraReach;
		Vec3 eye = player.getEyePosition(1.0f);
		Vec3 end = eye.add(player.getViewVector(1.0f).scale(range));
		HitResult block = player.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (block.getType() != HitResult.Type.MISS) {
			end = block.getLocation();
		}
		AABB area = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
		return closestAlongRay(player, eye, end, area);
	}

	/**
	 * The living entity the ray from {@code eye} to {@code end} hits first. Same logic as
	 * {@code ProjectileUtil.getEntityHitResult(Entity, ...)}, which is client-only in 1.16.5 and so
	 * missing on a dedicated server.
	 */
	private static LivingEntity closestAlongRay(Player player, Vec3 eye, Vec3 end, AABB area) {
		LivingEntity closest = null;
		double closestDistSqr = Double.MAX_VALUE;
		for (Entity entity : player.level.getEntities(player, area,
				e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable())) {
			AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
			if (box.contains(eye)) {
				return (LivingEntity) entity;
			}
			Optional<Vec3> hit = box.clip(eye, end);
			if (hit.isPresent() && entity.getRootVehicle() != player.getRootVehicle()) {
				double distSqr = eye.distanceToSqr(hit.get());
				if (distSqr < closestDistSqr) {
					closest = (LivingEntity) entity;
					closestDistSqr = distSqr;
				}
			}
		}
		return closest;
	}
}
