package me.moonscenty.createkinetism.content.evaporation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.fluid.SmartFluidTank;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Ported from the Steel Tank template ({@code SteelTankRenderer}), which itself reuses Create's own
 * fluid-box drawing - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Draws what is inside through the windows, the same way Create's own tank does - but once the
 * stack is tall enough to split into feed and product (see {@link EvaporationPlantBlockEntity#isActive()}),
 * the whole column shows the product rather than the feed: the feed floor has no window worth
 * showing on its own, so letting the product rise through it too as one continuous column reads
 * better than a permanent gap at the bottom that never fills.</p>
 */
public class EvaporationPlantRenderer extends SafeBlockEntityRenderer<EvaporationPlantBlockEntity> {

	private static final float CAP_HEIGHT = 1 / 4f;
	private static final float TANK_HULL_WIDTH = 1 / 16f + 1 / 128f;
	private static final float MIN_PUDDLE_HEIGHT = 1 / 16f;

	public EvaporationPlantRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(EvaporationPlantBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
		if (!be.isController() || !be.hasWindows())
			return;

		if (be.isActive()) {
			SmartFluidTank output = be.getOutputTank();
			if (output == null)
				return;
			// Drawn across the whole stack rather than starting at floor 2: the feed floor's own
			// window shows nothing on its own anyway, so filling it in as part of one continuous
			// rising column reads better than a gap that never fills.
			float level = output.getCapacity() > 0 ? output.getFluidAmount() / (float) output.getCapacity() : 0;
			renderFluidColumn(output.getFluid(), level, be.getHeight(), be.getWidth(), ms, buffer, light);
			return;
		}

		LerpedFloat fluidLevel = be.getFluidLevel();
		if (fluidLevel == null)
			return;
		renderFluidColumn(be.getTankInventory()
			.getFluid(), fluidLevel.getValue(partialTicks), be.getHeight(), be.getWidth(), ms, buffer, light);
	}

	/** Draws one fluid box the full {@code span} floors of the stack tall, capped top and bottom. */
	private void renderFluidColumn(FluidStack fluidStack, float level, int span, int width, PoseStack ms,
		MultiBufferSource buffer, int light) {
		if (fluidStack.isEmpty())
			return;

		float totalHeight = span - 2 * CAP_HEIGHT - MIN_PUDDLE_HEIGHT;
		if (totalHeight <= 0 || level < 1 / (512f * totalHeight))
			return;
		float clampedLevel = Mth.clamp(level * totalHeight, 0, totalHeight);

		// Gases pool against the ceiling rather than the floor.
		boolean top = fluidStack.getFluid()
			.getFluidType()
			.isLighterThanAir();

		float xMin = TANK_HULL_WIDTH;
		float xMax = xMin + width - 2 * TANK_HULL_WIDTH;
		float zMin = TANK_HULL_WIDTH;
		float zMax = zMin + width - 2 * TANK_HULL_WIDTH;

		float yMin, yMax;
		if (top) {
			yMax = span - CAP_HEIGHT;
			yMin = yMax - clampedLevel;
		} else {
			yMin = CAP_HEIGHT + MIN_PUDDLE_HEIGHT;
			yMax = yMin + clampedLevel;
		}

		ms.pushPose();
		NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluidStack, xMin, yMin, zMin, xMax, yMax, zMax, buffer, ms,
			light, false, true);
		ms.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen(EvaporationPlantBlockEntity be) {
		return be.isController();
	}
}
