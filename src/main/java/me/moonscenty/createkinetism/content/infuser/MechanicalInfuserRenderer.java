package me.moonscenty.createkinetism.content.infuser;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The infuser's cogwheel, nozzle and the fluid inside it.
 *
 * <p>Create's spout has no cogwheel because it costs nothing to run. This one does, so it needs
 * something that visibly turns - the same shaftless cog the vats use, on the same axis the shaft
 * enters by.</p>
 */
public class MechanicalInfuserRenderer extends KineticBlockEntityRenderer<MechanicalInfuserBlockEntity> {

	private static final PartialModel[] SEGMENTS = { CKPartialModels.MECHANICAL_INFUSER_TOP,
		CKPartialModels.MECHANICAL_INFUSER_MIDDLE, CKPartialModels.MECHANICAL_INFUSER_BOTTOM };

	public MechanicalInfuserRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	/** The nozzle hangs below its own block. */
	@Override
	public boolean shouldRenderOffScreen(MechanicalInfuserBlockEntity be) {
		return true;
	}

	@Override
	protected void renderSafe(MechanicalInfuserBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {

		// No Flywheel bail-out, for the same reason VatRenderer has none: without a Visual counterpart
		// returning early would delete the cog and the nozzle outright.
		BlockState blockState = be.getBlockState();
		VertexConsumer vb = buffer.getBuffer(RenderType.solid());

		SuperByteBuffer cog = CachedBuffers.partial(AllPartialModels.SHAFTLESS_COGWHEEL, blockState);
		standardKineticRotationTransform(cog, be, light).renderInto(ms, vb);

		SmartFluidTankBehaviour tank = be.tank;
		TankSegment primaryTank = tank == null ? null : tank.getPrimaryTank();
		FluidStack fluidStack = primaryTank == null ? FluidStack.EMPTY : primaryTank.getRenderedFluid();
		float level = primaryTank == null ? 0
			: primaryTank.getFluidLevel()
				.getValue(partialTicks);

		if (!fluidStack.isEmpty() && level != 0) {
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

		// The nozzle reaches down while the infusion is being applied, and a column of the fluid falls
		// from it. Both are Create's spout timings: the stream swells and thins over the last ten
		// ticks, and the segments pull apart with it.
		int processingTicks = be.processingTicks;
		float processingPT = processingTicks - partialTicks;
		float processingProgress = Mth.clamp(1 - (processingPT - 5) / 10, 0, 1);
		float radius = 0;

		if (!fluidStack.isEmpty() && processingTicks != -1) {
			radius = (float) (Math.pow(2 * processingProgress - 1, 2) - 1);
			AABB bb = new AABB(0.5, 0.0, 0.5, 0.5, -1.2, 0.5).inflate(radius / 32f);
			NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluidStack, (float) bb.minX,
				(float) bb.minY, (float) bb.minZ, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ,
				buffer, ms, light, true, true);
		}

		float squeeze = radius;
		if (processingPT < 0)
			squeeze = 0;
		else if (processingPT < 2)
			squeeze = Mth.lerp(processingPT / 2f, 0, -1);
		else if (processingPT < 10)
			squeeze = -1;

		ms.pushPose();
		for (PartialModel segment : SEGMENTS) {
			CachedBuffers.partial(segment, blockState)
				.light(light)
				.renderInto(ms, vb);
			ms.translate(0, -3 * squeeze / 32f, 0);
		}
		ms.popPose();
	}
}
