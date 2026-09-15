package me.moonscenty.createkinetism.mixin;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Create's private {@link HeatLevel} constructor, for {@link HeatLevelMixin} to append constants with. */
@Mixin(value = HeatLevel.class, remap = false)
public interface HeatLevelAccessor {

	@Invoker("<init>")
	static HeatLevel createkinetism$create(String name, int ordinal) {
		throw new AssertionError();
	}
}
