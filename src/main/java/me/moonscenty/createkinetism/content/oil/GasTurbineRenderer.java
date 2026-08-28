package me.moonscenty.createkinetism.content.oil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Three fan stages at staggered offsets and angles, one of them counter-rotating, which is what
 * gives a spinning turbine its layered blur instead of looking like a single disc.</p>
 *
 * <p>Petrochem's turbine drives its own lerped speed because it is an electrical generator with no
 * shaft; ours is a rotational generator, so the fan is driven straight off the network speed and
 * stays in step with everything else on the shaft.</p>
 */
public class GasTurbineRenderer extends SafeBlockEntityRenderer<FuelEngineBlockEntity> {

	public GasTurbineRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(FuelEngineBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource bufferSource, int light, int overlay) {

		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		BlockState blockState = be.getBlockState();
		Direction direction = blockState.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);

		VertexConsumer vb = bufferSource.getBuffer(RenderType.cutoutMipped());
		// Light the fan from the open air in front rather than from inside the casing.
		int lightInFront = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos()
			.relative(direction));

		SuperByteBuffer shaftHalf =
			CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, blockState, direction);
		SuperByteBuffer fan =
			CachedBuffers.partialFacing(CKPartialModels.TURBINE_PROPELLER, blockState, direction.getOpposite());

		float angle = AnimationTickHolder.getRenderTime(be.getLevel()) * be.getSpeed() * 3 / 10f % 360;
		Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());

		shaftHalf.light(lightInFront)
			.translate(normal.scale(-2 / 16f))
			.rotateCenteredDegrees(angle, direction)
			.renderInto(ms, vb);

		fan.light(lightInFront)
			.rotateCenteredDegrees(angle, direction)
			.renderInto(ms, vb);

		fan.light(lightInFront)
			.translate(normal.scale(-1 / 16f))
			.rotateCenteredDegrees(-angle + 30, direction)
			.renderInto(ms, vb);

		fan.light(lightInFront)
			.translate(normal.scale(-2 / 16f))
			.rotateCenteredDegrees(angle + 60, direction)
			.renderInto(ms, vb);
	}
}
