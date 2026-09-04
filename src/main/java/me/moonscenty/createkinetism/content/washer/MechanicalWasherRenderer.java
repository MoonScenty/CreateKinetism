package me.moonscenty.createkinetism.content.washer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws the auger turning inside the vessel, and what the vessel is holding.
 *
 * <p>The fluid is drawn here rather than left to a basin because this machine has no basin: the
 * model is a closed box, and the contents sit in the cavity its inner walls describe.</p>
 */
public class MechanicalWasherRenderer extends KineticBlockEntityRenderer<MechanicalWasherBlockEntity> {

	/** The cavity the model's inner walls enclose - see mechanical_washer/block.json. */
	private static final float WALL = 2 / 16f;
	private static final float FLOOR = 2 / 16f;
	private static final float LID = 15 / 16f;

	public MechanicalWasherRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(MechanicalWasherBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {

		// No Flywheel bail-out: with no Visual counterpart, returning early would delete the auger.
		CachedBuffers.partial(CKPartialModels.MECHANICAL_WASHER_PROPELLER, be.getBlockState())
			.center()
			.rotateYDegrees(be.getPropellerAngle(partialTicks))
			.uncenter()
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

		renderFluids(be, ms, buffer, light);
	}

	/**
	 * The vessel's contents. Input and output share the cavity and are stacked, so a washer part way
	 * through a batch shows both the water going in and the clean slurry coming out.
	 */
	private void renderFluids(MechanicalWasherBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light) {
		if (be.inputTank == null || be.outputTank == null)
			return;

		float partialTicks = AnimationTickHolder.getPartialTicks();
		float total = 0;
		for (SmartFluidTankBehaviour tank : new SmartFluidTankBehaviour[] { be.inputTank, be.outputTank })
			for (TankSegment segment : tank.getTanks())
				if (!segment.getRenderedFluid()
					.isEmpty())
					total += segment.getFluidLevel()
						.getValue(partialTicks);
		if (total == 0)
			return;

		// Levels are per-tank fractions, so several full tanks would otherwise overflow the cavity.
		float scale = Math.min(1f, 1f / total);
		float y = FLOOR;

		for (SmartFluidTankBehaviour tank : new SmartFluidTankBehaviour[] { be.inputTank, be.outputTank }) {
			for (TankSegment segment : tank.getTanks()) {
				FluidStack fluid = segment.getRenderedFluid();
				float level = segment.getFluidLevel()
					.getValue(partialTicks);
				if (fluid.isEmpty() || level == 0)
					continue;

				float height = (LID - FLOOR) * level * scale;
				NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid, WALL, y, WALL, 1 - WALL, y + height,
					1 - WALL, buffer, ms, light, false, true);
				y += height;
			}
		}
	}

}
