package me.moonscenty.createkinetism.content.vibrator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws two things the static block model cannot: the shaft spinning through the housing, and the
 * shaking half of the machine on top of it - the head, the installed basin sitting on its rim, and
 * whatever fluid is in that basin, all three moving together as one assembly.
 *
 * <p>The shaft is Create's own generic shaft block, rendered the way the Speed Controller renders
 * one through its own housing: it fills the gap the static model leaves at the front and back faces
 * and actually turns, which a chunk-baked model never could. It does not shake - only the head and
 * the basin do, since a real shaft has to stay put to keep driving whatever is behind it.</p>
 */
public class PurificationVibratorRenderer extends KineticBlockEntityRenderer<PurificationVibratorBlockEntity> {

	public PurificationVibratorRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRenderOffScreen(PurificationVibratorBlockEntity be) {
		return true;
	}

	@Override
	protected void renderSafe(PurificationVibratorBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {

		// No Flywheel bail-out, for the same reason VatRenderer has none: with no Visual counterpart
		// returning early would delete the shaft, the head and the basin outright.
		BlockState blockState = be.getBlockState();

		// The solid-type shaft has to be drawn - and its buffer fetched and finished with - before the
		// cutout-type buffer below is grabbed. MultiBufferSource ends the previous type's batch the
		// moment a different one is requested, so a VertexConsumer fetched earlier and reused after a
		// type switch is already closed; renderInto on it throws "Not building!" instead of drawing.
		standardKineticRotationTransform(
			CachedBuffers.block(KINETIC_BLOCK, shaft(blockState.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS))),
			be, light)
				.renderInto(ms, buffer.getBuffer(RenderType.solid()));

		float shake = be.getShakeOffset(AnimationTickHolder.getRenderTime(be.getLevel()));

		CachedBuffers.partial(CKPartialModels.PURIFICATION_VIBRATOR_HEAD, blockState)
			.translate(0, shake, 0)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

		if (!be.hasBasin())
			return;

		CachedBuffers.block(AllBlocks.BASIN.getDefaultState())
			.translate(0, 1 + shake, 0)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

		renderFluid(be, shake, ms, buffer, light);
	}

	/** The contents of the installed basin, drawn the way Create's own basin draws them. */
	private void renderFluid(PurificationVibratorBlockEntity be, float shake, PoseStack ms,
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
			float yMin = 1 + shake + 2 / 16f;
			float yMax = yMin + (10 / 16f) * level;

			ms.pushPose();
			NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid, min, yMin, min, max, yMax, max,
				buffer, ms, light, false, true);
			ms.popPose();
			return;
		}
	}
}
