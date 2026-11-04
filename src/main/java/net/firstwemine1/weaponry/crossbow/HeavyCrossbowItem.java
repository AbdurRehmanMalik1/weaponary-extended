package net.firstwemine1.weaponry.crossbow;

import java.util.List;
import java.util.Optional;

import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.Level;

/**
 * Loads and shoots like vanilla's crossbow, with two changes from {@code crossbow-stats.md} §3. Heavy
 * Bolt: arrows do x1.4 base damage and have built-in Punch II (rockets are unchanged, their damage
 * comes from the explosion). Slow Crank: 1.6 s to load, with lower-pitched load sounds. The load time
 * and sounds live in static or package-private vanilla methods, so {@code CrossbowItemMixin} sets them.
 */
public class HeavyCrossbowItem extends CrossbowItem implements WeaponCrossbow {
	/** Vanilla's is 1.25 s. Quick Charge still takes 0.25 s off per level. */
	public static final float CHARGE_SECONDS = 1.6f;
	/** Entity tag on its arrows; the arrow mixin adds {@link #PUNCH} to their knockback. */
	public static final String HEAVY_BOLT = "weaponry.heavy_bolt";
	/** Punch II: Punch adds 1 per level. */
	public static final float PUNCH = 2.0f;
	private static final double DAMAGE = 1.4;
	private static final int DURABILITY = 520;

	/** Vanilla's load sounds at a lower pitch, set in {@code assets/weaponry/sounds.json}. */
	private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Weaponry.MOD_ID);
	private static final DeferredHolder<SoundEvent, SoundEvent> LOADING_START = sound("item.heavy_crossbow.loading_start");
	private static final DeferredHolder<SoundEvent, SoundEvent> LOADING_MIDDLE = sound("item.heavy_crossbow.loading_middle");
	private static final DeferredHolder<SoundEvent, SoundEvent> LOADING_END = sound("item.heavy_crossbow.loading_end");

	public HeavyCrossbowItem(Properties properties) {
		super(properties.durability(DURABILITY).component(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY));
	}

	private static DeferredHolder<SoundEvent, SoundEvent> sound(String path) {
		return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(Weaponry.id(path)));
	}

	static void registerSounds(IEventBus modBus) {
		SOUNDS.register(modBus);
	}

	public static ChargingSounds sounds() {
		return new ChargingSounds(Optional.of(LOADING_START), Optional.of(LOADING_MIDDLE), Optional.of(LOADING_END));
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
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		for (int i = 0; i < description().size(); i++) {
			tooltip.add(Component.translatable(ModCrossbows.tooltipKey(this, i)).withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean crit) {
		Projectile projectile = super.createProjectile(level, shooter, weapon, ammo, crit);
		if (projectile instanceof AbstractArrow arrow) {
			arrow.setBaseDamage(arrow.getBaseDamage() * DAMAGE);
			arrow.addTag(HEAVY_BOLT);
		}
		return projectile;
	}
}
