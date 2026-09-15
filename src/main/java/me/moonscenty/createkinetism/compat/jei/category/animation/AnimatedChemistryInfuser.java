package me.moonscenty.createkinetism.compat.jei.category.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import me.moonscenty.createkinetism.content.chemistry.MechanicalChemistryInfuserBlock;
import me.moonscenty.createkinetism.content.chemistry.MechanicalChemistryInfuserRenderer;
import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;

/**
 * The Mechanical Chemistry Infuser for the JEI panel: three cells wide, the axle turning on its lid.
 *
 * <p>The block model already carries both side tanks, so drawing the one block draws the whole
 * machine. The axle and the two pipes are partials and are drawn beside it; the pipes sit where they
 * rest - 3px in towards the middle, see {@code MechanicalChemistryInfuserRenderer} - since the panel
 * has no recipe running to push them out.</p>
 *
 * <p>Turned to face the panel: the default state's front looks north, away from this camera. Facing
 * south, the model's Left tank lands on the left of the panel and its Right tank on the right, which
 * is where the category puts their slots. The partials are not turned by the blockstate, so they get
 * the same half turn by hand.</p>
 */
public class AnimatedChemistryInfuser extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(xOffset, yOffset, 100);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		ms.mulPose(Axis.YP.rotationDegrees(22.5f));
		int scale = 20;

		blockElement(CKPartialModels.CHEMISTRY_INFUSER_AXIS).rotateBlock(0, getCurrentAngle() * 2, 0)
			.scale(scale)
			.render(graphics);

		blockElement(CKBlocks.MECHANICAL_CHEMISTRY_INFUSER.getDefaultState()
			.setValue(MechanicalChemistryInfuserBlock.FACING, Direction.SOUTH)).scale(scale)
			.render(graphics);

		// Facing south turns the model half round, so the Left pipe is on the -X side and in is +X.
		float inset = MechanicalChemistryInfuserRenderer.PIPE_TRAVEL / 16f;
		blockElement(CKPartialModels.CHEMISTRY_INFUSER_LEFT_PIPE).rotateBlock(0, 180, 0)
			.atLocal(inset, 0, 0)
			.scale(scale)
			.render(graphics);
		blockElement(CKPartialModels.CHEMISTRY_INFUSER_RIGHT_PIPE).rotateBlock(0, 180, 0)
			.atLocal(-inset, 0, 0)
			.scale(scale)
			.render(graphics);

		ms.popPose();
	}
}
