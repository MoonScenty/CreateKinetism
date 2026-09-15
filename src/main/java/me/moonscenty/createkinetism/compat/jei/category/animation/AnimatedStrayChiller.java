package me.moonscenty.createkinetism.compat.jei.category.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

import me.moonscenty.createkinetism.content.chiller.StrayChillerSpriteShifts;
import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;

/**
 * A working Stray Chiller, for any recipe that needs {@code "chilled"}.
 *
 * <p>Create's {@code AnimatedBlazeBurner} with this mod's parts, drawn at the same anchor and scale -
 * {@code AnimatedBlazeBurnerMixin} hands the Chilled case over to this, so every category that already
 * draws a heater in the right place draws the chiller there as well. A chiller at work looks like a
 * seething burner, blue flame and all (see {@code StrayChillerRenderer}), so that is what is drawn.</p>
 */
public class AnimatedStrayChiller extends AnimatedKinetics {

	@Override
	public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack matrixStack = graphics.pose();
		matrixStack.pushPose();
		matrixStack.translate(xOffset, yOffset, 200);
		matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
		matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));
		int scale = 23;

		float offset = (Mth.sin(AnimationTickHolder.getRenderTime() / 16f) + 0.5f) / 16f;

		blockElement(CKBlocks.STRAY_CHILLER.getDefaultState()).atLocal(0, 1.65, 0)
			.scale(scale)
			.render(graphics);

		blockElement(CKPartialModels.STRAY_CHILLER_ACTIVE).atLocal(1, 1.8, 1)
			.rotate(0, 180, 0)
			.scale(scale)
			.render(graphics);
		blockElement(CKPartialModels.STRAY_CHILLER_SUPER_RODS_2).atLocal(1, 1.7 + offset, 1)
			.rotate(0, 180, 0)
			.scale(scale)
			.render(graphics);

		matrixStack.scale(scale, -scale, scale);
		matrixStack.translate(0, -1.8, 0);

		SpriteShiftEntry spriteShift = StrayChillerSpriteShifts.SUPER_FLAME;

		float spriteWidth = spriteShift.getTarget()
			.getU1()
			- spriteShift.getTarget()
				.getU0();

		float spriteHeight = spriteShift.getTarget()
			.getV1()
			- spriteShift.getTarget()
				.getV0();

		float time = AnimationTickHolder.getRenderTime(Minecraft.getInstance().level);
		float speed = 1 / 32f + 1 / 64f * HeatLevel.SEETHING.ordinal();

		double vScroll = speed * time;
		vScroll = vScroll - Math.floor(vScroll);
		vScroll = vScroll * spriteHeight / 2;

		double uScroll = speed * time / 2;
		uScroll = uScroll - Math.floor(uScroll);
		uScroll = uScroll * spriteWidth / 2;

		CachedBuffers.partial(CKPartialModels.STRAY_CHILLER_FLAME, Blocks.AIR.defaultBlockState())
			.shiftUVScrolling(spriteShift, (float) uScroll, (float) vScroll)
			.light(LightTexture.FULL_BRIGHT)
			.renderInto(matrixStack, graphics.bufferSource().getBuffer(RenderType.cutoutMipped()));
		matrixStack.popPose();
	}
}
