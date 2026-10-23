package net.firstwemine1.weaponry.ability;

import java.util.Optional;

import net.firstwemine1.weaponry.combat.Motion;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponMaterial;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.ActionResultType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

/** Event wiring for abilities, plus small helpers they share. */
public final class Abilities {
	private Abilities() {
	}

	public static void register() {
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
			MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			if (event.phase == TickEvent.Phase.END && server != null) {
				Motion.tick(server);
				server.getPlayerList().getPlayers().forEach(TwoHanded::tick);
				HookSwordAbility.tick(server);
			}
		});
		// The sabre parry: cancel a frontal melee hit on a parrying player before it does anything.
		MinecraftForge.EVENT_BUS.addListener((LivingAttackEvent event) -> {
			if (!event.getEntityLiving().level.isClientSide()
					&& !SabreAbility.allowDamage(event.getEntityLiving(), event.getSource(), event.getAmount())) {
				event.setCanceled(true);
			}
		});

		// Two-handed weapons block the off hand (item use, block use, entity use).
		MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem event) -> {
			if (TwoHanded.check(event.getPlayer(), event.getHand())) {
				event.setCanceled(true);
				event.setCancellationResult(ActionResultType.FAIL);
			}
		});
		MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock event) -> {
			if (TwoHanded.check(event.getPlayer(), event.getHand())) {
				event.setCanceled(true);
				event.setCancellationResult(ActionResultType.FAIL);
			}
		});
		MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract event) -> {
			if (TwoHanded.check(event.getPlayer(), event.getHand())) {
				event.setCanceled(true);
				event.setCancellationResult(ActionResultType.FAIL);
			}
		});
	}

	/** Puts every material of a weapon type on cooldown, so swapping tiers doesn't skip it. */
	static void cooldown(PlayerEntity player, WeaponType type, int ticks) {
		for (WeaponMaterial material : WeaponMaterial.values()) {
			player.getCooldowns().addCooldown(ModItems.get(type, material), ticks);
		}
	}

	/** Knock {@code target} back along the attacker's facing, the way vanilla melee knockback does. */
	static void knockbackFrom(LivingEntity target, Entity attacker, double strength) {
		float yaw = attacker.yRot * ((float) Math.PI / 180.0f);
		target.knockback((float) strength, MathHelper.sin(yaw), -MathHelper.cos(yaw));
	}

	/** Launch {@code target} horizontally toward {@code toward}, reduced by its knockback resistance. */
	static void pull(LivingEntity target, Entity toward, double speed, double lift) {
		double resistance = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
		Vector3d pull = toward.position().subtract(target.position()).multiply(1, 0, 1);
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
	static LivingEntity targetInReach(PlayerEntity player) {
		double range = player.getMainHandItem().getItem() instanceof WeaponItem
				? ((WeaponItem) player.getMainHandItem().getItem()).type().reach()
				: 3.0;
		Vector3d eye = player.getEyePosition(1.0f);
		Vector3d end = eye.add(player.getViewVector(1.0f).scale(range));
		RayTraceResult block = player.level.clip(new RayTraceContext(eye, end, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player));
		if (block.getType() != RayTraceResult.Type.MISS) {
			end = block.getLocation();
		}
		AxisAlignedBB area = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
		return closestAlongRay(player, eye, end, area);
	}

	/**
	 * The living entity the ray from {@code eye} to {@code end} hits first. Same logic as
	 * {@code ProjectileUtil.getEntityHitResult(Entity, ...)}, which is client-only in 1.16.5 and so
	 * missing on a dedicated server.
	 */
	private static LivingEntity closestAlongRay(PlayerEntity player, Vector3d eye, Vector3d end, AxisAlignedBB area) {
		LivingEntity closest = null;
		double closestDistSqr = Double.MAX_VALUE;
		for (Entity entity : player.level.getEntities(player, area,
				e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable())) {
			AxisAlignedBB box = entity.getBoundingBox().inflate(entity.getPickRadius());
			if (box.contains(eye)) {
				return (LivingEntity) entity;
			}
			Optional<Vector3d> hit = box.clip(eye, end);
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
