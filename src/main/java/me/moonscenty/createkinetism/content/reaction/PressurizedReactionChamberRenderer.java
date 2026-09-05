package me.moonscenty.createkinetism.content.reaction;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import me.moonscenty.createkinetism.content.vat.VatBlockEntity;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The shaft running through the chamber.
 *
 * <p>Drawn here rather than left to the kinetic pass, which bails out under Flywheel - this machine
 * ships no Visual, so the shaft would simply be missing. Everything else about the chamber is
 * static and lives in the blockstate.</p>
 */
public class PressurizedReactionChamberRenderer extends KineticBlockEntityRenderer<VatBlockEntity> {

	public PressurizedReactionChamberRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(VatBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {

		BlockState state = be.getBlockState();
		Axis axis = state.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS);
		VertexConsumer vb = buffer.getBuffer(RenderType.solid());

		// Turn first, aim second: the buffer's transforms apply to a vertex last-call-first, so the
		// yaw has to be the later call for the shaft to spin about the axis it ends up on.
		SuperByteBuffer shaft = CachedBuffers.partial(CKPartialModels.REACTION_CHAMBER_SHAFT, state);
		kineticRotationTransform(shaft, be, axis, getAngleForBe(be, be.getBlockPos(), axis), light);
		// The model is drawn along X; a chamber on the Z axis needs it turned a quarter.
		if (axis == Axis.Z)
			shaft.rotateCentered((float) (Math.PI / 2), Direction.UP);
		shaft.renderInto(ms, vb);
	}

	/** The static body is the blockstate's job; nothing for the kinetic pass to draw. */
	@Override
	protected BlockState getRenderedBlockState(VatBlockEntity be) {
		return be.getBlockState();
	}
}
