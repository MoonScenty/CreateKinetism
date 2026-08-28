package me.moonscenty.createkinetism.content.oil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Four pistons a quarter-turn apart, so the engine reads as a running four-cylinder rather than
 * one part bobbing. Each is offset along the block's own right and up vectors, which is what lets a
 * single model serve every facing.</p>
 *
 * <p>The body itself renders as a shaft, so the engine visually continues the shaft line it sits
 * in.</p>
 */
public class FuelEngineRenderer extends KineticBlockEntityRenderer<FuelEngineBlockEntity> {

	private static final float STROKE = 1 / 16f;

	public FuelEngineRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(FuelEngineBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource bufferSource, int light, int overlay) {
		super.renderSafe(be, partialTicks, ms, bufferSource, light, overlay);

		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		VertexConsumer solid = bufferSource.getBuffer(RenderType.solid());
		BlockState blockState = be.getBlockState();
		Direction facing = blockState.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);

		SuperByteBuffer piston =
			CachedBuffers.partialFacing(CKPartialModels.GASOLINE_ENGINE_PISTON, blockState, facing);

		Vec3 front = new Vec3(facing.step());
		Vec3 up = new Vec3(Direction.UP.step());
		Vec3 right = front.cross(up);

		// SuperByteBuffer resets its transform after each renderInto, so the four cylinders are
		// independent even though they share one buffer.
		float t = AnimationTickHolder.getRenderTime() / 20f * be.getSpeed() / 60f * Mth.TWO_PI;

		stroke(piston, right, up, t).light(light)
			.renderInto(ms, solid);

		t += Mth.HALF_PI;
		piston.translate(front.scale(7 / 16f));
		stroke(piston, right, up, t).light(light)
			.renderInto(ms, solid);

		t += Mth.HALF_PI;
		piston.rotateCentered(Mth.PI, Direction.Axis.Y);
		stroke(piston, right, up, t).light(light)
			.renderInto(ms, solid);

		t += Mth.HALF_PI;
		piston.translate(front.scale(-7 / 16f))
			.rotateCentered(Mth.PI, Direction.Axis.Y);
		stroke(piston, right, up, t).light(light)
			.renderInto(ms, solid);
	}

	/** Offsets a piston along the block's own right and up vectors, which keeps one model per facing. */
	private static SuperByteBuffer stroke(SuperByteBuffer piston, Vec3 right, Vec3 up, float t) {
		float offset = Mth.sin(t) * STROKE + STROKE;
		return piston.translate(right.scale(offset)
			.scale(0.707))
			.translate(up.scale(offset)
				.scale(0.707));
	}

	@Override
	protected BlockState getRenderedBlockState(FuelEngineBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}
}
