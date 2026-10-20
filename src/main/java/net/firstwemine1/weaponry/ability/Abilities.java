package net.firstwemine1.weaponry.ability;

import net.firstwemine1.weaponry.combat.Motion;
import net.firstwemine1.weaponry.entity.RopeDart;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponMaterial;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Event wiring for abilities, plus small helpers they share. */
public final class Abilities {
	private Abilities() {
	}

	public static void register() {
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END && ServerLifecycleHooks.getCurrentServer() != null) {
				MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
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
		// The rope dart's PvP way out. Not called for a hit the parry already cancelled.
		MinecraftForge.EVENT_BUS.addListener((LivingAttackEvent event) -> {
			if (!event.getEntityLiving().level.isClientSide()) {
				RopeDart.onDamage(event.getEntityLiving(), event.getSource(), event.getAmount());
			}
		});

		// Two-handed weapons block the off hand (item use, block use, entity use).
		MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem event) -> {
			if (TwoHanded.check(event.getPlayer(), event.getHand())) {
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.FAIL);
			}
		});
		MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock event) -> {
			if (TwoHanded.check(event.getPlayer(), event.getHand())) {
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.FAIL);
			}
		});
		MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract event) -> {
			if (TwoHanded.check(event.getPlayer(), event.getHand())) {
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.FAIL);
			}
		});
	}

	/** Puts every material of a weapon type on cooldown, so swapping tiers doesn't skip it. */
	static void cooldown(Player player, WeaponType type, int ticks) {
		for (WeaponMaterial material : WeaponMaterial.values()) {
			player.getCooldowns().addCooldown(ModItems.get(type, material), ticks);
		}
	}

	/** Knock {@code target} back along the attacker's facing, the way vanilla melee knockback does. */
	static void knockbackFrom(LivingEntity target, Entity attacker, double strength) {
		float yaw = attacker.getYRot() * Mth.DEG_TO_RAD;
		target.knockback(strength, Mth.sin(yaw), -Mth.cos(yaw));
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

	/** The living entity under the crosshair within the player's reach, not through walls. */
	static LivingEntity targetInReach(Player player) {
		return targetInReach(player, 0);
	}

	/** As {@link #targetInReach(Player)}, reaching {@code extraReach} blocks further. */
	static LivingEntity targetInReach(Player player, double extraReach) {
		double range = player.getAttackRange() + extraReach;
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1.0f).scale(range));
		HitResult block = player.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
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
