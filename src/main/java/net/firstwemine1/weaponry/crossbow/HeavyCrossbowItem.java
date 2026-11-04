package net.firstwemine1.weaponry.crossbow;

import java.util.List;

import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Loads and shoots like vanilla's crossbow, with two changes from {@code crossbow-stats.md} §3. Heavy
 * Bolt: arrows do x1.4 base damage and have built-in Punch II (rockets are unchanged, their damage
 * comes from the explosion). Slow Crank: 1.6 s to load, with lower-pitched start and middle sounds; vanilla
 * plays the last one itself. Forge has no mixins here: the timing is overridden below, and the arrow is
 * changed as it joins the level, while {@link #use} is firing it.
 */
public class HeavyCrossbowItem extends CrossbowItem implements WeaponCrossbow {
	/** Vanilla's is 25 ticks. Quick Charge still takes 5 off per level. */
	public static final int EXTRA_CHARGE_TICKS = 7;
	/** Entity tag on its arrows. */
	public static final String HEAVY_BOLT = "weaponry.heavy_bolt";
	/** Punch II: Punch adds 1 per level. */
	public static final int PUNCH = 2;
	public static final double DAMAGE = 1.4;
	private static final int DURABILITY = 520;

	private static final float START_AT = 0.2f;
	private static final float MIDDLE_AT = 0.5f;

	/** Vanilla's load sounds at a lower pitch, set in {@code assets/weaponry/sounds.json}. */
	private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Weaponry.MOD_ID);
	public static final RegistryObject<SoundEvent> LOADING_START = sound("item.heavy_crossbow.loading_start");
	public static final RegistryObject<SoundEvent> LOADING_MIDDLE = sound("item.heavy_crossbow.loading_middle");
	/** Set while {@link #use} runs on the server: whatever arrow joins the level then is this crossbow's. */
	private static boolean firing;
	private boolean startPlayed;
	private boolean middlePlayed;

	private static RegistryObject<SoundEvent> sound(String path) {
		return SOUNDS.register(path, () -> new SoundEvent(Weaponry.id(path)));
	}

	public HeavyCrossbowItem(Properties properties) {
		super(properties.durability(DURABILITY));
	}

	public static int chargeTicks(ItemStack stack) {
		return CrossbowItem.getChargeDuration(stack) + EXTRA_CHARGE_TICKS;
	}

	static void registerSounds(IEventBus modBus) {
		SOUNDS.register(modBus);
		MinecraftForge.EVENT_BUS.addListener(HeavyCrossbowItem::onArrowJoins);
	}

	/** Only arrows: a rocket's damage comes from its explosion. */
	private static void onArrowJoins(EntityJoinWorldEvent event) {
		if (firing && event.getEntity() instanceof AbstractArrow arrow) {
			arrow.setBaseDamage(arrow.getBaseDamage() * DAMAGE);
			arrow.setKnockback(PUNCH);
			arrow.addTag(HEAVY_BOLT);
		}
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		firing = !level.isClientSide;
		try {
			return super.use(level, player, hand);
		} finally {
			firing = false;
		}
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return chargeTicks(stack) + 3;
	}

	/** Vanilla's own check passes at its 25 ticks, so hold this crossbow to its full load first. */
	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingUseTicks) {
		if (getUseDuration(stack) - remainingUseTicks >= chargeTicks(stack)) {
			super.releaseUsing(stack, level, user, remainingUseTicks);
		}
	}

	/** Vanilla's start and middle load sounds, at this crossbow's pace and pitch. */
	@Override
	public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (level.isClientSide) {
			return;
		}
		float progress = (getUseDuration(stack) - remainingUseTicks) / (float) chargeTicks(stack);
		if (progress < START_AT) {
			startPlayed = false;
			middlePlayed = false;
		}
		int quickCharge = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.QUICK_CHARGE, stack);
		if (progress >= START_AT && !startPlayed) {
			startPlayed = true;
			level.playSound(null, user.getX(), user.getY(), user.getZ(), quickCharge > 0 ? quickChargeSound(quickCharge) : LOADING_START.get(), SoundSource.PLAYERS, 0.5f, 1.0f);
		}
		if (progress >= MIDDLE_AT && quickCharge == 0 && !middlePlayed) {
			middlePlayed = true;
			level.playSound(null, user.getX(), user.getY(), user.getZ(), LOADING_MIDDLE.get(), SoundSource.PLAYERS, 0.5f, 1.0f);
		}
	}

	/** Quick Charge has its own, faster sounds, which stay as they are. */
	private static SoundEvent quickChargeSound(int level) {
		return switch (level) {
			case 1 -> SoundEvents.CROSSBOW_QUICK_CHARGE_1;
			case 2 -> SoundEvents.CROSSBOW_QUICK_CHARGE_2;
			default -> SoundEvents.CROSSBOW_QUICK_CHARGE_3;
		};
	}

	@Override
	public String id() {
		return "heavy_crossbow";
	}

	@Override
	public String displayName() {
		return "Heavy Crossbow";
	}

	@Override
	public List<String> description() {
		return List.of(
				"Heavy Bolt: bolts hit harder and knock targets back further",
				"Slow Crank (use): slow to load");
	}

	/** Whether an arrow was shot from a heavy crossbow. */
	public static boolean isHeavyBolt(Entity arrow) {
		return arrow instanceof AbstractArrow && arrow.getTags().contains(HEAVY_BOLT);
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		for (int i = 0; i < description().size(); i++) {
			tooltip.add(new TranslatableComponent(ModCrossbows.tooltipKey(this, i)).withStyle(ChatFormatting.GRAY));
		}
	}
}
