package me.moonscenty.createkinetism.content.chemistry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;

import me.moonscenty.createkinetism.content.vat.VatBlockEntity;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The chemistry infuser's cogwheel, its own held fluid, and a nozzle in place of the mixer's whisk.
 *
 * <p>Every other plain vat shares {@code VatRenderer}, which draws Create's Mechanical Mixer pole and
 * head because a vat <em>is</em> a mixer stirring its basin. This one is not stirring anything - it
 * is pouring its own tank into the basin, continuously, with no batch cycle to animate against - so
 * the nozzle is drawn still rather than telescoping, and there is no whisk at all.</p>
 */
public class MechanicalChemistryInfuserRenderer extends KineticBlockEntityRenderer<VatBlockEntity> {

	public MechanicalChemistryInfuserRenderer(BlockEntityRendererProvider.Context context) {
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
		// returning early would delete the cog and the nozzle outright.
		BlockState blockState = be.getBlockState();
		VertexConsumer vb = buffer.getBuffer(RenderType.solid());

		SuperByteBuffer cog = CachedBuffers.partial(AllPartialModels.SHAFTLESS_COGWHEEL, blockState);
		standardKineticRotationTransform(cog, be, light).renderInto(ms, vb);

		CachedBuffers.partial(CKPartialModels.MECHANICAL_CHEMISTRY_INFUSER_TOP, blockState)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
		CachedBuffers.partial(CKPartialModels.MECHANICAL_CHEMISTRY_INFUSER_MIDDLE, blockState)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
		CachedBuffers.partial(CKPartialModels.MECHANICAL_CHEMISTRY_INFUSER_BOTTOM, blockState)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

		if (!(be instanceof MechanicalChemistryInfuserBlockEntity chemistry))
			return;

		SmartFluidTankBehaviour tank = chemistry.tank;
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
