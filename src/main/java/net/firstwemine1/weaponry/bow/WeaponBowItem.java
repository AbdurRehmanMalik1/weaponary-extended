package net.firstwemine1.weaponry.bow;

import java.util.List;
import java.util.function.Consumer;

import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.mixin.AbstractArrowAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.level.Level;

/**
 * One of the mod's bows. Draws and shoots like vanilla's {@link BowItem}, but with its
 * {@link BowType}'s draw time, arrow speed, damage, spread and durability, plus its {@link BowAbility}.
 */
public class WeaponBowItem extends BowItem {
	/** Vanilla's arrow speed at full draw, in blocks per tick. */
	private static final float VANILLA_SPEED = 3.0f;
	/** Vanilla won't shoot below this power. */
	private static final float MIN_POWER = 0.1f;

	private final BowType type;
	/** The shot being fired, read by {@link #createProjectile}. Only set on the server thread, during {@code shoot}. */
	private Shot shot;

	private record Shot(float velocity, int ticksAtFullDraw) {
	}

	public WeaponBowItem(BowType type, Properties properties) {
		// Enchantability is a component now; vanilla's bow has 1. The walk speed while drawing is the
		// use_effects component (vanilla: 0.2).
		super(properties.durability(type.durability()).enchantable(1)
				.component(DataComponents.USE_EFFECTS, new UseEffects(false, true, type.ability().walkSpeedWhileDrawing())));
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
		return getUseDuration(stack, user) - remainingUseTicks + BowState.headStart(user);
	}

	/** How long the bow has been held at full draw, or -1 if it isn't fully drawn. */
	public static int ticksAtFullDraw(int drawnTicks, int drawTicks) {
		return drawnTicks >= drawTicks ? drawnTicks - drawTicks : -1;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
			TooltipFlag flag) {
		int lines = type.ability().description().size();
		for (int i = 0; i < lines; i++) {
			tooltip.accept(Component.translatable(tooltipKey(type, i)).withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!type.ability().canDraw(player)) {
			return InteractionResult.FAIL;
		}
		InteractionResult result = super.use(level, player, hand);
		if (result.consumesAction()) {
			BowState.startDraw(player, type.ability().headStart(type, player));
		}
		return result;
	}

	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingUseTicks) {
		if (!(user instanceof Player player) || !type.ability().canDraw(player)) {
			return false;
		}
		ItemStack ammo = player.getProjectile(stack);
		if (ammo.isEmpty()) {
			return false;
		}
		int drawTicks = drawTicks(player);
		int drawn = drawnTicks(stack, player, remainingUseTicks);
		float power = power(drawn, drawTicks);
		if (power < MIN_POWER) {
			return false;
		}
		int atFull = ticksAtFullDraw(drawn, drawTicks);
		List<ItemStack> arrows = draw(stack, ammo, player);
		if (level instanceof ServerLevel server && !arrows.isEmpty()) {
			float velocity = power * VANILLA_SPEED * type.speed();
			InteractionHand hand = player.getUsedItemHand();
			shot = new Shot(velocity, atFull);
			try {
				shoot(server, player, hand, stack, arrows, velocity, type.ability().spread(type, player, atFull), power == 1.0f, null);
			} finally {
				shot = null;
			}
			int extra = type.ability().extraDurability(power);
			if (extra > 0) {
				stack.hurtAndBreak(extra, player, hand.asEquipmentSlot());
			}
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
				1.0f, 1.0f / (level.getRandom().nextFloat() * 0.4f + 1.2f) + power * 0.5f);
		player.awardStat(Stats.ITEM_USED.get(this));
		BowState.shot(player);
		return true;
	}

	@Override
	protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean crit) {
		Projectile projectile = super.createProjectile(level, shooter, weapon, ammo, crit);
		if (projectile instanceof AbstractArrow arrow && shot != null) {
			double base = ((AbstractArrowAccessor) arrow).weaponry$baseDamage() * type.damage();
			// Arrow damage is ceil(speed x base damage), so +N damage is +N / speed of base damage.
			float bonus = type.ability().bonusDamage(shooter, shot.ticksAtFullDraw());
			if (bonus > 0 && shot.velocity() > 0) {
				base += bonus / shot.velocity();
			}
			arrow.setBaseDamage(base);
			type.ability().onArrow(arrow, shooter);
		}
		return projectile;
	}
}
