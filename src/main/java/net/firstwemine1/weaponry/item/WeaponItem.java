package net.firstwemine1.weaponry.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A melee weapon: one {@link WeaponType} in one {@link WeaponMaterial}. Behaves like a sword minus
 * the sweep attack; everything else comes from the type's {@link WeaponAbility}.
 *
 * <p>Not a {@code SwordItem}: in this version {@code Player.attack} sweeps with anything that is
 * one. Sword enchantments are allowed through {@code WeaponEnchantmentCategoryMixin} instead.
 * Reach is not applied: the {@code entity_interaction_range} attribute only exists from 1.20.5.
 */
public class WeaponItem extends TieredItem {
	/** Vanilla sword damage is 1 (player base) + 3 + tier bonus. */
	private static final int SWORD_BASE_DAMAGE = 3;
	private static final float PLAYER_BASE_DAMAGE = 1.0f;
	private static final float PLAYER_BASE_ATTACK_SPEED = 4.0f;
	/** Breaking a block costs 2 durability, like a sword; the hatchet, an axe, pays 1. */
	private static final int BLOCK_DAMAGE = 2;
	private static final int CHOPPING_BLOCK_DAMAGE = 1;
	public static final String TWO_HANDED_TOOLTIP_KEY = "tooltip." + Weaponry.MOD_ID + ".two_handed";
	public static final String TWO_HANDED_MESSAGE_KEY = "message." + Weaponry.MOD_ID + ".two_handed";
	public static final String TWO_HANDED_MOVED_KEY = TWO_HANDED_MESSAGE_KEY + ".moved";
	public static final String TWO_HANDED_FULL_KEY = TWO_HANDED_MESSAGE_KEY + ".full";

	private final WeaponType type;
	private final WeaponMaterial material;
	private final float attackDamage;
	private final Multimap<Attribute, AttributeModifier> attributes;

	public WeaponItem(WeaponType type, WeaponMaterial material, Properties properties) {
		this(type, material, new WeaponTier(material.tier(), type.durabilityMultiplier()), properties);
	}

	private WeaponItem(WeaponType type, WeaponMaterial material, WeaponTier tier, Properties properties) {
		super(tier, properties);
		this.type = type;
		this.material = material;
		this.attackDamage = PLAYER_BASE_DAMAGE + damageModifier(type, tier);
		this.attributes = createAttributes(type, material, tier);
	}

	/** Modifier on top of the player's base 1 damage; never below 0 so a hit always does >= 1. */
	private static float damageModifier(WeaponType type, WeaponTier tier) {
		return Math.max(0, SWORD_BASE_DAMAGE + tier.getAttackDamageBonus() + type.damageOffset());
	}

	public static Multimap<Attribute, AttributeModifier> createAttributes(WeaponType type, WeaponMaterial material, WeaponTier tier) {
		ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.<Attribute, AttributeModifier>builder()
				.put(Attributes.ATTACK_DAMAGE,
						new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", damageModifier(type, tier),
								AttributeModifier.Operation.ADDITION))
				.put(Attributes.ATTACK_SPEED,
						new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", type.attackSpeed() - PLAYER_BASE_ATTACK_SPEED,
								AttributeModifier.Operation.ADDITION));
		type.ability().addAttributes(builder, material);
		return builder.build();
	}

	/** The ability of the weapon in this stack, or {@link WeaponAbility#NONE} for any other item. */
	public static WeaponAbility abilityOf(ItemStack stack) {
		return stack.getItem() instanceof WeaponItem weapon ? weapon.type.ability() : WeaponAbility.NONE;
	}

	public static boolean is(ItemStack stack, WeaponType type) {
		return stack.getItem() instanceof WeaponItem weapon && weapon.type == type;
	}

	public WeaponType type() {
		return type;
	}

	public WeaponMaterial material() {
		return material;
	}

	/** Full-charge melee damage in half-hearts, including the player's base 1. */
	public float attackDamage() {
		return attackDamage;
	}

	public float abilityStrength() {
		return material.abilityStrength();
	}

	public static String tooltipKey(WeaponType type, int line) {
		return "tooltip." + Weaponry.MOD_ID + "." + type.id() + "." + line;
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
		return slot == EquipmentSlot.MAINHAND ? attributes : super.getDefaultAttributeModifiers(slot);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		int lines = type.ability().description().size();
		for (int i = 0; i < lines; i++) {
			tooltip.add(new TranslatableComponent(tooltipKey(type, i)).withStyle(ChatFormatting.GRAY));
		}
		if (type.twoHanded()) {
			tooltip.add(new TranslatableComponent(TWO_HANDED_TOOLTIP_KEY).withStyle(ChatFormatting.DARK_GRAY));
		}
	}

	// --- sword-like durability handling, axe mining for the hatchet ---

	@Override
	public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
		return type.ability().choppingTool() || !player.isCreative();
	}

	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		return type.ability().choppingTool() && state.is(BlockTags.MINEABLE_WITH_AXE) ? getTier().getSpeed() : 1.0f;
	}

	/** Same rule as vanilla {@code DiggerItem} with the axe's block tag. */
	@Override
	public boolean isCorrectToolForDrops(BlockState state) {
		if (!type.ability().choppingTool()) {
			return false;
		}
		int level = getTier().getLevel();
		if (level < 3 && state.is(BlockTags.NEEDS_DIAMOND_TOOL)
				|| level < 2 && state.is(BlockTags.NEEDS_IRON_TOOL)
				|| level < 1 && state.is(BlockTags.NEEDS_STONE_TOOL)) {
			return false;
		}
		return state.is(BlockTags.MINEABLE_WITH_AXE);
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
		if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0f) {
			int cost = type.ability().choppingTool() ? CHOPPING_BLOCK_DAMAGE : BLOCK_DAMAGE;
			stack.hurtAndBreak(cost, miner, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		}
		return true;
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		type.ability().onHit(stack, target, attacker);
		stack.hurtAndBreak(1, attacker, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
		return true;
	}

	// --- ability hooks ---

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		InteractionResultHolder<ItemStack> result = type.ability().onUse(level, player, hand);
		return result.getResult().consumesAction() ? result : super.use(level, player, hand);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return type.ability().useAnimation();
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return type.ability().useDuration();
	}

	@Override
	public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingTicks) {
		type.ability().onUseTick(stack, level, user, remainingTicks);
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingTicks) {
		type.ability().onReleaseUsing(stack, level, user, remainingTicks);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
		type.ability().onFinishUsing(stack, level, user);
		return stack;
	}
}
