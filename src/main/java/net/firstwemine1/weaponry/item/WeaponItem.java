package net.firstwemine1.weaponry.item;

import java.util.List;

import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A melee weapon: one {@link WeaponType} in one {@link WeaponMaterial}. Behaves like a sword minus
 * the sweep attack; everything else comes from the type's {@link WeaponAbility}.
 */
public class WeaponItem extends Item {
	/** Vanilla sword damage is 1 (player base) + 3 + tier bonus. */
	private static final int SWORD_BASE_DAMAGE = 3;
	private static final float PLAYER_BASE_DAMAGE = 1.0f;
	private static final float PLAYER_BASE_ATTACK_SPEED = 4.0f;
	private static final double PLAYER_BASE_REACH = 3.0;
	private static final ResourceLocation REACH_MODIFIER_ID = Weaponry.id("weapon_reach");
	/** Not a tool: no fast-mined blocks, and breaking a block costs 2 durability, like a sword. */
	private static final Tool TOOL = new Tool(List.of(), 1.0f, 2);
	public static final String TWO_HANDED_TOOLTIP_KEY = "tooltip." + Weaponry.MOD_ID + ".two_handed";
	public static final String TWO_HANDED_MESSAGE_KEY = "message." + Weaponry.MOD_ID + ".two_handed";
	public static final String TWO_HANDED_MOVED_KEY = TWO_HANDED_MESSAGE_KEY + ".moved";
	public static final String TWO_HANDED_FULL_KEY = TWO_HANDED_MESSAGE_KEY + ".full";

	private final WeaponType type;
	private final WeaponMaterial material;
	private final float attackDamage;

	public WeaponItem(WeaponType type, WeaponMaterial material, Properties properties) {
		this(type, material, new WeaponTier(material.tier(), type.durabilityMultiplier()), properties);
	}

	private WeaponItem(WeaponType type, WeaponMaterial material, WeaponTier tier, Properties properties) {
		super(tier.applyCommonProperties(properties)
				.attributes(createAttributes(type, material, tier))
				.component(DataComponents.TOOL, toolOf(type, tier)));
		this.type = type;
		this.material = material;
		this.attackDamage = PLAYER_BASE_DAMAGE + damageModifier(type, tier);
	}

	/** Modifier on top of the player's base 1 damage; never below 0 so a hit always does >= 1. */
	private static float damageModifier(WeaponType type, WeaponTier tier) {
		return Math.max(0, SWORD_BASE_DAMAGE + tier.attackDamageBonus() + type.damageOffset());
	}

	private static Tool toolOf(WeaponType type, WeaponTier tier) {
		Tool tool = type.ability().tool(tier);
		return tool != null ? tool : TOOL;
	}

	public static ItemAttributeModifiers createAttributes(WeaponType type, WeaponMaterial material, WeaponTier tier) {
		ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE,
						new AttributeModifier(BASE_ATTACK_DAMAGE_ID, damageModifier(type, tier), AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED,
						new AttributeModifier(BASE_ATTACK_SPEED_ID, type.attackSpeed() - PLAYER_BASE_ATTACK_SPEED,
								AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.MAINHAND);
		double reachBonus = type.reach() - PLAYER_BASE_REACH;
		if (reachBonus != 0) {
			builder.add(Attributes.ENTITY_INTERACTION_RANGE,
					new AttributeModifier(REACH_MODIFIER_ID, reachBonus, AttributeModifier.Operation.ADD_VALUE),
					EquipmentSlotGroup.MAINHAND);
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
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		int lines = type.ability().description().size();
		for (int i = 0; i < lines; i++) {
			tooltip.add(Component.translatable(tooltipKey(type, i)).withStyle(ChatFormatting.GRAY));
		}
		if (type.twoHanded()) {
			tooltip.add(Component.translatable(TWO_HANDED_TOOLTIP_KEY).withStyle(ChatFormatting.DARK_GRAY));
		}
	}

	// --- sword-like durability handling ---

	@Override
	public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
		// The stack's tool, not the item's: a flipped war pick mines.
		return !TOOL.equals(player.getMainHandItem().get(DataComponents.TOOL)) || !player.isCreative();
	}

	@Override
	public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
		if (source.getEntity() instanceof LivingEntity attacker) {
			return type.ability().attackDamageBonus(this, attacker, target, damage);
		}
		return 0;
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		type.ability().onHit(stack, target, attacker);
		return true;
	}

	@Override
	public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
	}

	// --- ability hooks ---

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		InteractionResult result = type.ability().onUse(level, player, hand);
		return result.consumesAction() ? result : super.use(level, player, hand);
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return type.ability().useAnimation();
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user) {
		return type.ability().useDuration();
	}

	@Override
	public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingTicks) {
		type.ability().onUseTick(stack, level, user, remainingTicks);
	}

	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingTicks) {
		return type.ability().onReleaseUsing(stack, level, user, remainingTicks);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
		type.ability().onFinishUsing(stack, level, user);
		return stack;
	}
}
