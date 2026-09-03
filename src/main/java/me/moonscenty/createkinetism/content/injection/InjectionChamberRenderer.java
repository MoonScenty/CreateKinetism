package me.moonscenty.createkinetism.content.injection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The chamber's cog, its vacuum housing and the gas held inside it.
 *
 * <p>Unlike the Mechanical Infuser this has no telescoping nozzle. Instead the whole {@code head}
 * plunges: it rests 7px below its baked position, and drops further and rises back across the length
 * of a process - see {@link #getPlunge}. {@code arrows} is drawn statically at its own baked
 * position; the cog spins with the shaft as usual.
 *
 * <p>{@code arrows} is <em>not</em> drawn with {@code partialFacing}: its four blades already span
 * the block's full width and depth, and {@code partialFacing} rotates a partial as one rigid unit
 * assuming a small, direction-aware shape - on something already this wide it swaps which axis is
 * seventeen units long and which is one, so it ends up sticking far outside the block.</p>
 */
public class InjectionChamberRenderer extends KineticBlockEntityRenderer<InjectionChamberBlockEntity> {

	/** How far below its baked position the head rests when nothing is being processed. */
	private static final float REST_OFFSET = 7 / 16f;
	/** How much further it plunges at the peak of a process, on top of the rest offset. */
	private static final float PLUNGE_AMPLITUDE = 10 / 16f;

	public InjectionChamberRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRenderOffScreen(InjectionChamberBlockEntity be) {
		return true;
	}

	@Override
	protected void renderSafe(InjectionChamberBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {

		// No Flywheel bail-out, for the same reason VatRenderer has none: without a Visual counterpart
		// returning early would delete the cog and the housing outright.
		BlockState blockState = be.getBlockState();
		VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());

		standardKineticRotationTransform(
			CachedBuffers.partial(CKPartialModels.INJECTION_CHAMBER_COG, blockState), be, light)
				.renderInto(ms, vb);

		CachedBuffers.partial(CKPartialModels.INJECTION_CHAMBER_HEAD, blockState)
			.translate(0, -(REST_OFFSET + getPlunge(be, partialTicks)), 0)
			.light(light)
			.renderInto(ms, vb);

		// Baked pointing up; flipping 180 around the block-centered X axis swaps Y and Z, which turns
		// the chevron downward without mirroring it left-right (X is untouched, and the shape is
		// symmetric front-to-back anyway so the Z flip is invisible).
		CachedBuffers.partial(CKPartialModels.INJECTION_CHAMBER_ARROWS, blockState)
			.rotateXCenteredDegrees(180)
			.light(light)
			.renderInto(ms, vb);

		renderFluid(be, partialTicks, ms, buffer, light);
	}

	/**
	 * How far past the rest position the head has plunged, as a smooth hump from 0 up to
	 * {@link #PLUNGE_AMPLITUDE} and back to 0 across the whole of a process - not Create's fixed
	 * 40-tick vat curve, because a recipe's processing time varies and the travel should still land
	 * exactly on 0 the moment the item is released.
	 */
	private float getPlunge(InjectionChamberBlockEntity be, float partialTicks) {
		if (be.processingTicks < 0 || be.totalProcessingTicks <= 0)
			return 0;
		float ticksLeft = Mth.clamp(be.processingTicks - partialTicks, 0, be.totalProcessingTicks);
		float progress = 1 - ticksLeft / be.totalProcessingTicks;
		return Mth.sin(progress * (float) Math.PI) * PLUNGE_AMPLITUDE;
	}

	/** The gas resting in the tank, the same technique the infuser uses for its own nozzle. */
	private void renderFluid(InjectionChamberBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light) {

		SmartFluidTankBehaviour tank = be.tank;
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
