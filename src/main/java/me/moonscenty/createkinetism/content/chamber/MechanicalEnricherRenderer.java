package me.moonscenty.createkinetism.content.chamber;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The fallback path for the enricher, mirroring Create's {@code MechanicalPressRenderer}.
 *
 * <p>Note the early return: {@code KineticBlockEntityRenderer} stands down the moment Flywheel is
 * available, so on a default install nothing here runs and
 * {@link MechanicalEnricherVisual} does the drawing instead. Both exist for the same reason Create
 * ships both.</p>
 */
public class MechanicalEnricherRenderer extends KineticBlockEntityRenderer<MechanicalEnricherBlockEntity> {

	public MechanicalEnricherRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	/** The poles stand a block above their own block, so they must survive being off screen. */
	@Override
	public boolean shouldRenderOffScreen(MechanicalEnricherBlockEntity be) {
		return true;
	}

	@Override
	protected void renderSafe(MechanicalEnricherBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		BlockState state = be.getBlockState();
		EnrichingBehaviour pressing = be.getPressingBehaviour();
		float headOffset = pressing.getRenderedHeadOffset(partialTicks) * pressing.mode.headOffset;

		CachedBuffers.partialFacing(CKPartialModels.MECHANICAL_ENRICHER_HEAD, state,
			state.getValue(HORIZONTAL_FACING))
			.translate(0, -headOffset, 0)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.solid()));
	}

	/** The kinetic pass draws a shaft on the rotation axis, exactly as the press does. */
	@Override
	protected BlockState getRenderedBlockState(MechanicalEnricherBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}

	@Override
	protected SuperByteBuffer getRotatedModel(MechanicalEnricherBlockEntity be, BlockState state) {
		return CachedBuffers.block(KINETIC_BLOCK, state);
	}
}
