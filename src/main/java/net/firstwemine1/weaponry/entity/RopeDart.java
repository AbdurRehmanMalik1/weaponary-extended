package net.firstwemine1.weaponry.entity;

import java.util.List;

import javax.annotation.Nullable;

import net.firstwemine1.weaponry.combat.Motion;
import net.firstwemine1.weaponry.item.ModItems;
import net.firstwemine1.weaponry.item.WeaponItem;
import net.firstwemine1.weaponry.item.WeaponMaterial;
import net.firstwemine1.weaponry.item.WeaponType;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.DamageSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPartEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.monster.RavagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;

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
 * <p>In this version {@code AbstractArrowEntity} keeps no item stack, so the rope dart is stored here, as
 * {@code ThrownWeapon} does.
 */
public class RopeDart extends AbstractArrowEntity implements IEntityAdditionalSpawnData {
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

	private static final DataParameter<ItemStack> DATA_ITEM =
			EntityDataManager.defineId(RopeDart.class, DataSerializers.ITEM_STACK);
	/** Entity id of the tethered target, or -1. */
	private static final DataParameter<Integer> DATA_TETHER =
			EntityDataManager.defineId(RopeDart.class, DataSerializers.INT);
	private static final DataParameter<Boolean> DATA_ANCHORED =
			EntityDataManager.defineId(RopeDart.class, DataSerializers.BOOLEAN);

	private ItemStack weapon = ItemStack.EMPTY;
	private float damage;
	private int tetherTicks;
	private int dragTicks;
	/** Client-side, on the thrower's own client: the current rope length while anchored. */
	private double ropeLength = -1;

	public RopeDart(EntityType<? extends RopeDart> type, World level) {
		super(type, level);
	}

	public RopeDart(World level, PlayerEntity owner, ItemStack stack, float damage) {
		super(ModEntities.ROPE_DART.get(), owner, level);
		this.weapon = stack.copy();
		this.weapon.setCount(1);
		this.damage = damage;
		this.pickup = PickupStatus.DISALLOWED;
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
		Entity entity = id >= 0 ? level.getEntity(id) : null;
		return entity instanceof LivingEntity ? (LivingEntity) entity : null;
	}

	/** The dart this player has out, on either side, or {@code null}. */
	@Nullable
	public static RopeDart of(PlayerEntity player) {
		List<RopeDart> darts = player.level.getEntitiesOfClass(RopeDart.class,
				player.getBoundingBox().inflate(SNAP_LENGTH + 4.0), dart -> dart.getOwner() == player && !dart.removed);
		return darts.isEmpty() ? null : darts.get(0);
	}

	public static boolean isRopeDart(ItemStack stack) {
		return WeaponItem.is(stack, WeaponType.ROPE_DART);
	}

	/** Pulls the dart back in. */
	public void reelIn() {
		if (!removed) {
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundCategory.PLAYERS, 1.0f, 1.2f);
			remove();
		}
	}

	private void snap() {
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.LEASH_KNOT_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
		remove();
	}

	@Override
	public void tick() {
		PlayerEntity owner = getOwner() instanceof PlayerEntity ? (PlayerEntity) getOwner() : null;
		if (!level.isClientSide() && !ownerStillHolds(owner)) {
			reelIn();
			return;
		}

		if (isTethered()) {
			// setOldPosAndRot() in later versions.
			setPosAndOldPos(getX(), getY(), getZ());
			yRotO = yRot;
			xRotO = xRot;
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
		if (removed) {
			return;
		}
		if (level.isClientSide()) {
			// isControlledByLocalInstance() is false for the client's own player in this version.
			if (isAnchored() && owner != null && owner.isLocalPlayer()) {
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
				owner.fallDistance = 0.0f;
			}
		} else if (inGround) {
			entityData.set(DATA_ANCHORED, true);
			hurtHeldDart(owner, 1);
		} else if (distance > ROPE_LENGTH) {
			// The rope ran out: the dart falls short and comes back.
			reelIn();
		}
	}

	private static boolean ownerStillHolds(@Nullable PlayerEntity owner) {
		return owner != null && owner.isAlive() && !owner.isSpectator()
				&& (isRopeDart(owner.getMainHandItem()) || isRopeDart(owner.getOffhandItem()));
	}

	// --- tether (server) ---

	private void tickTether(PlayerEntity owner, @Nullable LivingEntity target) {
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
			Vector3d back = owner.position().subtract(target.position()).normalize();
			push(target, back.scale(Math.min(0.6, (distance - ROPE_LENGTH) * 0.4)));
		}
	}

	private static void push(LivingEntity entity, Vector3d motion) {
		entity.setDeltaMovement(motion.x, Math.max(entity.getDeltaMovement().y, motion.y), motion.z);
		// Players move themselves: this sends the new motion to their client.
		entity.hurtMarked = true;
	}

	private static boolean heavy(LivingEntity entity) {
		// No wardens before 1.19.
		return entity instanceof IronGolemEntity || entity instanceof RavagerEntity;
	}

	private static boolean boss(Entity entity) {
		return entity instanceof WitherEntity || entity instanceof EnderDragonEntity || entity instanceof EnderDragonPartEntity;
	}

	/** Costs the thrower's rope dart durability; false once it broke. */
	private static boolean hurtHeldDart(PlayerEntity owner, int amount) {
		Hand hand = owner.isUsingItem() && isRopeDart(owner.getUseItem()) ? owner.getUsedItemHand()
				: isRopeDart(owner.getMainHandItem()) ? Hand.MAIN_HAND : Hand.OFF_HAND;
		ItemStack stack = owner.getItemInHand(hand);
		stack.hurtAndBreak(amount, owner, broken -> broken.broadcastBreakEvent(hand));
		return !stack.isEmpty();
	}

	/**
	 * PvP way out ({@code ALLOW_DAMAGE}): a tethered player breaks free by hitting the thrower, or when
	 * the thrower hits them for 4 or more.
	 */
	public static void onDamage(LivingEntity hurt, DamageSource source, float amount) {
		if (!(source.getEntity() instanceof PlayerEntity) || !(hurt instanceof PlayerEntity)) {
			return;
		}
		PlayerEntity attacker = (PlayerEntity) source.getEntity();
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

	private void hang(PlayerEntity owner) {
		if (ropeLength < 0) {
			ropeLength = Math.min(ROPE_LENGTH, owner.position().distanceTo(position()));
		}
		if (owner.jumping) {
			ropeLength = Math.max(MIN_ROPE, ropeLength - CLIMB_SPEED);
		} else if (owner.isShiftKeyDown()) {
			ropeLength = Math.min(ROPE_LENGTH, ropeLength + CLIMB_SPEED);
		}

		Vector3d fromAnchor = owner.position().subtract(position());
		double distance = fromAnchor.length();
		if (distance <= ropeLength || distance < 1.0e-6) {
			return;
		}
		// A taut rope: drop the outward part of the motion (keeping the swing) and pull back onto the rope.
		Vector3d out = fromAnchor.scale(1.0 / distance);
		Vector3d motion = owner.getDeltaMovement();
		double outward = motion.dot(out);
		if (outward > 0) {
			motion = motion.subtract(out.scale(outward));
		}
		motion = motion.subtract(out.scale(Math.min(0.5, (distance - ropeLength) * 0.5)));
		owner.setDeltaMovement(motion);
	}

	// --- hits ---

	@Override
	protected void onHitEntity(EntityRayTraceResult result) {
		Entity target = result.getEntity();
		Entity owner = getOwner();
		DamageSource source = DamageSource.thrown(this, owner == null ? this : owner);
		float amount = damage;
		if (target instanceof LivingEntity) {
			amount += EnchantmentHelper.getDamageBonus(weapon, ((LivingEntity) target).getMobType());
		}
		boolean hurt = target.hurt(source, amount);
		if (hurt && target instanceof LivingEntity && owner instanceof LivingEntity) {
			EnchantmentHelper.doPostHurtEffects((LivingEntity) target, owner);
			EnchantmentHelper.doPostDamageEffects((LivingEntity) owner, target);
		}
		playSound(SoundEvents.TRIDENT_HIT, 1.0f, 1.3f);
		if (level.isClientSide()) {
			return;
		}
		// Bosses shrug it off: the hit lands, but the dart doesn't stick.
		if (!(target instanceof LivingEntity) || !target.isAlive() || boss(target)) {
			reelIn();
			return;
		}
		entityData.set(DATA_TETHER, target.getId());
		playSound(SoundEvents.LEASH_KNOT_PLACE, 1.0f, 1.0f);
	}

	@Override
	protected void onHitBlock(BlockRayTraceResult result) {
		super.onHitBlock(result);
		playSound(SoundEvents.LEASH_KNOT_PLACE, 1.0f, 1.0f);
	}

	@Nullable
	@Override
	protected EntityRayTraceResult findHitEntity(Vector3d from, Vector3d to) {
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
	public void playerTouch(PlayerEntity player) {
	}

	/** The 1.16 client only builds vanilla entity types from the vanilla spawn packet. */
	@Override
	public IPacket<?> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	/** Forge's spawn packet doesn't carry the owner, and the thrower's client needs it to hang on the rope. */
	@Override
	public void writeSpawnData(PacketBuffer buf) {
		Entity owner = getOwner();
		buf.writeVarInt(owner == null ? 0 : owner.getId());
	}

	@Override
	public void readSpawnData(PacketBuffer buf) {
		Entity owner = level.getEntity(buf.readVarInt());
		if (owner != null) {
			setOwner(owner);
		}
	}

	@Override
	public boolean shouldRender(double x, double y, double z) {
		return true;
	}
}
