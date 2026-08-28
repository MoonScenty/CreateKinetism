package me.moonscenty.createkinetism.content.oil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Draws the mode dial. Like Create's Stressometer it appears on every face that is both allowed
 * by the mounting orientation and actually visible, so a controller wedged into a wall only shows
 * the dial on the sides you can reach.</p>
 */
public class DistillationControllerRenderer extends SafeBlockEntityRenderer<DistillationControllerBlockEntity> {

	public DistillationControllerRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(DistillationControllerBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource bufferSource, int light, int overlay) {

		BlockState state = be.getBlockState();
		SuperByteBuffer head = CachedBuffers.partial(CKPartialModels.DISTILLATION_SELECTOR, state);

		for (Direction facing : Iterate.directions) {
			if (!DistillationControllerBlock.shouldRenderHeadOnFace(be.getLevel(), be.getBlockPos(), state, facing))
				continue;
			VertexConsumer vb = bufferSource.getBuffer(RenderType.solid());
			rotateBufferTowards(head, facing).light(light)
				.renderInto(ms, vb);
		}
	}

	private static SuperByteBuffer rotateBufferTowards(SuperByteBuffer buffer, Direction target) {
		return buffer.rotateCentered((float) ((-target.toYRot() - 90) / 180 * Math.PI), Direction.UP);
	}
}
