package me.moonscenty.createkinetism.mixin;

import com.simibubi.create.content.processing.burner.BlazeBurnerHandler;

import me.moonscenty.createkinetism.content.chiller.StrayChillerBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.HitResult;

import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

/**
 * Thrown eggs are not chiller fuel.
 *
 * <p>Create feeds any Blaze Burner block entity an egg thrown at it, with a blaze munching sound - and
 * a Stray Chiller is one. Its fuel is snow and ice only (see {@code StrayChillerBlockEntity#FUEL_SECONDS}),
 * so here the egg is left to break on the chiller like on any other block.</p>
 */
@Mixin(value = BlazeBurnerHandler.class, remap = false)
public abstract class BlazeBurnerHandlerMixin {

	@Inject(method = "thrownEggsGetEatenByBurner", at = @At("HEAD"), cancellable = true)
	private static void createkinetism$chillerIgnoresEggs(ProjectileImpactEvent event, CallbackInfo ci) {
		if (event.getRayTraceResult()
			.getType() != HitResult.Type.BLOCK)
			return;
		BlockPos pos = BlockPos.containing(event.getRayTraceResult()
			.getLocation());
		if (event.getProjectile()
			.level()
			.getBlockEntity(pos) instanceof StrayChillerBlockEntity)
			ci.cancel();
	}
}
