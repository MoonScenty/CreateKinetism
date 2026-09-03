package me.moonscenty.createkinetism.content.vat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws the cogwheel, pole and stirring head of a vat. This is Create's
 * {@code MechanicalMixerRenderer} with the block entity type swapped out; reusing Create's partial
 * models is what makes the machines animate identically to a Mechanical Mixer.
 */
public class VatRenderer extends KineticBlockEntityRenderer<VatBlockEntity> {

	public VatRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRenderOffScreen(VatBlockEntity be) {
		return true;
	}

	@Override
	protected void renderSafe(VatBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light,
		int overlay) {

		// No Flywheel bail-out here on purpose. That guard is only correct when a Visual counterpart
		// exists to draw these parts instead; without one it just deletes the cog and the stirring
		// pole on any default install, which is exactly what it did before this comment was written.
		BlockState blockState = be.getBlockState();

		VertexConsumer vb = buffer.getBuffer(RenderType.solid());

		SuperByteBuffer superBuffer = CachedBuffers.partial(AllPartialModels.SHAFTLESS_COGWHEEL, blockState);
		standardKineticRotationTransform(superBuffer, be, light).renderInto(ms, vb);

		float renderedHeadOffset = be.getRenderedHeadOffset(partialTicks);

		SuperByteBuffer poleRender = CachedBuffers.partial(AllPartialModels.MECHANICAL_MIXER_POLE, blockState);
		poleRender.translate(0, -renderedHeadOffset, 0)
			.light(light)
			.renderInto(ms, vb);

		// The whisk only travels; it does not spin. Create's mixer turns its head, but these machines
		// are not stirring anything - the cogwheel above is what shows they are running.
		VertexConsumer vbCutout = buffer.getBuffer(RenderType.cutoutMipped());
		SuperByteBuffer headRender = CachedBuffers.partial(AllPartialModels.MECHANICAL_MIXER_HEAD, blockState);
		headRender.translate(0, -renderedHeadOffset, 0)
			.light(light)
			.renderInto(ms, vbCutout);
	}
}
