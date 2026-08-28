package me.moonscenty.createkinetism.content.steel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import me.moonscenty.createkinetism.content.oil.DistillationControllerBlockEntity;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Two modes. A tank with windows draws its contents the way Create's does; a windowless one that
 * has been claimed as a fractionating column draws a gauge on every exposed side instead, with the
 * needle tracking the controller's progress. That second mode is the only thing that ever shows a
 * column's state, since a column has no other readout.</p>
 */
public class SteelTankRenderer extends SafeBlockEntityRenderer<SteelTankBlockEntity> {

	public SteelTankRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(SteelTankBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		if (!be.isController())
			return;

		if (!be.hasWindows()) {
			if (be.isDistillingColumn)
				renderAsDistiller(be, partialTicks, ms, buffer, light, overlay);
			return;
		}

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

	/** Draws the column's pressure gauge on each side that is not buried against another tank. */
	protected void renderAsDistiller(SteelTankBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {

		BlockState blockState = be.getBlockState();
		VertexConsumer vb = buffer.getBuffer(RenderType.cutout());

		ms.pushPose();
		TransformStack.of(ms)
			.translate(be.getWidth() / 2f, 0.5, be.getWidth() / 2f);

		float dialPivotY = 6f / 16;
		float dialPivotZ = 8f / 16;
		DistillationControllerBlockEntity controller = be.getDistillationControllerBE();
		float progress = controller == null ? 0 : controller.gaugeLevel.getValue(partialTicks);

		for (Direction d : Iterate.horizontalDirections) {
			if (be.occludedDirections[d.get2DDataValue()])
				continue;

			ms.pushPose();
			float yRot = -d.toYRot() - 90;

			CachedBuffers.partial(CKPartialModels.DISTILLATION_GAUGE, blockState)
				.rotateYDegrees(yRot)
				.uncenter()
				.translate(be.getWidth() / 2f - 6 / 16f, 0, 0)
				.light(light)
				.renderInto(ms, vb);

			CachedBuffers.partial(CKPartialModels.DISTILLATION_GAUGE_DIAL, blockState)
				.rotateYDegrees(yRot)
				.uncenter()
				.translate(be.getWidth() / 2f - 6 / 16f, 0, 0)
				.translate(0, dialPivotY, dialPivotZ)
				.rotateXDegrees(-145 * progress + 90)
				.translate(0, -dialPivotY, -dialPivotZ)
				.light(light)
				.renderInto(ms, vb);

			ms.popPose();
		}

		ms.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen(SteelTankBlockEntity be) {
		return be.isController();
	}
}
