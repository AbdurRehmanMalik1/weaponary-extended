package net.firstwemine1.weaponry.bow;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;

/**
 * One of the mod's bows. Draws and shoots like vanilla's {@link BowItem}, but with its
 * {@link BowType}'s draw time, arrow speed, damage, spread and durability, plus its {@link BowAbility}.
 *
 * <p>This version's {@code BowItem} shoots in one block of {@code releaseUsing} with no hooks to override,
 * so {@link #releaseUsing} is Forge's, step by step, with the bow's numbers in place of vanilla's. That keeps
 * Forge's {@code ArrowLooseEvent}, {@code ArrowItem.isInfinite} and {@link #customArrow} hooks working.
 */
public class WeaponBowItem extends BowItem {
	/** Vanilla's arrow speed at full draw, in blocks per tick. */
	private static final float VANILLA_SPEED = 3.0f;
	/** Vanilla won't shoot below this power. */
	private static final float MIN_POWER = 0.1f;
	/** Flame sets the arrow alight for 100 s, as vanilla does. */
	private static final int FLAME_SECONDS = 100;

	private final BowType type;

	public WeaponBowItem(BowType type, Properties properties) {
		super(properties.durability(type.durability()));
		this.type = type;
	}

	public BowType type() {
		return type;
	}

	public static String tooltipKey(BowType type, int line) {
		return "tooltip." + Weaponry.MOD_ID + "." + type.id() + "." + line;
	}

	/** Vanilla's power curve, stretched so it reaches 1.0 at {@code drawTicks}. */
	public static float power(int drawnTicks, int drawTicks) {
		float t = drawnTicks / (float) drawTicks;
		return Math.min(1.0f, (t * t + t * 2.0f) / 3.0f);
	}

	/** The fewest draw ticks that reach {@code power}. */
	public static int ticksForPower(float power, int drawTicks) {
		int ticks = 0;
		while (power(ticks, drawTicks) < power) {
			ticks++;
		}
		return ticks;
	}

	public int drawTicks(LivingEntity user) {
		return type.ability().drawTicks(type, user);
	}

	/** Ticks this draw has been going, including any head start. */
	public int drawnTicks(ItemStack stack, LivingEntity user, int remainingUseTicks) {
		return getUseDuration(stack) - remainingUseTicks + BowState.headStart(user);
	}

	/** How long the bow has been held at full draw, or -1 if it isn't fully drawn. */
	public static int ticksAtFullDraw(int drawnTicks, int drawTicks) {
		return drawnTicks >= drawTicks ? drawnTicks - drawTicks : -1;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		int lines = type.ability().description().size();
		for (int i = 0; i < lines; i++) {
			tooltip.add(Component.translatable(tooltipKey(type, i)).withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (!type.ability().canDraw(player)) {
			return InteractionResultHolder.fail(player.getItemInHand(hand));
		}
		InteractionResultHolder<ItemStack> result = super.use(level, player, hand);
		if (result.getResult().consumesAction()) {
			BowState.startDraw(player, type.ability().headStart(type, player));
		}
		return result;
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingUseTicks) {
		if (!(user instanceof Player player) || !type.ability().canDraw(player)) {
			return;
		}
		boolean infinite = player.getAbilities().instabuild
				|| EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, stack) > 0;
		ItemStack ammo = player.getProjectile(stack);
		// Other mods see (and can change) the draw in this bow's own ticks, head start included.
		int drawn = ForgeEventFactory.onArrowLoose(stack, level, player, drawnTicks(stack, player, remainingUseTicks),
				!ammo.isEmpty() || infinite);
		if (drawn < 0 || ammo.isEmpty() && !infinite) {
			return;
		}
		if (ammo.isEmpty()) {
			ammo = new ItemStack(Items.ARROW);
		}
		int drawTicks = drawTicks(player);
		float power = power(drawn, drawTicks);
		if (power < MIN_POWER) {
			return;
		}
		int atFull = ticksAtFullDraw(drawn, drawTicks);
		boolean freeArrow = player.getAbilities().instabuild
				|| ammo.getItem() instanceof ArrowItem arrowItem && arrowItem.isInfinite(ammo, stack, player);
		if (!level.isClientSide()) {
			float velocity = power * VANILLA_SPEED * type.speed();
			AbstractArrow arrow = createArrow(level, ammo, player, stack, velocity, atFull);
			arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, velocity, type.ability().spread(type, player, atFull));
			if (power == 1.0f) {
				arrow.setCritArrow(true);
			}
			InteractionHand hand = player.getUsedItemHand();
			stack.hurtAndBreak(1 + type.ability().extraDurability(power), player, entity -> entity.broadcastBreakEvent(hand));
			if (freeArrow || player.getAbilities().instabuild
					&& (ammo.getItem() == Items.SPECTRAL_ARROW || ammo.getItem() == Items.TIPPED_ARROW)) {
				arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
			}
			type.ability().onArrow(arrow, player);
			level.addFreshEntity(arrow);
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
				1.0f, 1.0f / (level.getRandom().nextFloat() * 0.4f + 1.2f) + power * 0.5f);
		if (!freeArrow && !player.getAbilities().instabuild) {
			ammo.shrink(1);
			if (ammo.isEmpty()) {
				player.getInventory().removeItem(ammo);
			}
		}
		player.awardStat(Stats.ITEM_USED.get(this));
		BowState.shot(player);
	}

	/** The arrow with the bow's damage, Snap Shot-style bonus and the bow enchantments, not yet shot. */
	private AbstractArrow createArrow(Level level, ItemStack ammo, Player player, ItemStack stack, float velocity, int atFull) {
		ArrowItem arrowItem = ammo.getItem() instanceof ArrowItem item ? item : (ArrowItem) Items.ARROW;
		AbstractArrow arrow = customArrow(arrowItem.createArrow(level, ammo, player));
		double base = arrow.getBaseDamage() * type.damage();
		// Arrow damage is ceil(speed x base damage), so +N damage is +N / speed of base damage.
		float bonus = type.ability().bonusDamage(player, atFull);
		if (bonus > 0 && velocity > 0) {
			base += bonus / velocity;
		}
		int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, stack);
		if (powerLevel > 0) {
			base += powerLevel * 0.5 + 0.5;
		}
		arrow.setBaseDamage(base);
		int punch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, stack);
		if (punch > 0) {
			arrow.setKnockback(punch);
		}
		if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, stack) > 0) {
			arrow.setSecondsOnFire(FLAME_SECONDS);
		}
		return arrow;
	}
}
