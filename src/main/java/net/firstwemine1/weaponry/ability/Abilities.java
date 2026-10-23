package net.firstwemine1.weaponry.ability;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.firstwemine1.weaponry.combat.Motion;
import net.firstwemine1.weaponry.entity.RopeDart;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.util.Mth;
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
		NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
			MinecraftServer server = event.getServer();
			Motion.tick(server);
			server.getPlayerList().getPlayers().forEach(TwoHanded::tick);
			HookSwordAbility.tick(server);
		});
		// The sabre parry: cancel a frontal melee hit on a parrying player before it does anything.
		NeoForge.EVENT_BUS.addListener((LivingIncomingDamageEvent event) -> {
			if (!event.getEntity().level().isClientSide()
					&& !SabreAbility.allowDamage(event.getEntity(), event.getSource(), event.getAmount())) {
				event.setCanceled(true);
			}
		});
		// The rope dart's PvP way out. A parried hit doesn't count, as on Fabric (where the parry stops the event).
		NeoForge.EVENT_BUS.addListener((LivingIncomingDamageEvent event) -> {
			if (!event.getEntity().level().isClientSide() && !event.isCanceled()) {
				RopeDart.onDamage(event.getEntity(), event.getSource(), event.getAmount());
			}
		});

		// Two-handed weapons block the off hand (item use, block use, entity use).
		NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem event) -> {
			if (TwoHanded.check(event.getEntity(), event.getHand())) {
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.FAIL);
			}
		});
		NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock event) -> {
			if (TwoHanded.check(event.getEntity(), event.getHand())) {
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.FAIL);
			}
		});
		NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract event) -> {
			if (TwoHanded.check(event.getEntity(), event.getHand())) {
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.FAIL);
			}
		});
	}

	/** Puts every material of a weapon type on cooldown, so swapping tiers doesn't skip it. */
	static void cooldown(Player player, WeaponType type, int ticks) {
		for (WeaponItem item : ModItems.variants(type)) {
			player.getCooldowns().addCooldown(item, ticks);
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
		double range = player.entityInteractionRange() + extraReach;
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
