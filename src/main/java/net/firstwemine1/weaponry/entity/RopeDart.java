package net.firstwemine1.weaponry.entity;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.firstwemine1.weaponry.combat.Motion;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponMaterial;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The rope dart's dart, tied to its thrower by a rope. It never drops as an item: the rope dart stays
 * in the thrower's hand while the dart is out, and "reeling in" just removes the dart.
 *
 * <ul>
 *   <li><b>Flying:</b> like an arrow, until the rope runs out (then it reels back).</li>
 *   <li><b>Tethered:</b> it hit a living target and rides on it. The target can't get further than
 *       the rope length from the thrower, and holding use drags it in.</li>
 *   <li><b>Anchored:</b> it stuck in a block. The thrower hangs on the rope, climbs with jump and lowers
 *       with sneak. The rope only limits distance; falling and fall damage are vanilla.</li>
 * </ul>
 *
 * <p>Players move themselves, so the rope's pull on its own thrower runs on that player's client. Mobs
 * move on the server, and a tethered player is pushed with velocity packets.
 *
 * <p>In this version {@code AbstractArrow} keeps no item stack, so the rope dart is stored here, as
 * {@code ThrownWeapon} does.
 */
public class RopeDart extends AbstractArrow {
	public static final double ROPE_LENGTH = 12.0;
	/** A tether stretched past this snaps. */
	public static final double SNAP_LENGTH = 16.0;
	private static final int TETHER_TICKS = 100;
	private static final double DRAG_SPEED = 0.35;
	/** Dragging stops this close, so the target isn't pulled into you. */
	private static final double DRAG_STOP = 2.0;
	private static final int DRAG_TICKS_PER_DURABILITY = 10;
	/** Ladder climbing speed, blocks per tick. */
	private static final double CLIMB_SPEED = 0.15;
	private static final double MIN_ROPE = 1.0;
	/** Damage from the thrower in one hit that frees a tethered player. */
	private static final float PVP_BREAK_DAMAGE = 4.0f;

	private static final EntityDataAccessor<ItemStack> DATA_ITEM =
			SynchedEntityData.defineId(RopeDart.class, EntityDataSerializers.ITEM_STACK);
	/** Entity id of the tethered target, or -1. */
	private static final EntityDataAccessor<Integer> DATA_TETHER =
			SynchedEntityData.defineId(RopeDart.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_ANCHORED =
			SynchedEntityData.defineId(RopeDart.class, EntityDataSerializers.BOOLEAN);

	private ItemStack weapon = ItemStack.EMPTY;
	private float damage;
	private int tetherTicks;
	private int dragTicks;
	/** Client-side, on the thrower's own client: the current rope length while anchored. */
	private double ropeLength = -1;

	public RopeDart(EntityType<? extends RopeDart> type, Level level) {
		super(type, level);
	}

	public RopeDart(Level level, Player owner, ItemStack stack, float damage) {
		super(ModEntities.ROPE_DART.get(), owner, level);
		this.weapon = stack.copy();
		this.weapon.setCount(1);
		this.damage = damage;
		this.pickup = Pickup.DISALLOWED;
		this.entityData.set(DATA_ITEM, weapon.copy());
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DATA_ITEM, ItemStack.EMPTY);
		entityData.define(DATA_TETHER, -1);
		entityData.define(DATA_ANCHORED, false);
	}

	/** The rope dart this was thrown from, synced so the client can render it. */
	public ItemStack getItem() {
		return entityData.get(DATA_ITEM);
	}

	public boolean isAnchored() {
		return entityData.get(DATA_ANCHORED);
	}

	public boolean isTethered() {
		return entityData.get(DATA_TETHER) >= 0;
	}

	@Nullable
	public LivingEntity tetherTarget() {
		int id = entityData.get(DATA_TETHER);
		return id >= 0 && level.getEntity(id) instanceof LivingEntity living ? living : null;
	}

	/** The dart this player has out, on either side, or {@code null}. */
	@Nullable
	public static RopeDart of(Player player) {
		List<RopeDart> darts = player.level.getEntitiesOfClass(RopeDart.class,
				player.getBoundingBox().inflate(SNAP_LENGTH + 4.0), dart -> dart.getOwner() == player && !dart.isRemoved());
		return darts.isEmpty() ? null : darts.get(0);
	}

	public static boolean isRopeDart(ItemStack stack) {
		return WeaponItem.is(stack, WeaponType.ROPE_DART);
	}

	/** Pulls the dart back in. */
	public void reelIn() {
		if (!isRemoved()) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0f, 1.2f);
			discard();
		}
	}

	private void snap() {
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.LEASH_KNOT_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
		discard();
	}

	@Override
	public void tick() {
		Player owner = getOwner() instanceof Player player ? player : null;
		if (!level.isClientSide() && !ownerStillHolds(owner)) {
			reelIn();
			return;
		}

		if (isTethered()) {
			setOldPosAndRot();
			LivingEntity target = tetherTarget();
			if (target != null) {
				setPos(target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ());
			}
			if (!level.isClientSide()) {
				tickTether(owner, target);
			}
			return;
		}

		super.tick();
		if (isRemoved()) {
			return;
		}
		if (level.isClientSide()) {
			if (isAnchored() && owner != null && owner.isControlledByLocalInstance()) {
				hang(owner);
			} else {
				ropeLength = -1;
			}
			return;
		}

		double distance = owner.distanceTo(this);
		if (isAnchored()) {
			// The anchor block was broken (the arrow started falling), or the thrower got away somehow.
			if (!inGround || distance > SNAP_LENGTH) {
				reelIn();
				return;
			}
			// A climb upward starts a fresh fall, like a jump does. Otherwise falls build up as usual.
			if (Motion.velocity(owner).y > 0.02) {
				owner.resetFallDistance();
			}
		} else if (inGround) {
			entityData.set(DATA_ANCHORED, true);
			hurtHeldDart(owner, 1);
		} else if (distance > ROPE_LENGTH) {
			// The rope ran out: the dart falls short and comes back.
			reelIn();
		}
	}

	private static boolean ownerStillHolds(@Nullable Player owner) {
		return owner != null && owner.isAlive() && !owner.isSpectator()
				&& (isRopeDart(owner.getMainHandItem()) || isRopeDart(owner.getOffhandItem()));
	}

	// --- tether (server) ---

	private void tickTether(Player owner, @Nullable LivingEntity target) {
		if (target == null || !target.isAlive() || target.level != level || ++tetherTicks > TETHER_TICKS) {
			reelIn();
			return;
		}
		double distance = owner.distanceTo(target);
		if (distance > SNAP_LENGTH) {
			snap();
			return;
		}

		boolean dragging = owner.isUsingItem() && isRopeDart(owner.getUseItem());
		if (dragging && distance > DRAG_STOP) {
			if (heavy(target)) {
				// Too heavy to move: reeling in pulls the thrower to it instead.
				push(owner, target.position().subtract(owner.position()).normalize().scale(DRAG_SPEED));
			} else {
				double resistance = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
				push(target, owner.position().subtract(target.position()).normalize().scale(DRAG_SPEED * (1.0 - resistance)));
			}
			if (++dragTicks % DRAG_TICKS_PER_DURABILITY == 0 && !hurtHeldDart(owner, 1)) {
				reelIn();
			}
		} else if (distance > ROPE_LENGTH) {
			// Held at the end of the rope, like a lead.
			Vec3 back = owner.position().subtract(target.position()).normalize();
			push(target, back.scale(Math.min(0.6, (distance - ROPE_LENGTH) * 0.4)));
		}
	}

	private static void push(LivingEntity entity, Vec3 motion) {
		entity.setDeltaMovement(motion.x, Math.max(entity.getDeltaMovement().y, motion.y), motion.z);
		// Players move themselves: this sends the new motion to their client.
		entity.hurtMarked = true;
	}

	private static boolean heavy(LivingEntity entity) {
		// No wardens before 1.19.
		return entity instanceof IronGolem || entity instanceof Ravager;
	}

	private static boolean boss(Entity entity) {
		return entity instanceof WitherBoss || entity instanceof EnderDragon || entity instanceof EnderDragonPart;
	}

	/** Costs the thrower's rope dart durability; false once it broke. */
	private static boolean hurtHeldDart(Player owner, int amount) {
		InteractionHand hand = owner.isUsingItem() && isRopeDart(owner.getUseItem()) ? owner.getUsedItemHand()
				: isRopeDart(owner.getMainHandItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
		ItemStack stack = owner.getItemInHand(hand);
		stack.hurtAndBreak(amount, owner, broken -> broken.broadcastBreakEvent(hand));
		return !stack.isEmpty();
	}

	/**
	 * PvP way out ({@code LivingAttackEvent}): a tethered player breaks free by hitting the thrower, or when
	 * the thrower hits them for 4 or more.
	 */
	public static void onDamage(LivingEntity hurt, DamageSource source, float amount) {
		if (!(source.getEntity() instanceof Player attacker) || !(hurt instanceof Player)) {
			return;
		}
		for (RopeDart dart : hurt.level.getEntitiesOfClass(RopeDart.class, hurt.getBoundingBox().inflate(SNAP_LENGTH + 4.0))) {
			LivingEntity target = dart.tetherTarget();
			Entity owner = dart.getOwner();
			boolean victimHitsBack = owner == hurt && target == attacker;
			boolean hardHit = owner == attacker && target == hurt && amount >= PVP_BREAK_DAMAGE;
			if (victimHitsBack || hardHit) {
				dart.snap();
			}
		}
	}

	// --- grapple (thrower's client) ---

	private void hang(Player owner) {
		if (ropeLength < 0) {
			ropeLength = Math.min(ROPE_LENGTH, owner.position().distanceTo(position()));
		}
		if (owner.jumping) {
			ropeLength = Math.max(MIN_ROPE, ropeLength - CLIMB_SPEED);
		} else if (owner.isShiftKeyDown()) {
			ropeLength = Math.min(ROPE_LENGTH, ropeLength + CLIMB_SPEED);
		}

		Vec3 fromAnchor = owner.position().subtract(position());
		double distance = fromAnchor.length();
		if (distance <= ropeLength || distance < 1.0e-6) {
			return;
		}
		// A taut rope: drop the outward part of the motion (keeping the swing) and pull back onto the rope.
		Vec3 out = fromAnchor.scale(1.0 / distance);
		Vec3 motion = owner.getDeltaMovement();
		double outward = motion.dot(out);
		if (outward > 0) {
			motion = motion.subtract(out.scale(outward));
		}
		motion = motion.subtract(out.scale(Math.min(0.5, (distance - ropeLength) * 0.5)));
		owner.setDeltaMovement(motion);
	}

	// --- hits ---

	@Override
	protected void onHitEntity(EntityHitResult result) {
		Entity target = result.getEntity();
		Entity owner = getOwner();
		DamageSource source = DamageSource.thrown(this, owner == null ? this : owner);
		float amount = damage;
		if (target instanceof LivingEntity living) {
			amount += EnchantmentHelper.getDamageBonus(weapon, living.getMobType());
		}
		boolean hurt = target.hurt(source, amount);
		if (hurt && target instanceof LivingEntity living && owner instanceof LivingEntity thrower) {
			EnchantmentHelper.doPostHurtEffects(living, thrower);
			EnchantmentHelper.doPostDamageEffects(thrower, living);
		}
		playSound(SoundEvents.TRIDENT_HIT, 1.0f, 1.3f);
		if (level.isClientSide()) {
			return;
		}
		// Bosses shrug it off: the hit lands, but the dart doesn't stick.
		if (!(target instanceof LivingEntity living) || !living.isAlive() || boss(target)) {
			reelIn();
			return;
		}
		entityData.set(DATA_TETHER, living.getId());
		playSound(SoundEvents.LEASH_KNOT_PLACE, 1.0f, 1.0f);
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		super.onHitBlock(result);
		playSound(SoundEvents.LEASH_KNOT_PLACE, 1.0f, 1.0f);
	}

	@Nullable
	@Override
	protected EntityHitResult findHitEntity(Vec3 from, Vec3 to) {
		return isTethered() || inGround ? null : super.findHitEntity(from, to);
	}

	@Override
	protected ItemStack getPickupItem() {
		return weapon.isEmpty() ? new ItemStack(ModItems.get(WeaponType.ROPE_DART, WeaponMaterial.IRON)) : weapon.copy();
	}

	@Override
	protected SoundEvent getDefaultHitGroundSoundEvent() {
		return SoundEvents.TRIDENT_HIT_GROUND;
	}

	/** Never times out on its own: it lives as long as the thrower keeps it out. */
	@Override
	protected void tickDespawn() {
	}

	@Override
	public void playerTouch(Player player) {
	}

	/** A dart is only ever out while its thrower holds the rope; don't save it with the chunk. */
	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	public boolean shouldRender(double x, double y, double z) {
		return true;
	}
}
