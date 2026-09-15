package me.moonscenty.createkinetism.content.chemistry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import mekanism.api.chemical.ChemicalStack;

import me.moonscenty.createkinetism.foundation.client.ChemicalBoxRenderer;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The infuser's moving parts and the gas in its middle tank.
 *
 * <ul>
 *   <li>The axle on top, turned by Create's own kinetic transform about Y.</li>
 *   <li>The two pipes between the middle tank and the side tanks. At rest each is drawn
 *       {@value #PIPE_TRAVEL}px in from where the model puts it, towards the middle; while a recipe
 *       runs it slides those {@value #PIPE_TRAVEL}px back out to the model's position, into its side
 *       tank - by {@link MechanicalChemistryInfuserBlockEntity#pipeExtension}.</li>
 *   <li>The main tank's gas, behind the glass. What goes in it is a Mekanism chemical rather than a
 *       fluid, hence {@link ChemicalBoxRenderer}.</li>
 * </ul>
 *
 * <p>Partial models are not turned by the blockstate the way the block model is, so the pipes and
 * the gas are drawn inside the same Y rotation the blockstate gives the model: {@code facing=north}
 * is unrotated, and each step clockwise adds 90 degrees.</p>
 */
public class MechanicalChemistryInfuserRenderer
	extends KineticBlockEntityRenderer<MechanicalChemistryInfuserBlockEntity> {

	private static final float PX = 1 / 16f;

	/** How far each pipe slides out, in pixels. */
	public static final float PIPE_TRAVEL = 3;

	/** A hair inside the glass. Without it the two surfaces flicker against each other. */
	private static final float INSET = 0.01f;

	public MechanicalChemistryInfuserRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRenderOffScreen(MechanicalChemistryInfuserBlockEntity be) {
		return true;
	}

	@Override
	protected void renderSafe(MechanicalChemistryInfuserBlockEntity be, float partialTicks,
		PoseStack ms, MultiBufferSource buffer, int light, int overlay) {

		BlockState blockState = be.getBlockState();

		// Cutout: the axle's end texture is a small cross-section on a mostly transparent sheet.
		standardKineticRotationTransform(
			CachedBuffers.partial(CKPartialModels.CHEMISTRY_INFUSER_AXIS, blockState), be, light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

		float modelAngle = modelAngle(blockState.getValue(MechanicalChemistryInfuserBlock.FACING));
		// How far in from the model's position each pipe is: all the way in at rest, none when extended.
		float inset = (1 - be.pipeExtension.getValue(partialTicks)) * PIPE_TRAVEL * PX;

		// The Left pipe is at +X in the model, so in is -X; the Right pipe the other way.
		CachedBuffers.partial(CKPartialModels.CHEMISTRY_INFUSER_LEFT_PIPE, blockState)
			.rotateCentered(modelAngle * Mth.DEG_TO_RAD, Direction.UP)
			.translate(-inset, 0, 0)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
		CachedBuffers.partial(CKPartialModels.CHEMISTRY_INFUSER_RIGHT_PIPE, blockState)
			.rotateCentered(modelAngle * Mth.DEG_TO_RAD, Direction.UP)
			.translate(inset, 0, 0)
			.light(light)
			.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

		renderGas(be, partialTicks, modelAngle, ms, buffer, light);
	}

	/**
	 * The model's Y rotation for a facing, as {@code SuperByteBuffer}/{@code PoseStack} take it. The
	 * blockstate turns the model clockwise seen from above - {@code y: 90} for east - and a positive
	 * angle about UP turns it the other way, hence the minus.
	 */
	private static float modelAngle(Direction facing) {
		return -((facing.toYRot() + 180) % 360);
	}

	/**
	 * The main tank's contents, filling its cavity from the floor up: between the floor plate (y 3)
	 * and the lid (y 15), the full width, and just inside the two glass walls (z 0.1 and 15.9).
	 *
	 * <p>The floor is nudged down out of the glass rather than up into the gas, so a nearly empty tank
	 * still shows a sliver instead of nothing at all.</p>
	 */
	private static void renderGas(MechanicalChemistryInfuserBlockEntity be, float partialTicks, float modelAngle,
		PoseStack ms, MultiBufferSource buffer, int light) {

		ChemicalStack held = be.mainTank.getStack();
		if (held.isEmpty())
			return;
		float level = be.mainLevel.getValue(partialTicks);
		if (level <= 0)
			return;
		level = Math.max(level, 0.05f);

		float bottom = 3 * PX + INSET;
		float top = bottom + (15 * PX - INSET - bottom) * level;

		ms.pushPose();
		ms.translate(0.5, 0, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(modelAngle));
		ms.translate(-0.5, 0, -0.5);
		ChemicalBoxRenderer.renderChemicalBox(held, INSET, bottom, 0.1f * PX + INSET, 1 - INSET, top,
			15.9f * PX - INSET, buffer, ms, light, false);
		ms.popPose();
	}
}
