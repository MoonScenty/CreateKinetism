package me.moonscenty.createkinetism.content.infuser;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The infuser's cogwheel and nozzle.
 *
 * <p>Create's spout has no cogwheel because it costs nothing to run. This one does, so it needs
 * something that visibly turns - the same shaftless cog the vats use, on the same axis the shaft
 * enters by.</p>
 */
public class MechanicalMetallurgicInfuserRenderer extends KineticBlockEntityRenderer<MechanicalMetallurgicInfuserBlockEntity> {

	private static final PartialModel[] SEGMENTS = { CKPartialModels.MECHANICAL_METALLURGIC_INFUSER_TOP,
		CKPartialModels.MECHANICAL_METALLURGIC_INFUSER_MIDDLE, CKPartialModels.MECHANICAL_METALLURGIC_INFUSER_BOTTOM };

	public MechanicalMetallurgicInfuserRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	/** The nozzle hangs below its own block. */
	@Override
	public boolean shouldRenderOffScreen(MechanicalMetallurgicInfuserBlockEntity be) {
		return true;
	}

	@Override
	protected void renderSafe(MechanicalMetallurgicInfuserBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {

		// No Flywheel bail-out, for the same reason VatRenderer has none: without a Visual counterpart
		// returning early would delete the cog and the nozzle outright.
		BlockState blockState = be.getBlockState();
		VertexConsumer vb = buffer.getBuffer(RenderType.solid());

		SuperByteBuffer cog = CachedBuffers.partial(AllPartialModels.SHAFTLESS_COGWHEEL, blockState);
		standardKineticRotationTransform(cog, be, light).renderInto(ms, vb);

		// Nothing liquid is drawn any more. What the machine holds is a Mekanism chemical, and
		// Create's fluid renderer takes a FluidStack - there is no fluid form of an infuse type to hand
		// it. The nozzle still reaches and the tint still shows in the particles; the level inside the
		// housing and the falling stream are gone with the fluid they were made of.
		boolean charged = !be.getStoredChemical()
			.isEmpty();

		// The nozzle reaches down while the infusion is being applied. Create's spout timings: the
		// segments pull apart over the last ten ticks.
		int processingTicks = be.processingTicks;
		float processingPT = processingTicks - partialTicks;
		float processingProgress = Mth.clamp(1 - (processingPT - 5) / 10, 0, 1);
		float radius = charged && processingTicks != -1
			? (float) (Math.pow(2 * processingProgress - 1, 2) - 1)
			: 0;

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
