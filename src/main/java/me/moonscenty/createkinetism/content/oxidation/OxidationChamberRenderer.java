package me.moonscenty.createkinetism.content.oxidation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import me.moonscenty.createkinetism.content.vat.VatBlockEntity;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The chamber's cog and its plunging head.
 *
 * <p>The Injection Chamber's renderer with the tank taken out: same cog on Create's own timing, same
 * head riding {@code getRenderedHeadOffset}, same chevrons on the body. There is no fluid pass
 * because there is no fluid to draw - what this machine makes goes into the basin, which draws its
 * own contents.</p>
 */
public class OxidationChamberRenderer extends KineticBlockEntityRenderer<VatBlockEntity> {

	public OxidationChamberRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRenderOffScreen(VatBlockEntity be) {
		return true;
	}

	@Override
	protected void renderSafe(VatBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {

		// No Flywheel bail-out, for the same reason VatRenderer has none: without a Visual counterpart
		// returning early would delete the cog and the head outright.
		BlockState blockState = be.getBlockState();
		VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());

		standardKineticRotationTransform(
			CachedBuffers.partial(CKPartialModels.OXIDATION_CHAMBER_COG, blockState), be, light)
				.renderInto(ms, vb);

		float headOffset = be.getRenderedHeadOffset(partialTicks);

		CachedBuffers.partial(CKPartialModels.OXIDATION_CHAMBER_HEAD, blockState)
			.translate(0, -headOffset, 0)
			.light(light)
			.renderInto(ms, vb);

		// On the body, not the plunging head - it stays put while the head moves. Flipped 180 around X
		// so the baked-upward chevron points down, without mirroring it left-right.
		CachedBuffers.partial(CKPartialModels.OXIDATION_CHAMBER_ARROWS, blockState)
			.rotateXCenteredDegrees(180)
			.light(light)
			.renderInto(ms, vb);
	}
}
