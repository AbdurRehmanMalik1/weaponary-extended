package net.firstwemine1.weaponry.crossbow;

import java.util.Arrays;
import java.util.List;

import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.RegistryObject;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.SoundEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.world.World;

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
		if (firing && event.getEntity() instanceof AbstractArrowEntity) {
			AbstractArrowEntity arrow = (AbstractArrowEntity) event.getEntity();
			arrow.setBaseDamage(arrow.getBaseDamage() * DAMAGE);
			arrow.setKnockback(PUNCH);
			arrow.addTag(HEAVY_BOLT);
		}
	}

	@Override
	public ActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
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
	public void releaseUsing(ItemStack stack, World level, LivingEntity user, int remainingUseTicks) {
		if (getUseDuration(stack) - remainingUseTicks >= chargeTicks(stack)) {
			super.releaseUsing(stack, level, user, remainingUseTicks);
		}
	}

	/** Vanilla's start and middle load sounds, at this crossbow's pace and pitch. */
	@Override
	public void onUseTick(World level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
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
			level.playSound(null, user.getX(), user.getY(), user.getZ(), quickCharge > 0 ? quickChargeSound(quickCharge) : LOADING_START.get(), SoundCategory.PLAYERS, 0.5f, 1.0f);
		}
		if (progress >= MIDDLE_AT && quickCharge == 0 && !middlePlayed) {
			middlePlayed = true;
			level.playSound(null, user.getX(), user.getY(), user.getZ(), LOADING_MIDDLE.get(), SoundCategory.PLAYERS, 0.5f, 1.0f);
		}
	}

	/** Quick Charge has its own, faster sounds, which stay as they are. */
	private static SoundEvent quickChargeSound(int level) {
		switch (level) {
			case 1:
				return SoundEvents.CROSSBOW_QUICK_CHARGE_1;
			case 2:
				return SoundEvents.CROSSBOW_QUICK_CHARGE_2;
			default:
				return SoundEvents.CROSSBOW_QUICK_CHARGE_3;
		}
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
		return Arrays.asList(
				"Heavy Bolt: bolts hit harder and knock targets back further",
				"Slow Crank (use): slow to load");
	}

	/** Vanilla's asks for {@code Items.CROSSBOW} by name: without this, holding past a full load wouldn't load it. */
	@Override
	public boolean useOnRelease(ItemStack stack) {
		return true;
	}

	/** Whether an arrow was shot from a heavy crossbow. */
	public static boolean isHeavyBolt(Entity arrow) {
		return arrow instanceof AbstractArrowEntity && arrow.getTags().contains(HEAVY_BOLT);
	}

	@Override
	public void appendHoverText(ItemStack stack, World level, List<ITextComponent> tooltip, ITooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		for (int i = 0; i < description().size(); i++) {
			tooltip.add(new TranslationTextComponent(ModCrossbows.tooltipKey(this, i)).withStyle(TextFormatting.GRAY));
		}
	}
}
