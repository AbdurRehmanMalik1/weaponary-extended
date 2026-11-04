package net.firstwemine1.weaponry.crossbow;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.bow.HorseBowAbility;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.stats.Stats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.CrossbowItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.UseAction;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.vector.Vector3d;

/**
 * The gatling crossbow ({@code crossbow-stats.md} §3). Gatling: {@link RapidFiring} calls {@link #fire}
 * while attack is held. Crank Reload: holding use loads arrows into its {@link Magazine}. Rapid hits:
 * the arrow mixin clears the target's hurt cooldown after each hit.
 */
public class RapidCrossbowItem extends CrossbowItem implements WeaponCrossbow {
	/** Entity tag on its arrows; the arrow mixin clears the target's hurt cooldown when they hit. */
	public static final String RAPID_ARROW = "weaponry.rapid_arrow";
	public static final String ARROWS_TOOLTIP_KEY = "tooltip." + Weaponry.MOD_ID + ".rapid_crossbow.arrows";
	private static final Predicate<ItemStack> LOADABLE = stack -> stack.getItem() == Items.ARROW || stack.getItem() == Items.SPECTRAL_ARROW;
	private static final int DURABILITY = 1600;
	private static final float VELOCITY = 3.15f;
	private static final float SPREAD = 2.0f;
	/** Arrow damage is ceil(speed x base damage), so any base this low does exactly 1. */
	private static final double BASE_DAMAGE = 0.15;
	private static final int USE_DURATION = 72000;
	/** A reload is 5 steps of 0.25 s, the unit Quick Charge takes off vanilla's load time per level. */
	private static final int FULL_RELOAD_STEPS = 5;
	private static final int CRANK_SOUND_EVERY = 8;

	public RapidCrossbowItem(Properties properties) {
		super(properties.durability(DURABILITY));
	}

	@Override
	public String id() {
		return "rapid_crossbow";
	}

	@Override
	public String displayName() {
		return "Rapid Crossbow";
	}

	@Override
	public List<String> description() {
		return Arrays.asList(
				"Gatling (hold attack): fires a stream of weak arrows from the magazine",
				"Crank Reload (hold use): loads arrows into the magazine; let go to stop");
	}

	public static Magazine magazine(ItemStack stack) {
		return Magazine.read(stack);
	}

	/** Whether an arrow was shot from a rapid crossbow. */
	public static boolean isRapidArrow(Entity arrow) {
		return arrow instanceof AbstractArrowEntity && arrow.getTags().contains(RAPID_ARROW);
	}

	/** Holds arrows and isn't being reloaded: drawn and held like a loaded crossbow. */
	public static boolean looksLoaded(ItemStack stack, LivingEntity holder) {
		boolean reloading = holder != null && holder.isUsingItem() && holder.getUseItem().getItem() == stack.getItem();
		return !magazine(stack).isEmpty() && !reloading;
	}

	/** Fires one arrow from the magazine, if it has one. The caller keeps the rate: see {@link RapidFiring}. */
	public boolean fire(ServerWorld level, PlayerEntity player, ItemStack stack) {
		Magazine magazine = magazine(stack);
		if (magazine.isEmpty()) {
			return false;
		}
		ItemStack ammo = new ItemStack(magazine.arrow());
		AbstractArrowEntity arrow = ((ArrowItem) magazine.arrow()).createArrow(level, ammo, player);
		arrow.setBaseDamage(BASE_DAMAGE);
		arrow.setSoundEvent(SoundEvents.CROSSBOW_HIT);
		arrow.addTag(RAPID_ARROW);
		arrow.addTag(HorseBowAbility.LIGHT_ARROW);
		int piercing = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, stack);
		if (piercing > 0) {
			arrow.setPierceLevel((byte) piercing);
		}
		if (player.abilities.instabuild) {
			arrow.pickup = AbstractArrowEntity.PickupStatus.CREATIVE_ONLY;
		}
		// Like vanilla's crossbow, and unlike a bow, the shooter's own movement isn't added to the arrow.
		Vector3d view = player.getViewVector(1.0f);
		arrow.shoot(view.x, view.y, view.z, VELOCITY, SPREAD);
		level.addFreshEntity(arrow);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_SHOOT,
				player.getSoundSource(), 0.5f, 1.3f + player.getRandom().nextFloat() * 0.2f);
		new Magazine(magazine.count() - 1, magazine.arrow()).write(stack);
		stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(EquipmentSlotType.MAINHAND));
		player.awardStat(Stats.ITEM_USED.get(this));
		return true;
	}

	@Override
	public ActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!canLoad(player, magazine(stack))) {
			return ActionResult.fail(stack);
		}
		player.startUsingItem(hand);
		return ActionResult.consume(stack);
	}

	@Override
	public void onUseTick(World level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (!(user instanceof PlayerEntity)) {
			return;
		}
		PlayerEntity player = (PlayerEntity) user;
		if (!level.isClientSide) {
			int used = getUseDuration(stack) - remainingUseTicks;
			load(level, player, stack, arrowsThisTick(stack, used));
		}
		// The client stops too, so the frames and the walk speed don't wait for the button to be let go.
		if (!canLoad(player, magazine(stack))) {
			user.stopUsingItem();
		}
	}

	/**
	 * Arrows to load on the {@code used}th tick of holding use: 1 per tick, or faster with Quick Charge
	 * (5/4, 5/3 or 5/2 per tick at levels I to III). Whole steps, so the reload times come out exact.
	 */
	private static int arrowsThisTick(ItemStack stack, int used) {
		int quickCharge = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, stack);
		int steps = MathHelper.clamp(FULL_RELOAD_STEPS - quickCharge, 1, FULL_RELOAD_STEPS);
		return (used + 1) * FULL_RELOAD_STEPS / steps - used * FULL_RELOAD_STEPS / steps;
	}

	private static void load(World level, PlayerEntity player, ItemStack stack, int arrows) {
		Magazine magazine = magazine(stack);
		int before = magazine.count();
		boolean infinite = player.abilities.instabuild;
		for (int i = 0; i < arrows && !magazine.isFull(); i++) {
			ItemStack ammo = findAmmo(player, magazine);
			if (ammo.isEmpty() && !infinite) {
				break;
			}
			Item type = magazine.isEmpty() && !ammo.isEmpty() ? ammo.getItem() : magazine.arrow();
			if (!infinite) {
				ammo.shrink(1);
			}
			magazine = new Magazine(magazine.count() + 1, type);
		}
		if (magazine.count() != before) {
			magazine.write(stack);
			if (magazine.count() / CRANK_SOUND_EVERY != before / CRANK_SOUND_EVERY) {
				level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_MIDDLE,
						SoundCategory.PLAYERS, 0.4f, 1.2f);
			}
		}
	}

	private static boolean canLoad(PlayerEntity player, Magazine magazine) {
		return !magazine.isFull() && (player.abilities.instabuild || !findAmmo(player, magazine).isEmpty());
	}

	/**
	 * The next arrows to load, in vanilla's order: the off hand, then the inventory. Anything the magazine
	 * can't take is skipped: tipped arrows, or once it holds arrows, any other type.
	 */
	private static ItemStack findAmmo(PlayerEntity player, Magazine magazine) {
		Predicate<ItemStack> matches = magazine.isEmpty() ? LOADABLE : stack -> stack.getItem() == magazine.arrow();
		if (matches.test(player.getOffhandItem())) {
			return player.getOffhandItem();
		}
		for (ItemStack stack : player.inventory.items) {
			if (matches.test(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	/** Its magazine loads while use is held, so letting go does nothing. */
	@Override
	public void releaseUsing(ItemStack stack, World level, LivingEntity user, int remainingUseTicks) {
	}

	/** Takes crossbow enchantments, except Multishot. */
	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
		return enchantment != Enchantments.MULTISHOT && super.canApplyAtEnchantingTable(stack, enchantment);
	}

	@Override
	public Predicate<ItemStack> getSupportedHeldProjectiles() {
		return LOADABLE;
	}

	@Override
	public UseAction getUseAnimation(ItemStack stack) {
		return UseAction.CROSSBOW;
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return USE_DURATION;
	}

	@Override
	public Predicate<ItemStack> getAllSupportedProjectiles() {
		return LOADABLE;
	}

	@Override
	public int getDefaultProjectileRange() {
		return 8;
	}

	/** Holding attack fires instead, so it never breaks blocks, even if a client sends it. */
	@Override
	public boolean canAttackBlock(BlockState state, World level, BlockPos pos, PlayerEntity player) {
		return false;
	}

	@Override
	public void appendHoverText(ItemStack stack, World level, List<ITextComponent> tooltip, ITooltipFlag flag) {
		tooltip.add(new TranslationTextComponent(ARROWS_TOOLTIP_KEY, magazine(stack).count(), Magazine.CAPACITY).withStyle(TextFormatting.GRAY));
		for (int i = 0; i < description().size(); i++) {
			tooltip.add(new TranslationTextComponent(ModCrossbows.tooltipKey(this, i)).withStyle(TextFormatting.GRAY));
		}
	}
}
