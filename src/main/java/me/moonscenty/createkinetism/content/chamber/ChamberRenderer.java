package me.moonscenty.createkinetism.content.chamber;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders the spinning cog inside a chamber. We reuse Create's own millstone cog partial so the
 * animation reads as "a Create machine" without shipping a second copy of the model.
 *
 * <p>Chambers whose model is a solid casing (the two-input ones, which mark their secondary input
 * face instead) have nowhere to show a cog, so they skip this entirely.</p>
 */
public class ChamberRenderer extends KineticBlockEntityRenderer<ChamberBlockEntity> {

	public ChamberRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(ChamberBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		if (be.getBlockState()
			.getBlock() instanceof ChamberBlock chamber && !chamber.rendersCog())
			return;
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
	}

	@Override
	protected SuperByteBuffer getRotatedModel(ChamberBlockEntity be, BlockState state) {
		return CachedBuffers.partial(AllPartialModels.MILLSTONE_COG, state);
	}
}
