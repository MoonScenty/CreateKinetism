package me.moonscenty.createkinetism.content.infuser;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import mekanism.api.chemical.ChemicalStack;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import me.moonscenty.createkinetism.foundation.client.ChemicalBoxRenderer;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The infuser's cogwheel, nozzle and the infusion inside it.
 *
 * <p>Create's spout has no cogwheel because it costs nothing to run. This one does, so it needs
 * something that visibly turns - a shaftless cog on the same axis the shaft enters by, Create's
 * geometry with this machine's own texture (see
 * {@link CKPartialModels#MECHANICAL_METALLURGIC_INFUSER_COGWHEEL}).</p>
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

		SuperByteBuffer cog = CachedBuffers.partial(CKPartialModels.MECHANICAL_METALLURGIC_INFUSER_COGWHEEL,
			blockState);
		standardKineticRotationTransform(cog, be, light).renderInto(ms, vb);

		// A Mekanism chemical rather than a fluid, so the box comes from our own renderer - see
		// ChemicalBoxRenderer for why Create's could not be used directly.
		ChemicalStack chemical = be.getStoredChemical();
		float fill = be.fillLevel.getValue(partialTicks);

		if (!chemical.isEmpty() && fill != 0) {
			fill = Math.max(fill, 0.175f);
			float min = 2.5f / 16f;
			float max = min + (11 / 16f);
			float yOffset = (11 / 16f) * fill;

			ms.pushPose();
			ms.translate(0, yOffset, 0);
			ChemicalBoxRenderer.renderChemicalBox(chemical, min, min - yOffset, min, max, min, max,
				buffer, ms, light, false);
			ms.popPose();
		}

		// The nozzle reaches down while the infusion is being applied, and a column of it falls from
		// there. Both are Create's spout timings: the stream swells and thins over the last ten ticks,
		// and the segments pull apart with it.
		int processingTicks = be.processingTicks;
		float processingPT = processingTicks - partialTicks;
		float processingProgress = Mth.clamp(1 - (processingPT - 5) / 10, 0, 1);
		float radius = 0;

		if (!chemical.isEmpty() && processingTicks != -1) {
			radius = (float) (Math.pow(2 * processingProgress - 1, 2) - 1);
			AABB bb = new AABB(0.5, 0.0, 0.5, 0.5, -1.2, 0.5).inflate(radius / 32f);
			ChemicalBoxRenderer.renderChemicalBox(chemical, (float) bb.minX, (float) bb.minY,
				(float) bb.minZ, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ, buffer, ms, light,
				true);
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
