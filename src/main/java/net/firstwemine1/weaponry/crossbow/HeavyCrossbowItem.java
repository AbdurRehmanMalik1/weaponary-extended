package net.firstwemine1.weaponry.crossbow;

import java.util.List;

import net.firstwemine1.weaponry.Weaponry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
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
 * comes from the explosion). Slow Crank: 1.6 s to load, with lower-pitched load sounds. The load time,
 * sounds and arrow all live in static or private vanilla methods, so {@code CrossbowItemMixin} sets them.
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

	/** Vanilla's load sounds at a lower pitch, set in {@code assets/weaponry/sounds.json}. */
	public static final SoundEvent LOADING_START = new SoundEvent(Weaponry.id("item.heavy_crossbow.loading_start"));
	public static final SoundEvent LOADING_MIDDLE = new SoundEvent(Weaponry.id("item.heavy_crossbow.loading_middle"));
	public static final SoundEvent LOADING_END = new SoundEvent(Weaponry.id("item.heavy_crossbow.loading_end"));

	public HeavyCrossbowItem(Properties properties) {
		super(properties.durability(DURABILITY));
	}

	static void registerSounds() {
		for (SoundEvent sound : List.of(LOADING_START, LOADING_MIDDLE, LOADING_END)) {
			Registry.register(Registry.SOUND_EVENT, sound.getLocation(), sound);
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
