package me.moonscenty.createkinetism.content.evaporation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Ported from the Steel Tank template ({@code SteelTankRenderer}), which itself reuses Create's own
 * fluid-box drawing - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Draws what is inside through the windows, the same way Create's own tank does. There is no
 * second mode here - a plant is always either holding fluid or it is not.</p>
 */
public class EvaporationPlantRenderer extends SafeBlockEntityRenderer<EvaporationPlantBlockEntity> {

	public EvaporationPlantRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(EvaporationPlantBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
		if (!be.isController() || !be.hasWindows())
			return;

		LerpedFloat fluidLevel = be.getFluidLevel();
		if (fluidLevel == null)
			return;

		float capHeight = 1 / 4f;
		float tankHullWidth = 1 / 16f + 1 / 128f;
		float minPuddleHeight = 1 / 16f;
		float totalHeight = be.getHeight() - 2 * capHeight - minPuddleHeight;

		float level = fluidLevel.getValue(partialTicks);
		if (level < 1 / (512f * totalHeight))
			return;
		float clampedLevel = Mth.clamp(level * totalHeight, 0, totalHeight);

		FluidTank tank = be.getTankInventory();
		FluidStack fluidStack = tank.getFluid();
		if (fluidStack.isEmpty())
			return;

		// Gases pool against the ceiling rather than the floor.
		boolean top = fluidStack.getFluid()
			.getFluidType()
			.isLighterThanAir();

		float xMin = tankHullWidth;
		float xMax = xMin + be.getWidth() - 2 * tankHullWidth;
		float yMin = totalHeight + capHeight + minPuddleHeight - clampedLevel;
		float yMax = yMin + clampedLevel;

		if (top) {
			yMin += totalHeight - clampedLevel;
			yMax += totalHeight - clampedLevel;
		}

		float zMin = tankHullWidth;
		float zMax = zMin + be.getWidth() - 2 * tankHullWidth;

		ms.pushPose();
		ms.translate(0, clampedLevel - totalHeight, 0);
		NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluidStack, xMin, yMin, zMin, xMax, yMax, zMax, buffer, ms,
			light, false, true);
		ms.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen(EvaporationPlantBlockEntity be) {
		return be.isController();
	}
}
