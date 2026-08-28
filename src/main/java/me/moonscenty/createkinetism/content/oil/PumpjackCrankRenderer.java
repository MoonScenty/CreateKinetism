package me.moonscenty.createkinetism.content.oil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Draws the half shaft going into the crank at network speed, and the crank wheel itself at the
 * saturated speed the block entity tracks - so the shaft can be screaming while the crank still
 * turns at a pumpjack's pace.</p>
 */
public class PumpjackCrankRenderer extends KineticBlockEntityRenderer<PumpjackCrankBlockEntity> {

	public PumpjackCrankRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(PumpjackCrankBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource bufferSource, int light, int overlay) {

		VertexConsumer solid = bufferSource.getBuffer(RenderType.solid());
		BlockState blockState = be.getBlockState();
		Direction facing = blockState.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);

		SuperByteBuffer crank = CachedBuffers.partialFacing(CKPartialModels.PUMPJACK_CRANK, blockState, facing);
		SuperByteBuffer halfShaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, blockState, facing);

		standardKineticRotationTransform(halfShaft, be, light);
		halfShaft.renderInto(ms, solid);

		float speed = be.visualSpeed.getValue(partialTicks) * 3 / 10f;
		float angle = be.angle + speed * partialTicks;

		crank.light(light)
			.translate(0, 3 / 16f, 0)
			.rotateCentered(angle * Mth.DEG_TO_RAD, facing.getClockWise())
			.translate(0, -3 / 16f, 0)
			.renderInto(ms, solid);
	}
}
