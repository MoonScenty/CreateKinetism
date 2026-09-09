package me.moonscenty.createkinetism.compat.jei.category.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The plant, for the JEI panel: three floors, always showing the seam-hiding "connected" sprites
 * from Create's own fluid_tank sheet - the real block only picks those live from world connectivity,
 * which JEI's fake render context never has, same reason as {@code ThermalBoilingCategory}.
 *
 * <p>Nothing moves and nothing is kinetic - it boils on its own, faster with a heat source
 * underneath - so this is just the assembled shape sitting still.</p>
 */
public class AnimatedEvaporationPlant extends AnimatedKinetics {

	private static final int SCALE = 23;

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(xOffset, yOffset, 200);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		ms.mulPose(Axis.YP.rotationDegrees(22.5f));

		blockElement(CKPartialModels.EVAPORATION_PLANT_JEI_TOP).atLocal(0, -2.5, 0)
			.scale(SCALE)
			.render(graphics);
		blockElement(CKPartialModels.EVAPORATION_PLANT_JEI_MIDDLE).atLocal(0, -1.5, 0)
			.scale(SCALE)
			.render(graphics);
		blockElement(CKPartialModels.EVAPORATION_PLANT_JEI_BOTTOM).atLocal(0, -0.5, 0)
			.scale(SCALE)
			.render(graphics);

		ms.popPose();
	}
}
