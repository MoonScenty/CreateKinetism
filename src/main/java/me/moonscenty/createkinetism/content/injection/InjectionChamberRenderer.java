package me.moonscenty.createkinetism.content.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;

import me.moonscenty.createkinetism.content.vat.VatBlockEntity;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The chamber's cog, its plunging head and the gas held in its own tank.
 *
 * <p>Built like the plain vats and the Combiner: the cog spins with the shaft on Create's own timing,
 * and the head rides {@code getRenderedHeadOffset}, which {@link InjectionChamberBlockEntity}
 * overrides to rescale {@code VatBlockEntity}'s own down-hold-up curve from the mixer's ~16px swing
 * to a 10px plunge. {@code arrows} stays with the body rather than the head - it marks where the gas
 * goes in, not the plunger - and {@code block} is left to the ordinary static model.</p>
 */
public class InjectionChamberRenderer extends KineticBlockEntityRenderer<VatBlockEntity> {

	public InjectionChamberRenderer(BlockEntityRendererProvider.Context context) {
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
			CachedBuffers.partial(CKPartialModels.INJECTION_CHAMBER_COG, blockState), be, light)
				.renderInto(ms, vb);

		float headOffset = be.getRenderedHeadOffset(partialTicks);

		CachedBuffers.partial(CKPartialModels.INJECTION_CHAMBER_HEAD, blockState)
			.translate(0, -headOffset, 0)
			.light(light)
			.renderInto(ms, vb);

		// On the body, not the plunging head - it stays put while the head moves. Flipped 180 around X
		// so the baked-upward chevron points down, without mirroring it left-right.
		CachedBuffers.partial(CKPartialModels.INJECTION_CHAMBER_ARROWS, blockState)
			.rotateXCenteredDegrees(180)
			.light(light)
			.renderInto(ms, vb);

		renderFluid(be, partialTicks, ms, buffer, light);
	}

	/** The gas resting in the chamber's own tank - static, since the tank is in the housing, not on
	 *  the plunger. */
	private void renderFluid(VatBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light) {

		if (!(be instanceof InjectionChamberBlockEntity chamber))
			return;

		SmartFluidTankBehaviour tank = chamber.tank;
		TankSegment primaryTank = tank == null ? null : tank.getPrimaryTank();
		FluidStack fluidStack = primaryTank == null ? FluidStack.EMPTY : primaryTank.getRenderedFluid();
		float level = primaryTank == null ? 0
			: primaryTank.getFluidLevel()
				.getValue(partialTicks);

		if (fluidStack.isEmpty() || level == 0)
			return;

		boolean lighterThanAir = fluidStack.getFluid()
			.getFluidType()
			.isLighterThanAir();

		level = Math.max(level, 0.175f);
		float min = 2.5f / 16f;
		float max = min + (11 / 16f);
		float yOffset = (11 / 16f) * level;

		ms.pushPose();
		ms.translate(0, lighterThanAir ? max - min : yOffset, 0);
		NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluidStack, min, min - yOffset, min, max, min, max,
			buffer, ms, light, false, true);
		ms.popPose();
	}
}
