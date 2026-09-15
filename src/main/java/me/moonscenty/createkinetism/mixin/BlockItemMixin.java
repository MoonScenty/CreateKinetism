package me.moonscenty.createkinetism.mixin;

import me.moonscenty.createkinetism.content.condensentrator.CondensentratorPlacement;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;

/**
 * Leaves a block free under a Mechanical Condensentrator when a heater and a condensentrator are
 * placed against each other - see {@link CondensentratorPlacement}.
 *
 * <p>Hooked on {@code BlockItem} itself rather than on each item, because the heaters come from three
 * item classes, two of them Create's.</p>
 */
@Mixin(value = BlockItem.class, remap = false)
public abstract class BlockItemMixin {

	@Inject(method = "updatePlacementContext", at = @At("RETURN"), cancellable = true)
	private void createkinetism$condensentratorGap(BlockPlaceContext context,
		CallbackInfoReturnable<BlockPlaceContext> cir) {
		BlockPlaceContext current = cir.getReturnValue();
		if (current == null)
			return;
		BlockPlaceContext adjusted = CondensentratorPlacement.adjust((BlockItem) (Object) this, current);
		if (adjusted != current)
			cir.setReturnValue(adjusted);
	}
}
