package net.firstwemine1.weaponry.mixin;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** An arrow's base damage, which the bows scale. There is a setter but no getter in this version. */
@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
	@Accessor("baseDamage")
	double weaponry$baseDamage();
}
