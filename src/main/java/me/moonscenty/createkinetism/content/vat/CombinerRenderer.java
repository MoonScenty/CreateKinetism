package me.moonscenty.createkinetism.content.vat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * A vat that also shows what it is holding.
 *
 * <p>The Combiner's infusion item is invisible otherwise - it lives in an inventory on the block,
 * with nothing in the mixer model to suggest it. Drawing it under the head, riding the same offset,
 * is what makes "the machine presses this into the basin" read at a glance, the way Create's Deployer
 * shows the item on its arm.</p>
 */
public class CombinerRenderer extends VatRenderer {

	public CombinerRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(VatBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light,
		int overlay) {
		super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

		if (!(be instanceof CombinerBlockEntity combiner))
			return;
		ItemStack held = combiner.getHeldInfusion();
		if (held.isEmpty())
			return;

		ItemRenderer itemRenderer = Minecraft.getInstance()
			.getItemRenderer();
		BakedModel model = itemRenderer.getModel(held, be.getLevel(), null, 0);
		boolean blockItem = held.getItem() instanceof BlockItem && model.isGui3d();

		ms.pushPose();
		// Just below the head, and moving with it: the item is what the head is pushing down.
		ms.translate(0.5, 0.5f - be.getRenderedHeadOffset(partialTicks) - 3 / 16f, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(AnimationTickHolder.getRenderTime(be.getLevel()) * 2));

		float scale = blockItem ? .5f : .375f;
		ms.scale(scale, scale, scale);

		itemRenderer.render(held, ItemDisplayContext.GROUND, false, ms, buffer, light, overlay, model);
		ms.popPose();
	}
}
