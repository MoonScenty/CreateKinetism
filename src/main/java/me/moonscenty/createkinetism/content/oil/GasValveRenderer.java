package me.moonscenty.createkinetism.content.oil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The shaft, and the handwheel turning a quarter turn with it.
 *
 * <p>Create's own maths for where the pointer ends up: a quarter turn from shut to open, with an
 * extra quarter added when the pipe and shaft axes leave it lying the other way round.</p>
 */
public class GasValveRenderer extends KineticBlockEntityRenderer<GasValveBlockEntity> {

	public GasValveRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(GasValveBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {

		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

		BlockState blockState = be.getBlockState();
		SuperByteBuffer pointer = CachedBuffers.partial(CKPartialModels.GAS_VALVE_POINTER, blockState);
		Direction facing = blockState.getValue(com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock.FACING);

		float pointerRotation = Mth.lerp(be.pointer.getValue(partialTicks), 0, -90);
		Axis pipeAxis = com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock.getPipeAxis(blockState);
		Axis shaftAxis = getRotationAxisOf(be);

		int offset = 0;
		if (pipeAxis.isHorizontal() && shaftAxis == Axis.X || pipeAxis.isVertical())
			offset = 90;

		pointer.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(facing))
			.rotateXDegrees(facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90)
			.rotateYDegrees(offset + pointerRotation)
			.uncenter()
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
	}

	@Override
	protected BlockState getRenderedBlockState(GasValveBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}
}
