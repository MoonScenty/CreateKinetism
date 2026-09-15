package me.moonscenty.createkinetism.content.condensentrator;

import com.mojang.blaze3d.vertex.PoseStack;
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
 * {@code DissolutionChamberRenderer} upside down: the shaft through the housing, the table hanging
 * under it, and the installed basin a block below - right way up, so its fluid still sits in it.
 *
 * <p>The head model is already flipped (turned 180 degrees about the shaft), so only the basin's place
 * and the motion change here. Where the chamber rocks, this spins: while a recipe runs the table, the
 * basin and its fluid turn together about the block's vertical centre line, clockwise seen from
 * above - see {@link MechanicalCondensentratorBlockEntity#getSpinAngle}. The fluid shown in the basin
 * is the machine's fluid tank.</p>
 */
public class MechanicalCondensentratorRenderer extends KineticBlockEntityRenderer<MechanicalCondensentratorBlockEntity> {

	public MechanicalCondensentratorRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRenderOffScreen(MechanicalCondensentratorBlockEntity be) {
		return true;
	}

	@Override
	protected void renderSafe(MechanicalCondensentratorBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {

		BlockState blockState = be.getBlockState();
		Axis axis = blockState.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS);

		// Solid shaft first, and its buffer finished, before the cutout buffer is requested - see
		// DissolutionChamberRenderer.
		standardKineticRotationTransform(CachedBuffers.block(KINETIC_BLOCK, shaft(axis)), be, light)
			.renderInto(ms, buffer.getBuffer(RenderType.solid()));

		float angle = be.getSpinAngle(AnimationTickHolder.getRenderTime(be.getLevel()));

		spin(CachedBuffers.partial(CKPartialModels.MECHANICAL_CONDENSENTRATOR_HEAD, blockState), angle)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

		if (!be.hasBasin())
			return;

		spin(CachedBuffers.partial(CKPartialModels.CLOSED_BASIN, blockState), angle)
			.translate(0, -1, 0)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

		renderFluid(be, angle, ms, buffer, light);
	}

	/** Turns a part about the block's vertical centre line. */
	private static SuperByteBuffer spin(SuperByteBuffer buffer, float angle) {
		return buffer.rotateCentered(angle * Mth.DEG_TO_RAD, Direction.UP);
	}

	/** The fluid tank, drawn in the basin the way Create's own basin draws its contents. */
	private void renderFluid(MechanicalCondensentratorBlockEntity be, float angle, PoseStack ms,
		MultiBufferSource buffer, int light) {

		if (be.fluidTank == null)
			return;

		for (TankSegment segment : be.fluidTank.getTanks()) {
			FluidStack fluid = segment.getRenderedFluid();
			float level = segment.getFluidLevel()
				.getValue(AnimationTickHolder.getPartialTicks());
			if (fluid.isEmpty() || level == 0)
				continue;

			float min = 2 / 16f;
			float max = 14 / 16f;
			// A basin holds its fluid between y2 and y12 of its own model, one block down from us.
			float yMin = -1 + 2 / 16f;
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
