package me.moonscenty.createkinetism.content.chemical;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws what is in the tank behind its window.
 *
 * <p>The proportions are Create's, kept so this reads as one of its fluid tanks: the lid and the
 * foot each swallow a quarter of the block, the hull is a pixel and a bit thick, and there is a
 * shallow puddle floor so a nearly empty tank still shows something. Unlike the Steel Tank this one
 * never combines, so the box is always a single 1x1x1.</p>
 */
public class ChemicalTankRenderer extends SafeBlockEntityRenderer<ChemicalTankBlockEntity> {

	private static final float CAP_HEIGHT = 1 / 4f;
	private static final float HULL_WIDTH = 1 / 16f + 1 / 128f;
	private static final float MIN_PUDDLE = 1 / 16f;

	public ChemicalTankRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(ChemicalTankBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {

		FluidStack fluidStack = be.getTankContents();
		if (fluidStack.isEmpty())
			return;

		float totalHeight = 1 - 2 * CAP_HEIGHT - MIN_PUDDLE;
		float fill = be.fluidLevel.getValue(partialTicks);
		if (fill < 1 / (512f * totalHeight))
			return;
		float filled = Mth.clamp(fill * totalHeight, 0, totalHeight);

		float xMin = HULL_WIDTH;
		float xMax = 1 - HULL_WIDTH;
		float zMin = HULL_WIDTH;
		float zMax = 1 - HULL_WIDTH;
		float yMin = totalHeight + CAP_HEIGHT + MIN_PUDDLE - filled;
		float yMax = yMin + filled;

		// Gases pool against the ceiling rather than the floor - most of what this tank holds is one.
		if (fluidStack.getFluid()
			.getFluidType()
			.isLighterThanAir()) {
			yMin += totalHeight - filled;
			yMax += totalHeight - filled;
		}

		ms.pushPose();
		ms.translate(0, filled - totalHeight, 0);
		NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluidStack, xMin, yMin, zMin, xMax, yMax, zMax,
			buffer, ms, light, false, true);
		ms.popPose();
	}
}
