package me.moonscenty.createkinetism.content.multimeter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import com.simibubi.create.content.kinetics.gauge.GaugeBlock;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The shaft, the gauge face, and two needles on every face that shows one.
 *
 * <p>Create's {@code GaugeRenderer} with a second dial: the pivot, the quarter-turn sweep and the
 * yaw that points a face outward are all its numbers, because the models are its models. Both
 * needles ride the same maths and differ only in which state they read.</p>
 *
 * <p>Unlike Create's, this one does not bail out when Flywheel is active, and it draws the shaft
 * itself instead of leaving it to the superclass. Create can do both, because it ships a
 * {@code GaugeVisual} to take over; we have no Visual counterpart, so returning early - or calling a
 * {@code renderSafe} that returns early - would simply delete that part on the default backend.</p>
 */
public class MultimeterRenderer extends ShaftRenderer<MultimeterBlockEntity> {

	/** Where the needle is hinged, in Create's own model space. */
	private static final float DIAL_PIVOT = 5.75f / 16;

	public MultimeterRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(MultimeterBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {

		// The shaft. Drawn here rather than through super, whose renderSafe returns early under
		// Flywheel - which would have left a gauge with no axle through it while the face and the
		// needles below still drew.
		BlockState shaft = getRenderedBlockState(be);
		renderRotatingBuffer(be, getRotatedModel(be, shaft), ms, buffer.getBuffer(getRenderType(be, shaft)),
			light);

		BlockState state = be.getBlockState();
		if (!(state.getBlock() instanceof GaugeBlock gauge))
			return;

		SuperByteBuffer head = CachedBuffers.partial(CKPartialModels.MULTIMETER_HEAD, state);
		SuperByteBuffer speedDial = CachedBuffers.partial(CKPartialModels.MULTIMETER_DIAL_SPEED, state);
		SuperByteBuffer stressDial = CachedBuffers.partial(CKPartialModels.MULTIMETER_DIAL_STRESS, state);

		float speed = Mth.lerp(partialTicks, be.prevDialState, be.dialState);
		float stress = Mth.lerp(partialTicks, be.prevStressDialState, be.stressDialState);

		for (Direction facing : Iterate.directions) {
			if (!gauge.shouldRenderHeadOnFace(be.getLevel(), be.getBlockPos(), state, facing))
				continue;

			VertexConsumer vb = buffer.getBuffer(RenderType.solid());
			sweep(speedDial, facing, speed).light(light)
				.renderInto(ms, vb);
			sweep(stressDial, facing, stress).light(light)
				.renderInto(ms, vb);
			towards(head, facing).light(light)
				.renderInto(ms, vb);
		}
	}

	/** Hinge the needle at the pivot, swing it a quarter turn at full scale, put it back. */
	private static SuperByteBuffer sweep(SuperByteBuffer dial, Direction facing, float progress) {
		return towards(dial, facing).translate(0, DIAL_PIVOT, DIAL_PIVOT)
			.rotate((float) (Math.PI / 2 * -progress), Direction.EAST)
			.translate(0, -DIAL_PIVOT, -DIAL_PIVOT);
	}

	/** The models are drawn facing east; this yaws them onto the face being rendered. */
	private static SuperByteBuffer towards(SuperByteBuffer buffer, Direction target) {
		return buffer.rotateCentered((float) ((-target.toYRot() - 90) / 180 * Math.PI), Direction.UP);
	}
}
