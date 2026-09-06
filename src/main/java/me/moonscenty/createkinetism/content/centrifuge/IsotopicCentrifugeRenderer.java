package me.moonscenty.createkinetism.content.centrifuge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws the shaft turning through the housing and the table above it swinging about the vertical -
 * the table, the basin fitted into it, and whatever is in that basin, all three turning as one.
 *
 * <p>{@link me.moonscenty.createkinetism.content.dissolution.DissolutionChamberRenderer} with the
 * rock swapped for a turn. Because the turn is about Y, the pivot is simply the block's own centre
 * and the sandwich that renderer needs to keep the basin from sliding off is unnecessary here.</p>
 */
public class IsotopicCentrifugeRenderer extends KineticBlockEntityRenderer<IsotopicCentrifugeBlockEntity> {

	public IsotopicCentrifugeRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRenderOffScreen(IsotopicCentrifugeBlockEntity be) {
		return true;
	}

	@Override
	protected void renderSafe(IsotopicCentrifugeBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {

		// No Flywheel bail-out, for the same reason the Dissolution Chamber has none: with no Visual
		// counterpart returning early would delete the shaft, the table and the basin outright.
		BlockState blockState = be.getBlockState();
		Axis axis = blockState.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS);

		// Solid first, and finished with, before the cutout buffer below is asked for -
		// MultiBufferSource closes the previous batch the moment a different type is requested.
		standardKineticRotationTransform(CachedBuffers.block(KINETIC_BLOCK, shaft(axis)), be, light)
			.renderInto(ms, buffer.getBuffer(RenderType.solid()));

		float angle = be.getSwingAngle(AnimationTickHolder.getRenderTime(be.getLevel()));

		swing(CachedBuffers.partial(CKPartialModels.ISOTOPIC_CENTRIFUGE_HEAD, blockState), angle)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

		if (!be.hasBasin())
			return;

		swing(CachedBuffers.block(AllBlocks.BASIN.getDefaultState()), angle)
			.translate(0, 1, 0)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

		renderFluid(be, angle, ms, buffer, light);
	}

	/** Turns a part about the block's vertical centre line. */
	private static SuperByteBuffer swing(SuperByteBuffer buffer, float angle) {
		return buffer.rotateCentered(angle * Mth.DEG_TO_RAD, Direction.UP);
	}

	/** The contents of the fitted basin, drawn the way Create's own basin draws them. */
	private void renderFluid(IsotopicCentrifugeBlockEntity be, float angle, PoseStack ms,
		MultiBufferSource buffer, int light) {

		if (be.inputTank == null)
			return;

		for (TankSegment segment : be.inputTank.getTanks()) {
			FluidStack fluid = segment.getRenderedFluid();
			float level = segment.getFluidLevel()
				.getValue(AnimationTickHolder.getPartialTicks());
			if (fluid.isEmpty() || level == 0)
				continue;

			float min = 2 / 16f;
			float max = 14 / 16f;
			// A basin holds its fluid between y2 and y12 of its own model, one block up from us.
			float yMin = 1 + 2 / 16f;
			float yMax = yMin + (10 / 16f) * level;

			ms.pushPose();
			// Same turn as the parts above, applied to the stack because the fluid renderer draws
			// straight into it rather than through a SuperByteBuffer.
			ms.translate(0.5, 0, 0.5);
			ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle));
			ms.translate(-0.5, 0, -0.5);

			NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid, min, yMin, min, max, yMax, max,
				buffer, ms, light, false, true);
			ms.popPose();
			return;
		}
	}
}
