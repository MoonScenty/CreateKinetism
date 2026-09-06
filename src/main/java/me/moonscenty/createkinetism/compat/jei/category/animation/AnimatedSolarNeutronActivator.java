package me.moonscenty.createkinetism.compat.jei.category.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import me.moonscenty.createkinetism.registry.CKBlocks;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The Solar Neutron Activator under its basin, for the JEI panel.
 *
 * <p>The only picture in this mod's recipe list with the basin on top. Every other machine here
 * hangs over one; this one reaches up into it, and the panel has to say so or a player will build it
 * upside down.</p>
 *
 * <p>Nothing turns, because nothing on this machine is driven - see
 * {@link me.moonscenty.createkinetism.content.solar.SolarNeutronActivatorBlock}.</p>
 */
public class AnimatedSolarNeutronActivator extends AnimatedKinetics {

	/** Screen pixels, not block units - the two are drawn in their own poses so this stays exact. */
	private static final int BASIN_DROP = 9;

	private static final int SCALE = 23;

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		drawAt(graphics, xOffset, yOffset,
			() -> blockElement(CKBlocks.SOLAR_NEUTRON_ACTIVATOR.getDefaultState()).scale(SCALE)
				.render(graphics));

		// Negative, where every other machine here is positive: the basin is above this one. Its own
		// pose so the nudge below is measured on screen rather than through the isometric rotation.
		drawAt(graphics, xOffset, yOffset + BASIN_DROP,
			() -> blockElement(AllBlocks.BASIN.getDefaultState()).atLocal(0, -2, 0)
				.scale(SCALE)
				.render(graphics));
	}

	private static void drawAt(GuiGraphics graphics, int x, int y, Runnable element) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(x, y, 200);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		ms.mulPose(Axis.YP.rotationDegrees(22.5f));
		element.run();
		ms.popPose();
	}
}
