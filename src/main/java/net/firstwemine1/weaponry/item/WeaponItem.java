package net.firstwemine1.weaponry.item;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.firstwemine1.weaponry.Weaponry;
import net.firstwemine1.weaponry.ability.WarPickAbility;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.TieredItem;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.UseAction;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;

/**
 * A melee weapon: one {@link WeaponType} in one {@link WeaponMaterial}. Behaves like a sword minus
 * the sweep attack; everything else comes from the type's {@link WeaponAbility}.
 *
 * <p>Not a {@code SwordItem}: in this version {@code Player.attack} sweeps with anything that is
 * one. Sword enchantments are allowed through {@link #canApplyAtEnchantingTable} instead.
 * Reach is not applied: the {@code entity_interaction_range} attribute only exists from 1.20.5.
 */
public class WeaponItem extends TieredItem {
	/** Vanilla sword damage is 1 (player base) + 3 + tier bonus. */
	private static final int SWORD_BASE_DAMAGE = 3;
	private static final float PLAYER_BASE_DAMAGE = 1.0f;
	private static final float PLAYER_BASE_ATTACK_SPEED = 4.0f;
	/** Breaking a block costs 2 durability, like a sword; the hatchet and flipped war pick, tools, pay 1. */
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
		return stack.getItem() instanceof WeaponItem ? ((WeaponItem) stack.getItem()).type.ability() : WeaponAbility.NONE;
	}

	public static boolean is(ItemStack stack, WeaponType type) {
		return stack.getItem() instanceof WeaponItem && ((WeaponItem) stack.getItem()).type == type;
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
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlotType slot) {
		return slot == EquipmentSlotType.MAINHAND ? attributes : super.getDefaultAttributeModifiers(slot);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable World level, List<ITextComponent> tooltip, ITooltipFlag flag) {
		int lines = type.ability().description().size();
		for (int i = 0; i < lines; i++) {
			tooltip.add(new TranslationTextComponent(tooltipKey(type, i)).withStyle(TextFormatting.GRAY));
		}
		if (type.twoHanded()) {
			tooltip.add(new TranslationTextComponent(TWO_HANDED_TOOLTIP_KEY).withStyle(TextFormatting.DARK_GRAY));
		}
	}

	// --- Forge hooks: shield breaking, enchanting ---

	@Override
	public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
		return type.ability().disablesShields() || super.canDisableShield(stack, shield, entity, attacker);
	}

	/**
	 * Sword enchantments (category {@code WEAPON}, which only accepts {@code SwordItem}) on every
	 * weapon, and the {@code DIGGER} ones (Efficiency, Fortune, Silk Touch) on the hatchet and war pick. Forge
	 * routes the table, the anvil and {@code /enchant} through this.
	 */
	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
		return enchantment.category == EnchantmentType.WEAPON
				|| enchantment.category == EnchantmentType.DIGGER && type.ability().diggerEnchantable()
				|| super.canApplyAtEnchantingTable(stack, enchantment);
	}

	/**
	 * A flipped war pick gets its pickaxe drops (stone, ores). Forge routes {@code ItemStack.isCorrectToolForDrops}
	 * here, and the drops and the wrong-tool mining penalty both go through that.
	 */
	@Override
	public boolean canHarvestBlock(ItemStack stack, BlockState state) {
		if (WarPickAbility.isFlipped(stack)) {
			return WarPickAbility.isCorrectToolForDrops(material, state);
		}
		return super.canHarvestBlock(stack, state);
	}

	// --- sword-like durability handling, axe mining for the hatchet, pickaxe mining for the flipped war pick ---

	@Override
	public boolean canAttackBlock(BlockState state, World level, BlockPos pos, PlayerEntity player) {
		return type.ability().choppingTool() || WarPickAbility.isFlipped(player.getMainHandItem()) || !player.isCreative();
	}

	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		if (WarPickAbility.isFlipped(stack)) {
			return WarPickAbility.destroySpeed(material, stack, state);
		}
		return type.ability().choppingTool() && choppable(stack, state) ? getTier().getSpeed() : 1.0f;
	}

	/**
	 * There is no {@code #minecraft:mineable/axe} tag before 1.17; the axe's block list is private,
	 * so ask a vanilla axe: it digs faster than by hand exactly on the blocks it is the tool for.
	 */
	private static boolean choppable(ItemStack stack, BlockState state) {
		return Items.WOODEN_AXE.getDestroySpeed(stack, state) > 1.0f;
	}

	@Override
	public boolean mineBlock(ItemStack stack, World level, BlockState state, BlockPos pos, LivingEntity miner) {
		if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0f) {
			int cost = type.ability().choppingTool() || WarPickAbility.isFlipped(stack) ? CHOPPING_BLOCK_DAMAGE : BLOCK_DAMAGE;
			stack.hurtAndBreak(cost, miner, entity -> entity.broadcastBreakEvent(EquipmentSlotType.MAINHAND));
		}
		return true;
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		type.ability().onHit(stack, target, attacker);
		stack.hurtAndBreak(1, attacker, entity -> entity.broadcastBreakEvent(EquipmentSlotType.MAINHAND));
		return true;
	}

	// --- ability hooks ---

	@Override
	public ActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
		ActionResult<ItemStack> result = type.ability().onUse(level, player, hand);
		return result.getResult().consumesAction() ? result : super.use(level, player, hand);
	}

	@Override
	public UseAction getUseAnimation(ItemStack stack) {
		return type.ability().useAnimation();
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return type.ability().useDuration();
	}

	@Override
	public void onUseTick(World level, LivingEntity user, ItemStack stack, int remainingTicks) {
		type.ability().onUseTick(stack, level, user, remainingTicks);
	}

	@Override
	public void releaseUsing(ItemStack stack, World level, LivingEntity user, int remainingTicks) {
		type.ability().onReleaseUsing(stack, level, user, remainingTicks);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, World level, LivingEntity user) {
		type.ability().onFinishUsing(stack, level, user);
		return stack;
	}
}
