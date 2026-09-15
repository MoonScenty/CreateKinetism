package me.moonscenty.createkinetism.mixin;

import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.fluids.tank.BoilerHeaters;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

import me.moonscenty.createkinetism.content.heat.CKHeatLevels;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a burner at one of the new heat levels gives a boiler. Sodium Heated is 3, the same heat a lit
 * Sodium Burner reports through its own heater; Chilled gives none. Left alone, Create would read
 * Sodium Heated as an ordinary 1 and Chilled as passive heat.
 */
@Mixin(value = BoilerHeaters.class, remap = false)
public abstract class BoilerHeatersMixin {

	@Inject(method = "blazeBurner", at = @At("HEAD"), cancellable = true)
	private static void createkinetism$newLevelHeat(Level level, BlockPos pos, BlockState state,
		CallbackInfoReturnable<Integer> cir) {
		HeatLevel value = state.getValue(BlazeBurnerBlock.HEAT_LEVEL);
		if (value == CKHeatLevels.SODIUM_HEATED)
			cir.setReturnValue(3);
		else if (value == CKHeatLevels.CHILLED)
			cir.setReturnValue(BoilerHeater.NO_HEAT);
	}
}
