package me.moonscenty.createkinetism.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.compat.jei.category.animations.AnimatedBlazeBurner;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedSodiumBurner;
import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedStrayChiller;
import me.moonscenty.createkinetism.content.heat.CKHeatLevels;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The heater under a JEI recipe, for the two new heat levels.
 *
 * <p>Create's mixing and packing categories - and this mod's own - draw the heater as
 * {@code heater.withHeat(condition.visualizeAsBlazeBurner()).draw(...)}. Handing the new levels over
 * here puts the right block in every one of those panels without touching any of them.</p>
 *
 * <p>The chiller is drawn at the burner's own anchor. The Sodium Burner is not: it puts its block at
 * local Y 0, where the Blaze Burner puts its own at 1.65, so it is moved 36.5px down to sit where a
 * burner would.</p>
 */
@Mixin(value = AnimatedBlazeBurner.class, remap = false)
public abstract class AnimatedBlazeBurnerMixin {

	@Unique
	private static final float createkinetism$SODIUM_BURNER_DROP = 36.5f;

	@Shadow
	private HeatLevel heatLevel;

	@Unique
	private final AnimatedSodiumBurner createkinetism$sodiumBurner = new AnimatedSodiumBurner();
	@Unique
	private final AnimatedStrayChiller createkinetism$chiller = new AnimatedStrayChiller();

	@Inject(method = "draw", at = @At("HEAD"), cancellable = true)
	private void createkinetism$drawNewHeaters(GuiGraphics graphics, int xOffset, int yOffset, CallbackInfo ci) {
		if (heatLevel == CKHeatLevels.SODIUM_HEATED) {
			PoseStack ms = graphics.pose();
			ms.pushPose();
			ms.translate(0, createkinetism$SODIUM_BURNER_DROP, 0);
			createkinetism$sodiumBurner.draw(graphics, xOffset, yOffset);
			ms.popPose();
			ci.cancel();
		} else if (heatLevel == CKHeatLevels.CHILLED) {
			createkinetism$chiller.draw(graphics, xOffset, yOffset);
			ci.cancel();
		}
	}
}
