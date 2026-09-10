package me.moonscenty.createkinetism.content.boiler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * Draws the shaft running through a Sodium Burner, end to end - the housing itself is the static
 * baked block model. Lifted from the Mechanical Electrolyzer branch of {@code VatRenderer}, minus the
 * pole and head this block does not have.
 */
public class SodiumBurnerRenderer extends KineticBlockEntityRenderer<SodiumBurnerBlockEntity> {

	public SodiumBurnerRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRenderOffScreen(SodiumBurnerBlockEntity be) {
		return true;
	}

	@Override
	protected void renderSafe(SodiumBurnerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		VertexConsumer vb = buffer.getBuffer(RenderType.solid());
		SuperByteBuffer superBuffer = getRotatedModel(be, shaft(getRotationAxisOf(be)));
		standardKineticRotationTransform(superBuffer, be, light).renderInto(ms, vb);
	}
}
