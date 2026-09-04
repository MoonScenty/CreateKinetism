package me.moonscenty.createkinetism.content.dissolution;

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
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws the shaft turning through the housing and the rocking half of the machine above it - the
 * table, the installed basin sitting on it, and whatever fluid is in that basin, all three tipping
 * together as one assembly.
 *
 * <p>Everything tips about the same pivot: the underside of the table, on the shaft's own axis. Use
 * separate pivots and the basin slides off the table as it tilts.</p>
 */
public class DissolutionChamberRenderer extends KineticBlockEntityRenderer<DissolutionChamberBlockEntity> {

	/** The table's underside, where it meets the piston. */
	private static final float PIVOT_Y = 12 / 16f;

	public DissolutionChamberRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRenderOffScreen(DissolutionChamberBlockEntity be) {
		return true;
	}

	@Override
	protected void renderSafe(DissolutionChamberBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {

		// No Flywheel bail-out, for the same reason the vibrator has none: with no Visual counterpart
		// returning early would delete the shaft, the table and the basin outright.
		BlockState blockState = be.getBlockState();
		Axis axis = blockState.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS);

		// The solid-type shaft has to be drawn - and its buffer finished with - before the cutout
		// buffer below is grabbed. MultiBufferSource ends the previous type's batch the moment a
		// different one is requested, so a VertexConsumer fetched earlier and reused after a type
		// switch is already closed and throws "Not building!" instead of drawing.
		standardKineticRotationTransform(CachedBuffers.block(KINETIC_BLOCK, shaft(axis)), be, light)
			.renderInto(ms, buffer.getBuffer(RenderType.solid()));

		float angle = be.getRockAngle(AnimationTickHolder.getRenderTime(be.getLevel()));

		rock(CachedBuffers.partial(CKPartialModels.DISSOLUTION_CHAMBER_HEAD, blockState), axis, angle)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

		if (!be.hasBasin())
			return;

		rock(CachedBuffers.block(AllBlocks.BASIN.getDefaultState()), axis, angle)
			.translate(0, 1, 0)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

		renderFluid(be, axis, angle, ms, buffer, light);
	}

	/** Tips a part about the table's pivot, around whichever axis the shaft runs along. */
	private static SuperByteBuffer rock(SuperByteBuffer buffer, Axis axis, float angle) {
		buffer.translate(-0.5, -PIVOT_Y, -0.5);
		if (axis == Axis.X)
			buffer.rotateXDegrees(angle);
		else
			buffer.rotateZDegrees(angle);
		return buffer.translate(0.5, PIVOT_Y, 0.5);
	}

	/** The contents of the installed basin, drawn the way Create's own basin draws them. */
	private void renderFluid(DissolutionChamberBlockEntity be, Axis axis, float angle, PoseStack ms,
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
			// Same pivot as the parts above, applied to the stack because the fluid renderer draws
			// straight into it rather than through a SuperByteBuffer.
			ms.translate(0.5, PIVOT_Y, 0.5);
			if (axis == Axis.X)
				ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(angle));
			else
				ms.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(angle));
			ms.translate(-0.5, -PIVOT_Y, -0.5);

			NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid, min, yMin, min, max, yMax, max,
				buffer, ms, light, false, true);
			ms.popPose();
			return;
		}
	}
}
