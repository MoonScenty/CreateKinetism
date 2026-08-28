package me.moonscenty.createkinetism.content.oil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Draws the mounting collar against the column. Which of the two base models is used is what
 * shows, at a glance across the refinery, whether a tap is storing its cut or dumping it.</p>
 */
public class DistillationOutputRenderer extends SafeBlockEntityRenderer<DistillationOutputBlockEntity> {

	public DistillationOutputRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(DistillationOutputBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource bufferSource, int light, int overlay) {

		BlockState state = be.getBlockState();
		PartialModel model = state.getValue(DistillationOutputBlock.POWERED)
			? CKPartialModels.DISTILLATION_OUTPUT_BASE_POWERED
			: CKPartialModels.DISTILLATION_OUTPUT_BASE_UNPOWERED;

		SuperByteBuffer base =
			CachedBuffers.partialFacing(model, state, state.getValue(DistillationOutputBlock.TANK_FACE));
		base.light(light)
			.renderInto(ms, bufferSource.getBuffer(RenderType.solid()));
	}
}
