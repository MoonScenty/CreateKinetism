package me.moonscenty.createkinetism.compat.jei.category.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import me.moonscenty.createkinetism.content.boiler.SodiumBurnerBlock;
import me.moonscenty.createkinetism.registry.CKBlocks;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;

/**
 * The Sodium Burner, for the {@code minimum_tier: 3} panel of {@code ThermalBoilingCategory} - drawn
 * lit (see {@link SodiumBurnerBlock#LIT}), since that recipe only ever runs while the burner actually
 * is. The turning shaft is drawn separately from the baked block model, same as
 * {@link AnimatedMechanicalElectrolyzer} - the real in-world renderer works the same way.
 */
public class AnimatedSodiumBurner extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(xOffset, yOffset, 200);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		ms.mulPose(Axis.YP.rotationDegrees(22.5f));

		int scale = 23;

		blockElement(shaft(Direction.Axis.Z)).rotateBlock(0, 0, getCurrentAngle() * 2)
			.atLocal(0, 0, 0)
			.scale(scale)
			.render(graphics);

		blockElement(CKBlocks.SODIUM_BURNER.getDefaultState()
			.setValue(SodiumBurnerBlock.LIT, true)).atLocal(0, 0, 0)
			.scale(scale)
			.render(graphics);

		ms.popPose();
	}
}
