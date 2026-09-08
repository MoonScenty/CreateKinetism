package me.moonscenty.createkinetism.content.chemistry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;

import me.moonscenty.createkinetism.foundation.client.ChemicalBoxRenderer;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The infuser's shaft and the three gases inside it.
 *
 * <p>Every tank in this block is a window with nothing painted behind it, so all three are drawn
 * here. What goes in them is a Mekanism chemical rather than a fluid, hence
 * {@link ChemicalBoxRenderer} - Create's own renderer only takes a {@code FluidStack}.</p>
 *
 * <p>The boxes are the model's own cavities, inset by a hair so the gas does not z-fight the glass
 * it is behind. Sides fill upward from their floor; the main tank does too, and it is the shorter of
 * the three because it lies on its side across the back.</p>
 */
public class MechanicalChemistryInfuserRenderer
	extends KineticBlockEntityRenderer<MechanicalChemistryInfuserBlockEntity> {

	private static final float PX = 1 / 16f;

	/** A hair inside the glass. Without it the two surfaces flicker against each other. */
	private static final float INSET = 0.01f;

	public MechanicalChemistryInfuserRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(MechanicalChemistryInfuserBlockEntity be, float partialTicks,
		PoseStack ms, MultiBufferSource buffer, int light, int overlay) {

		BlockState blockState = be.getBlockState();

		// The stub of shaft in the recess underneath. Drawn here rather than in the blockstate because
		// it is the only part of this block that moves.
		//
		// Cutout, not solid. Create's shaft texture is a 4x4 cross-section in a 16x16 sheet and the
		// other 240 pixels are transparent black - which the solid pass does not honour, so drawing it
		// there paints a black square where the shaft should be.
		//
		// The axis is written out rather than taken from standardKineticRotationTransform, which reads
		// it back off the block. This block only ever turns about Y - it is driven from underneath and
		// has no axis property to vary - so saying so directly leaves nothing to go wrong.
		VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
		CachedBuffers.partial(CKPartialModels.CHEMISTRY_INFUSER_SHAFT, blockState)
			.light(light)
			.rotateCentered(getAngleForBe(be, be.getBlockPos(), Axis.Y), Direction.UP)
			.renderInto(ms, vb);

		// Left and right feed tanks, at the front. Model coordinates, so this all follows the
		// blockstate's own rotation and needs no facing maths of its own.
		fill(be.leftTank, be.leftLevel, partialTicks, 0.1f, 4.1f, 11.1f, 6.9f, 15.9f, 15.9f,
			buffer, ms, light);
		fill(be.rightTank, be.rightLevel, partialTicks, 9.1f, 4.1f, 11.1f, 15.9f, 15.9f, 15.9f,
			buffer, ms, light);

		// The main tank, lying across the back.
		fill(be.mainTank, be.mainLevel, partialTicks, 0.1f, 4.1f, 0.1f, 15.9f, 11.9f, 9.9f,
			buffer, ms, light);
	}


	/**
	 * One tank's contents, filling its cavity from the floor up.
	 *
	 * <p>The floor is nudged down out of the glass rather than up into the gas, so a nearly empty
	 * tank still shows a sliver instead of nothing at all.</p>
	 */
	private static void fill(IChemicalTank tank, LerpedFloat lerp, float partialTicks, float x0,
		float y0, float z0, float x1, float y1, float z1, MultiBufferSource buffer, PoseStack ms,
		int light) {

		ChemicalStack held = tank.getStack();
		if (held.isEmpty())
			return;
		float level = lerp.getValue(partialTicks);
		if (level <= 0)
			return;
		level = Math.max(level, 0.05f);

		float bottom = y0 * PX + INSET;
		float top = bottom + (y1 * PX - INSET - bottom) * level;
		ChemicalBoxRenderer.renderChemicalBox(held, x0 * PX + INSET, bottom, z0 * PX + INSET,
			x1 * PX - INSET, top, z1 * PX - INSET, buffer, ms, light, false);
	}
}
