package me.moonscenty.createkinetism.compat.jei.category.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import me.moonscenty.createkinetism.registry.CKBlocks;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The Solar Neutron Activator, for the JEI panel.
 *
 * <p>One block element and no basin drawn beside it: the basin is part of the machine's own model
 * now - a basin cannot hold a Mekanism chemical, so both of this recipe's halves are the machine's
 * tanks and there was nothing left for a separate basin to do.</p>
 *
 * <p>Nothing turns, because nothing on this machine is driven - see
 * {@link me.moonscenty.createkinetism.content.solar.SolarNeutronActivatorBlock}.</p>
 */
public class AnimatedSolarNeutronActivator extends AnimatedKinetics {

	private static final int SCALE = 23;

	/**
	 * Screen pixels, downward.
	 *
	 * <p>The model stands 23 pixels tall - it reaches seven up into the cell the panel holds - and
	 * it is drawn from its own origin, so left alone it hangs high in the panel. Measured on screen
	 * rather than in block units so the isometric rotation does not have to be undone to read it.</p>
	 */
	private static final int DROP = 5;

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(xOffset, yOffset + DROP, 200);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		ms.mulPose(Axis.YP.rotationDegrees(22.5f));
		blockElement(CKBlocks.SOLAR_NEUTRON_ACTIVATOR.getDefaultState()).scale(SCALE)
			.render(graphics);
		ms.popPose();
	}
}
