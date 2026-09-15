package me.moonscenty.createkinetism.mixin;

import com.simibubi.create.content.processing.recipe.HeatCondition;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Create's private {@link HeatCondition} constructor, for {@link HeatConditionMixin} to append constants with. */
@Mixin(value = HeatCondition.class, remap = false)
public interface HeatConditionAccessor {

	@Invoker("<init>")
	static HeatCondition createkinetism$create(String name, int ordinal, int color) {
		throw new AssertionError();
	}
}
