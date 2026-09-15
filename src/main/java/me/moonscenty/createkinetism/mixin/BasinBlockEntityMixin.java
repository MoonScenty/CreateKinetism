package me.moonscenty.createkinetism.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

import me.moonscenty.createkinetism.content.boiler.SodiumBurnerBlock;
import me.moonscenty.createkinetism.content.heat.CKHeatLevels;
import me.moonscenty.createkinetism.registry.CKBlocks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Lets a basin see a Sodium Burner underneath it.
 *
 * <p>A basin only reads the {@code blaze} property of whatever is below. The Sodium Burner has no such
 * property - it is a kinetic block with its own {@link SodiumBurnerBlock#LIT} - so it is answered for
 * here: lit, meaning fuelled with the shaft turning, is Sodium Heated. Fuelled without a shaft it only
 * warms a boiler (see {@code SodiumBurnerBlockEntity#heatLevel}); a block state alone cannot tell
 * that apart from cold, so a basin sees it as no heat.</p>
 */
@Mixin(value = BasinBlockEntity.class, remap = false)
public abstract class BasinBlockEntityMixin {

	@Inject(method = "getHeatLevelOf", at = @At("HEAD"), cancellable = true)
	private static void createkinetism$sodiumBurnerHeat(BlockState state, CallbackInfoReturnable<HeatLevel> cir) {
		if (CKBlocks.SODIUM_BURNER.has(state))
			cir.setReturnValue(state.getValue(SodiumBurnerBlock.LIT) ? CKHeatLevels.SODIUM_HEATED : HeatLevel.NONE);
	}
}
