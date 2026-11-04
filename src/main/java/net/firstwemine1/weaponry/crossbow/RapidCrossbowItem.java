package net.firstwemine1.weaponry.crossbow;

import java.util.List;
import java.util.function.Predicate;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.bow.HorseBowAbility;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Vanishable;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The gatling crossbow ({@code crossbow-stats.md} §3). Gatling: {@link RapidFiring} calls {@link #fire}
 * while attack is held. Crank Reload: holding use loads arrows into its {@link Magazine}. Rapid hits:
 * the arrow mixin clears the target's hurt cooldown after each hit.
 */
public class RapidCrossbowItem extends ProjectileWeaponItem implements WeaponCrossbow, Vanishable {
	/** Entity tag on its arrows; the arrow mixin clears the target's hurt cooldown when they hit. */
	public static final String RAPID_ARROW = "weaponry.rapid_arrow";
	public static final String ARROWS_TOOLTIP_KEY = "tooltip." + Weaponry.MOD_ID + ".rapid_crossbow.arrows";
	private static final Predicate<ItemStack> LOADABLE = stack -> stack.is(Items.ARROW) || stack.is(Items.SPECTRAL_ARROW);
	private static final int DURABILITY = 1600;
	private static final float VELOCITY = 3.15f;
	private static final float SPREAD = 2.0f;
	/** Arrow damage is ceil(speed x base damage), so any base this low does exactly 1. */
	private static final double BASE_DAMAGE = 0.15;
	private static final int USE_DURATION = 72000;
	/** A reload is 5 steps of 0.25 s, the unit Quick Charge takes off vanilla's load time per level. */
	private static final int FULL_RELOAD_STEPS = 5;
	private static final int CRANK_SOUND_EVERY = 8;
	private static final int BAR_COLOR = 0x3D9BE0;
	private static final int BAR_WIDTH = 13;

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
		return List.of(
				"Gatling (hold attack): fires a stream of weak arrows from the magazine",
				"Crank Reload (hold use): loads arrows into the magazine; let go to stop");
	}

	public static Magazine magazine(ItemStack stack) {
		return Magazine.read(stack);
	}

	/** Whether an arrow was shot from a rapid crossbow. */
	public static boolean isRapidArrow(Entity arrow) {
		return arrow instanceof AbstractArrow && arrow.getTags().contains(RAPID_ARROW);
	}

	/** Holds arrows and isn't being reloaded: drawn and held like a loaded crossbow. */
	public static boolean looksLoaded(ItemStack stack, LivingEntity holder) {
		boolean reloading = holder != null && holder.isUsingItem() && holder.getUseItem().is(stack.getItem());
		return !magazine(stack).isEmpty() && !reloading;
	}

	/** Fires one arrow from the magazine, if it has one. The caller keeps the rate: see {@link RapidFiring}. */
	public boolean fire(ServerLevel level, Player player, ItemStack stack) {
		Magazine magazine = magazine(stack);
		if (magazine.isEmpty()) {
			return false;
		}
		ItemStack ammo = new ItemStack(magazine.arrow());
		AbstractArrow arrow = ((ArrowItem) magazine.arrow()).createArrow(level, ammo, player);
		arrow.setBaseDamage(BASE_DAMAGE);
		arrow.setSoundEvent(SoundEvents.CROSSBOW_HIT);
		arrow.addTag(RAPID_ARROW);
		arrow.addTag(HorseBowAbility.LIGHT_ARROW);
		int piercing = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, stack);
		if (piercing > 0) {
			arrow.setPierceLevel((byte) piercing);
		}
		if (player.getAbilities().instabuild) {
			arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
		}
		// Like vanilla's crossbow, and unlike a bow, the shooter's own movement isn't added to the arrow.
		Vec3 view = player.getViewVector(1.0f);
		arrow.shoot(view.x, view.y, view.z, VELOCITY, SPREAD);
		level.addFreshEntity(arrow);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CROSSBOW_SHOOT,
				player.getSoundSource(), 0.5f, 1.3f + player.getRandom().nextFloat() * 0.2f);
		new Magazine(magazine.count() - 1, magazine.arrow()).write(stack);
		stack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		player.awardStat(Stats.ITEM_USED.get(this));
		return true;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!canLoad(player, magazine(stack))) {
			return InteractionResultHolder.fail(stack);
		}
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (!(user instanceof Player player)) {
			return;
		}
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
		int steps = Mth.clamp(FULL_RELOAD_STEPS - quickCharge, 1, FULL_RELOAD_STEPS);
		return (used + 1) * FULL_RELOAD_STEPS / steps - used * FULL_RELOAD_STEPS / steps;
	}

	private static void load(Level level, Player player, ItemStack stack, int arrows) {
		Magazine magazine = magazine(stack);
		int before = magazine.count();
		boolean infinite = player.getAbilities().instabuild;
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
						SoundSource.PLAYERS, 0.4f, 1.2f);
			}
		}
	}

	private static boolean canLoad(Player player, Magazine magazine) {
		return !magazine.isFull() && (player.getAbilities().instabuild || !findAmmo(player, magazine).isEmpty());
	}

	/**
	 * The next arrows to load, in vanilla's order: the off hand, then the inventory. Anything the magazine
	 * can't take is skipped: tipped arrows, or once it holds arrows, any other type.
	 */
	private static ItemStack findAmmo(Player player, Magazine magazine) {
		Predicate<ItemStack> matches = magazine.isEmpty() ? LOADABLE : stack -> stack.is(magazine.arrow());
		if (matches.test(player.getOffhandItem())) {
			return player.getOffhandItem();
		}
		for (ItemStack stack : player.getInventory().items) {
			if (matches.test(stack)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.CROSSBOW;
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
	public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
		return false;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return !magazine(stack).isEmpty();
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(BAR_WIDTH * magazine(stack).count() / (float) Magazine.CAPACITY);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return BAR_COLOR;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(ARROWS_TOOLTIP_KEY, magazine(stack).count(), Magazine.CAPACITY).withStyle(ChatFormatting.GRAY));
		for (int i = 0; i < description().size(); i++) {
			tooltip.add(Component.translatable(ModCrossbows.tooltipKey(this, i)).withStyle(ChatFormatting.GRAY));
		}
	}
}
