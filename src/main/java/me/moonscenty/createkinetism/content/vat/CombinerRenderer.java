package me.moonscenty.createkinetism.content.vat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Combiner draws its own whisk and pole, and shows what it is holding.
 *
 * <p>It does not extend {@link VatRenderer}: the other vats share Create's mixer partials, and this
 * one has copies of its own so its shape can be changed without moving the other nine. The cogwheel
 * is still Create's - that part is shared by half of Create's machines and is not mixer-specific.</p>
 *
 * <p>The held item rides the same offset as the head. The infusion item lives in an inventory with
 * nothing in the model to suggest it, so drawing it under the whisk is what makes "this is being
 * pressed into the basin" read at a glance.</p>
 */
public class CombinerRenderer extends KineticBlockEntityRenderer<VatBlockEntity> {

	public CombinerRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRenderOffScreen(VatBlockEntity be) {
		return true;
	}

	@Override
	protected void renderSafe(VatBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light,
		int overlay) {

		// No Flywheel bail-out, for the same reason VatRenderer has none: there is no Visual
		// counterpart, so returning early would simply delete every moving part.
		BlockState blockState = be.getBlockState();
		VertexConsumer vb = buffer.getBuffer(RenderType.solid());

		SuperByteBuffer cog = CachedBuffers.partial(AllPartialModels.SHAFTLESS_COGWHEEL, blockState);
		standardKineticRotationTransform(cog, be, light).renderInto(ms, vb);

		// A pixel shy of where a vat's whisk would stop. Only the Combiner is shifted - the other nine
		// keep VatBlockEntity's offset untouched.
		float headOffset = be.getRenderedHeadOffset(partialTicks) - 1 / 16f;

		CachedBuffers.partial(CKPartialModels.COMBINER_POLE, blockState)
			.translate(0, -headOffset, 0)
			.light(light)
			.renderInto(ms, vb);

		// The whisk travels but does not spin.
		CachedBuffers.partial(CKPartialModels.COMBINER_HEAD, blockState)
			.translate(0, -headOffset, 0)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

		renderHeldInfusion(be, partialTicks, ms, buffer, light, overlay, headOffset);
	}

	private static void renderHeldInfusion(VatBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay, float headOffset) {

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
		ms.translate(0.5, 0.5f - headOffset - 3 / 16f, 0.5);

		// Laid face down, towards the basin. A flat item renders upright - its face pointing south - so
		// a quarter turn about X tips that normal to point straight down.
		ms.mulPose(Axis.XP.rotationDegrees(90));

		// Held still. A spin would read as the machine working, and the whisk above it does not turn.
		float scale = blockItem ? 1f : .75f;
		ms.scale(scale, scale, scale);

		// FIXED rather than GROUND: the ground transform carries a translation of its own, and rotating
		// before the renderer applies it swings that offset sideways and pushes the item off centre.
		// FIXED has no translation, so what is drawn sits exactly where this pose put it.
		itemRenderer.render(held, ItemDisplayContext.FIXED, false, ms, buffer, light, overlay, model);
		ms.popPose();
	}
}
