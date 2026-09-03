package me.moonscenty.createkinetism.content.accumulator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the tool lying on top of the accumulator while it winds.
 *
 * <p>The accumulator has no GUI, so this is the whole readout: if a Disassembler is sitting there,
 * it is being wound. It is laid flat rather than stood upright so it reads as put down rather than
 * held.</p>
 */
public class KineticAccumulatorRenderer extends SafeBlockEntityRenderer<KineticAccumulatorBlockEntity> {

	public KineticAccumulatorRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(KineticAccumulatorBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {

		ItemStack tool = be.getHeldTool();
		if (tool.isEmpty())
			return;

		ms.pushPose();
		ms.translate(0.5f, 1.02f, 0.5f);
		ms.mulPose(Axis.XP.rotationDegrees(90));
		ms.scale(0.6f, 0.6f, 0.6f);
		Minecraft.getInstance()
			.getItemRenderer()
			.renderStatic(tool, ItemDisplayContext.FIXED, light, overlay, ms, buffer, be.getLevel(), 0);
		ms.popPose();
	}
}
