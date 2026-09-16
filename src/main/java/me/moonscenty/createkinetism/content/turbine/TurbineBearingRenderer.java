package me.moonscenty.createkinetism.content.turbine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** The half shaft out of the bearing's underside, turning with the network. */
public class TurbineBearingRenderer extends KineticBlockEntityRenderer<TurbineBearingBlockEntity> {

	public TurbineBearingRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(TurbineBearingBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		standardKineticRotationTransform(
			CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), Direction.DOWN), be, light)
			.renderInto(ms, buffer.getBuffer(RenderType.solid()));
	}
}
