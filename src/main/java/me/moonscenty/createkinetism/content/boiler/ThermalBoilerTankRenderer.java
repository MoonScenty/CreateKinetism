package me.moonscenty.createkinetism.content.boiler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The visible proof that a stack folded into a boiler. Reuses the exact gauge-and-needle draw from
 * {@code SteelTankRenderer#renderAsDistiller} - the same round dial the Distillation Controller
 * shows - rather than inventing a new visual: a single gauge on the feed floor, reading how full the
 * feed tank is. It does not appear unless {@link ThermalBoilerTankBlockEntity#boilerMode} is set.
 */
public class ThermalBoilerTankRenderer extends SafeBlockEntityRenderer<ThermalBoilerTankBlockEntity> {

	private static final float DIAL_PIVOT_Y = 6f / 16;
	private static final float DIAL_PIVOT_Z = 8f / 16;

	public ThermalBoilerTankRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(ThermalBoilerTankBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
		if (!be.isController() || !be.boilerMode)
			return;

		VertexConsumer vb = buffer.getBuffer(RenderType.cutout());
		BlockState state = be.getBlockState();
		int width = be.getWidth();

		renderGauge(state, width, be.getBoilerInputFillState(), ms, vb, light);
	}

	/** One dial per exposed side, needle sweeping from empty (up) to full, exactly like the column's. */
	private void renderGauge(BlockState state, int width, float fillState, PoseStack ms, VertexConsumer vb,
		int light) {
		float progress = Mth.clamp(fillState, 0, 1);

		ms.pushPose();
		ms.translate(width / 2f, 0.5, width / 2f);

		for (Direction d : Iterate.horizontalDirections) {
			ms.pushPose();
			float yRot = -d.toYRot() - 90;

			CachedBuffers.partial(CKPartialModels.DISTILLATION_GAUGE, state)
				.rotateYDegrees(yRot)
				.uncenter()
				.translate(width / 2f - 6 / 16f, 0, 0)
				.light(light)
				.renderInto(ms, vb);

			CachedBuffers.partial(CKPartialModels.DISTILLATION_GAUGE_DIAL, state)
				.rotateYDegrees(yRot)
				.uncenter()
				.translate(width / 2f - 6 / 16f, 0, 0)
				.translate(0, DIAL_PIVOT_Y, DIAL_PIVOT_Z)
				.rotateXDegrees(-145 * progress + 90)
				.translate(0, -DIAL_PIVOT_Y, -DIAL_PIVOT_Z)
				.light(light)
				.renderInto(ms, vb);
			ms.popPose();
		}

		ms.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen(ThermalBoilerTankBlockEntity be) {
		return be.isController();
	}
}
