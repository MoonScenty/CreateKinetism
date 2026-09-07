package me.moonscenty.createkinetism.content.curio.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import me.moonscenty.createkinetism.CreateKinetism;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Draws the wings on the wearer's back.
 *
 * <p>Vanilla's own {@link ElytraModel} and the layer it belongs to, with our texture - so the wings
 * open, fold and flap exactly as an elytra's do. Rebuilding that animation would only be a chance to
 * get it subtly wrong.</p>
 */
public class KineticElytraRenderer implements ICurioRenderer {

	private static final ResourceLocation TEXTURE = CreateKinetism.asResource("textures/entity/kinetic_elytra.png");

	private ElytraModel<LivingEntity> model;

	@Override
	public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext,
		PoseStack matrixStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource renderTypeBuffer,
		int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
		float headPitch) {

		LivingEntity wearer = slotContext.entity();
		if (model == null)
			model = new ElytraModel<>(Minecraft.getInstance()
				.getEntityModels()
				.bakeLayer(ModelLayers.ELYTRA));

		// Vanilla's ElytraLayer, step for step. The quarter-pixel push is what keeps the wings off the
		// body, and copyPropertiesTo is what makes them shrink on a baby and lean when riding.
		matrixStack.pushPose();
		matrixStack.translate(0.0F, 0.0F, 0.125F);
		@SuppressWarnings("unchecked")
		EntityModel<T> wings = (EntityModel<T>) model;
		renderLayerParent.getModel()
			.copyPropertiesTo(wings);
		model.setupAnim(wearer, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

		VertexConsumer consumer = ItemRenderer.getArmorFoilBuffer(renderTypeBuffer,
			RenderType.armorCutoutNoCull(TEXTURE), stack.hasFoil());
		model.renderToBuffer(matrixStack, consumer, light, OverlayTexture.NO_OVERLAY);
		matrixStack.popPose();
	}
}