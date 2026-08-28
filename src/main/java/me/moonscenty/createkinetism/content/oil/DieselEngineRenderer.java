package me.moonscenty.createkinetism.content.oil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
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
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Draws the piston, its cam linkage and the collar gripping the powered shaft. The animation is
 * driven off the shaft's own rotation angle rather than a free-running clock, so the piston stroke
 * stays locked to the shaft it is turning - slow the network and the engine visibly labours.</p>
 *
 * <p>The stroke is solved as a crank-slider: the piston height comes from the crank angle and a
 * fixed connecting-rod length, and the linkage angle falls out of that.</p>
 */
public class DieselEngineRenderer extends SafeBlockEntityRenderer<DieselEngineBlockEntity> {

	private static final float CRANK_RADIUS = 6 / 16f;
	private static final float ROD_LENGTH = 14 / 16f;

	public DieselEngineRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public int getViewDistance() {
		return 128;
	}

	@Override
	protected void renderSafe(DieselEngineBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource bufferSource, int light, int overlay) {

		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		Float angle = be.getTargetAngle();
		if (angle == null)
			return;

		BlockState blockState = be.getBlockState();
		Direction facing = SteamEngineBlock.getFacing(blockState);
		Axis facingAxis = facing.getAxis();

		Axis axis = Axis.Y;
		PoweredShaftBlockEntity shaft = be.getShaft();
		if (shaft != null)
			axis = KineticBlockEntityRenderer.getRotationAxisOf(shaft);

		boolean roll90 = facingAxis.isHorizontal() && axis == Axis.Y || facingAxis.isVertical() && axis == Axis.Z;

		// Crank-slider: piston height, then the rod angle that reaches it.
		float piston = CRANK_RADIUS * Mth.sin(angle)
			- Mth.sqrt(Mth.square(ROD_LENGTH) - Mth.square(CRANK_RADIUS) * Mth.square(Mth.cos(angle)));
		float distance = Mth.sqrt(Mth.square(piston - CRANK_RADIUS * Mth.sin(angle)));
		float rodAngle = (float) Math.acos(distance / ROD_LENGTH) * (Mth.cos(angle) >= 0 ? 1f : -1f);

		VertexConsumer vb = bufferSource.getBuffer(RenderType.solid());

		transformed(CKPartialModels.DIESEL_ENGINE_PISTON, blockState, facing, roll90)
			.translate(0, piston + 20 / 16f, 0)
			.light(light)
			.renderInto(ms, vb);

		transformed(CKPartialModels.DIESEL_ENGINE_LINKAGE, blockState, facing, roll90)
			.center()
			.translate(0, 1, 0)
			.uncenter()
			.translate(0, piston + 20 / 16f, 0)
			.translate(0, 4 / 16f, 8 / 16f)
			.rotateX(rodAngle)
			.translate(0, -4 / 16f, -8 / 16f)
			.light(light)
			.renderInto(ms, vb);

		transformed(CKPartialModels.DIESEL_ENGINE_CONNECTOR, blockState, facing, roll90)
			.translate(0, 2, 0)
			.center()
			.rotateX(-(angle + Mth.HALF_PI))
			.uncenter()
			.light(light)
			.renderInto(ms, vb);
	}

	/** Orients a part for whichever face the engine is bolted to. */
	private static SuperByteBuffer transformed(PartialModel model, BlockState blockState, Direction facing,
		boolean roll90) {
		return CachedBuffers.partial(model, blockState)
			.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(facing))
			.rotateXDegrees(AngleHelper.verticalAngle(facing) + 90)
			.rotateYDegrees(roll90 ? -90 : 0)
			.uncenter();
	}
}
