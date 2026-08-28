package me.moonscenty.createkinetism.content.oil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Draws the walking beam. This is the one piece of real kinematics in the mod: the crank turns a
 * pin on a circle, the pitman arm is a fixed-length rod hanging off that pin, and the beam is a
 * fixed-length arm pivoting on the A-frame. Where those two circles cross is where the beam and the
 * pitman meet, so the whole linkage falls out of one circle-circle intersection and the beam nods
 * with the slightly uneven rhythm a real pumpjack has, rather than a sine wave.</p>
 */
public class PumpjackArmRenderer extends SafeBlockEntityRenderer<PumpjackArmBlockEntity> {

	public PumpjackArmRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public int getViewDistance() {
		return 128;
	}

	@Override
	protected void renderSafe(PumpjackArmBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource bufferSource, int light, int overlay) {

		VertexConsumer solid = bufferSource.getBuffer(RenderType.solid());
		BlockState blockState = be.getBlockState();
		Direction facing = PumpjackArmBlock.getFacing(blockState);
		if (facing == null)
			return;

		SuperByteBuffer head = CachedBuffers.partialFacing(CKPartialModels.PUMPJACK_HEAD, blockState, facing);
		SuperByteBuffer body = CachedBuffers.partialFacing(CKPartialModels.PUMPJACK_ARM, blockState, facing);
		SuperByteBuffer tail = CachedBuffers.partialFacing(CKPartialModels.PUMPJACK_CONNECTOR, blockState, facing);
		SuperByteBuffer pitman = CachedBuffers.partialFacing(CKPartialModels.PUMPJACK_PITMAN, blockState, facing);
		SuperByteBuffer smoothRod =
			CachedBuffers.partialFacing(CKPartialModels.PUMPJACK_SMOOTH_ROD, blockState, facing);

		ms.pushPose();

		float crankAngle = 0;
		if (be.crank != null) {
			float speed = be.crank.visualSpeed.getValue(partialTicks) * 6 / 20f;
			crankAngle = (be.crank.angle + speed * partialTicks) * Mth.DEG_TO_RAD;
		}

		TransformStack.of(ms)
			.translate(Vec3i.ZERO.relative(facing, 2)
				.below(2));

		Vec2 pitmanPivot = new Vec2(Mth.sin(-crankAngle) * 8f / 16f, Mth.cos(-crankAngle) * 8 / 16f + 11 / 16f);
		Vec2 beamPivot = new Vec2(-32 / 16f, 40 / 16f);
		float beamRadius = 32 / 16f;
		float pitmanRadius = 28 / 16f;

		float[] intersections = new float[4];
		findCircleIntersection(pitmanPivot, pitmanRadius, beamPivot, beamRadius, intersections);

		float pitmanAngle =
			(float) Mth.atan2(pitmanPivot.y - intersections[1], pitmanPivot.x - intersections[0]) + Mth.HALF_PI;
		float beamAngle = (float) Mth.atan2(intersections[1] - beamPivot.y, intersections[0] - beamPivot.x);

		// The linkage is solved in 2D, so project it back onto whichever horizontal axis we face.
		float xCoef = 0f;
		float zCoef = 0f;
		switch (facing) {
			case SOUTH -> zCoef = 1f;
			case NORTH -> zCoef = -1f;
			case EAST -> xCoef = 1f;
			case WEST -> xCoef = -1f;
			default -> {
			}
		}

		pitman.light(light)
			.translate(pitmanPivot.x * xCoef, pitmanPivot.y, pitmanPivot.x * zCoef)
			.translate(0, -8 / 16f, 0)
			.rotateCentered(pitmanAngle, facing.getClockWise())
			.translate(0, 8 / 16f, 0)
			.renderInto(ms, solid);

		TransformStack.of(ms)
			.translate(intersections[0] * xCoef, intersections[1], intersections[0] * zCoef)
			.translate(0, -8 / 16f, 0)
			.rotateCentered(beamAngle, facing.getClockWise());

		tail.light(light)
			.renderInto(ms, solid);
		body.light(light)
			.translate(Vec3i.ZERO.relative(facing, -2))
			.renderInto(ms, solid);
		head.light(light)
			.translate(Vec3i.ZERO.relative(facing, -4))
			.renderInto(ms, solid);

		ms.popPose();

		// The polished rod only exists if there is actually a wellhead to run down into.
		if (be.well == null)
			return;

		float headHeight = (float) Math.sin(-beamAngle) * 2f;
		TransformStack.of(ms)
			.translate(Vec3i.ZERO.relative(facing, -2)
				.below());
		smoothRod.light(light)
			.translate(0, headHeight - 1, 0)
			.scale(1f, 2f, 1f)
			.renderInto(ms, solid);
	}

	/**
	 * Where two circles cross. Writes both intersection points into {@code result} as
	 * {@code [x1, y1, x2, y2]}.
	 */
	public static boolean findCircleIntersection(Vec2 c1, float r1, Vec2 c2, float r2, float[] result) {
		float dx = c2.x - c1.x;
		float dy = c2.y - c1.y;
		float d = (float) Math.sqrt(dx * dx + dy * dy);

		// Too far apart, or one circle swallowed by the other.
		if (d > r1 + r2 || d < Math.abs(r1 - r2))
			return false;
		// Coincident circles: infinitely many answers, none of them useful here.
		if (d == 0 && r1 == r2)
			return false;

		float a = (r1 * r1 - r2 * r2 + d * d) / (2 * d);
		float h = (float) Math.sqrt(r1 * r1 - a * a);
		float xm = c1.x + a * dx / d;
		float ym = c1.y + a * dy / d;

		result[0] = xm + h * dy / d;
		result[1] = ym - h * dx / d;
		result[2] = xm - h * dy / d;
		result[3] = ym + h * dx / d;
		return true;
	}
}
