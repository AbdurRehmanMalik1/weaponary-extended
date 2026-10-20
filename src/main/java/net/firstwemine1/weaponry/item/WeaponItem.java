package net.firstwemine1.weaponry.item;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.ability.WarPickAbility;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

/**
 * A melee weapon: one {@link WeaponType} in one {@link WeaponMaterial}. Behaves like a sword minus
 * the sweep attack; everything else comes from the type's {@link WeaponAbility}.
 *
 * <p>Not a {@code SwordItem}, so Forge's {@code SWORD_SWEEP} tool action doesn't apply and vanilla
 * never sweeps with it. Sword enchantments are allowed through {@link #canApplyAtEnchantingTable};
 * reach comes from Forge's {@code attack_range} attribute.
 */
public class WeaponItem extends TieredItem {
	/** Vanilla sword damage is 1 (player base) + 3 + tier bonus. */
	private static final int SWORD_BASE_DAMAGE = 3;
	private static final float PLAYER_BASE_DAMAGE = 1.0f;
	private static final float PLAYER_BASE_ATTACK_SPEED = 4.0f;
	private static final double PLAYER_BASE_REACH = 3.0;
	private static final UUID REACH_UUID = UUID.fromString("2f1e7c3a-6b4d-4e8f-9a0b-1c2d3e4f5a6b");
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
	private final WeaponTier tier;
	/** Built on first use: Forge registers the reach attribute separately from (and after) items. */
	private Multimap<Attribute, AttributeModifier> attributes;

	public WeaponItem(WeaponType type, WeaponMaterial material, Properties properties) {
		this(type, material, new WeaponTier(material.tier(), type.durabilityMultiplier()), properties);
	}

	private WeaponItem(WeaponType type, WeaponMaterial material, WeaponTier tier, Properties properties) {
		super(tier, properties);
		this.type = type;
		this.material = material;
		this.attackDamage = PLAYER_BASE_DAMAGE + damageModifier(type, tier);
		this.tier = tier;
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
		double reachBonus = type.reach() - PLAYER_BASE_REACH;
		if (reachBonus != 0) {
			builder.put(ForgeMod.ATTACK_RANGE.get(),
					new AttributeModifier(REACH_UUID, "Weapon reach", reachBonus, AttributeModifier.Operation.ADDITION));
		}
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
		if (slot != EquipmentSlot.MAINHAND) {
			return super.getDefaultAttributeModifiers(slot);
		}
		if (attributes == null) {
			attributes = createAttributes(type, material, tier);
		}
		return attributes;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		int lines = type.ability().description().size();
		for (int i = 0; i < lines; i++) {
			tooltip.add(Component.translatable(tooltipKey(type, i)).withStyle(ChatFormatting.GRAY));
		}
		if (type.twoHanded()) {
			tooltip.add(Component.translatable(TWO_HANDED_TOOLTIP_KEY).withStyle(ChatFormatting.DARK_GRAY));
		}
	}

	// --- Forge hooks: shield breaking, enchanting, axe and pickaxe actions ---

	@Override
	public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
		return type.ability().disablesShields() || super.canDisableShield(stack, shield, entity, attacker);
	}

	/**
	 * Sword enchantments (category {@code WEAPON}, which only accepts {@code SwordItem}) on every
	 * weapon, and the {@code DIGGER} ones (Efficiency, Fortune, Silk Touch) on the hatchet and war pick.
	 * Forge routes the table, the anvil and {@code /enchant} through this.
	 */
	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
		return enchantment.category == EnchantmentCategory.WEAPON
				|| enchantment.category == EnchantmentCategory.DIGGER && type.ability().diggerEnchantable()
				|| super.canApplyAtEnchantingTable(stack, enchantment);
	}

	@Override
	public boolean canPerformAction(ItemStack stack, ToolAction action) {
		return action == ToolActions.AXE_DIG && type.ability().choppingTool()
				|| action == ToolActions.PICKAXE_DIG && WarPickAbility.isFlipped(stack)
				|| super.canPerformAction(stack, action);
	}

	// --- sword-like durability handling, axe mining for the hatchet, pickaxe mining for the flipped war pick ---

	@Override
	public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
		return type.ability().choppingTool() || WarPickAbility.isFlipped(player.getMainHandItem()) || !player.isCreative();
	}

	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		if (WarPickAbility.isFlipped(stack)) {
			return state.is(BlockTags.MINEABLE_WITH_PICKAXE) ? getTier().getSpeed() * WarPickAbility.MINING_SPEED : 1.0f;
		}
		return type.ability().choppingTool() && state.is(BlockTags.MINEABLE_WITH_AXE) ? getTier().getSpeed() : 1.0f;
	}

	/** The hatchet; the flipped war pick needs the stack, so it goes through the Forge overload below. */
	@Override
	public boolean isCorrectToolForDrops(BlockState state) {
		return type.ability().choppingTool() && isCorrectToolForDrops(state, BlockTags.MINEABLE_WITH_AXE);
	}

	/** Forge's stack-aware version, which both the drops and the mining speed penalty for the wrong tool use. */
	@Override
	public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
		return WarPickAbility.isFlipped(stack) ? isCorrectToolForDrops(state, BlockTags.MINEABLE_WITH_PICKAXE) : isCorrectToolForDrops(state);
	}

	/** Same rule as vanilla {@code DiggerItem}, with {@code mineable} as its block tag. */
	public boolean isCorrectToolForDrops(BlockState state, TagKey<Block> mineable) {
		int level = getTier().getLevel();
		if (level < 3 && state.is(BlockTags.NEEDS_DIAMOND_TOOL)
				|| level < 2 && state.is(BlockTags.NEEDS_IRON_TOOL)
				|| level < 1 && state.is(BlockTags.NEEDS_STONE_TOOL)) {
			return false;
		}
		return state.is(mineable);
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
		if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0f) {
			int cost = type.ability().choppingTool() || WarPickAbility.isFlipped(stack) ? CHOPPING_BLOCK_DAMAGE : BLOCK_DAMAGE;
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
