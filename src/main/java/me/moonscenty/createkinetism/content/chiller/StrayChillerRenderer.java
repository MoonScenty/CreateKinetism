package me.moonscenty.createkinetism.content.chiller;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;

import me.moonscenty.createkinetism.content.heat.CKHeatLevels;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Create's {@code BlazeBurnerRenderer}, drawing this mod's parts.
 *
 * <p>Copied because Create's names its partial models and flame sprites as constants inside the
 * method bodies, so there is no seam to hand it different ones. The arithmetic - head bob, rod
 * float, flame scroll speed - is Create's unchanged; only the models and sprites are swapped for the
 * ones under {@code stray_chiller/}. The train and logistics hats are still Create's own.</p>
 *
 * <p>This is the only thing that draws the chiller: it has no Flywheel visual - see
 * {@link StrayChillerBlockEntity}.</p>
 *
 * <p>A working chiller is Chilled, a level Create's drawing code ranks below everything that shows
 * rods and a flame. It is drawn as Seething instead, blue flame and all - see {@link #visualLevel}.</p>
 */
public class StrayChillerRenderer extends SafeBlockEntityRenderer<StrayChillerBlockEntity> {

	public StrayChillerRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(StrayChillerBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource bufferSource, int light, int overlay) {
		HeatLevel heatLevel = visualLevel(be.getHeatLevelFromBlock());
		if (heatLevel == HeatLevel.NONE)
			return;

		Level level = be.getLevel();
		BlockState blockState = be.getBlockState();
		float animation = be.headAnimation.getValue(partialTicks) * .175f;
		float horizontalAngle = AngleHelper.rad(be.getHeadAngle(partialTicks));
		boolean canDrawFlame = heatLevel.isAtLeast(HeatLevel.FADING);
		boolean drawGoggles = be.goggles;
		PartialModel drawHat = be.hat ? AllPartialModels.TRAIN_HAT
			: be.stockKeeper ? AllPartialModels.LOGISTICS_HAT : null;
		int hashCode = be.hashCode();

		renderShared(ms, null, bufferSource, level, blockState, heatLevel, animation, horizontalAngle,
			canDrawFlame, drawGoggles, drawHat, hashCode);
	}

	public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, MultiBufferSource bufferSource, LerpedFloat headAngle, boolean conductor) {
		BlockState state = context.state;
		HeatLevel heatLevel = visualLevel(BlazeBurnerBlock.getHeatLevelOf(state));
		if (heatLevel == HeatLevel.NONE)
			return;

		if (!heatLevel.isAtLeast(HeatLevel.FADING))
			heatLevel = HeatLevel.FADING;

		Level level = context.world;
		float horizontalAngle = AngleHelper.rad(headAngle.getValue(AnimationTickHolder.getPartialTicks(level)));
		boolean drawGoggles = context.blockEntityData.contains("Goggles");
		boolean drawHat = conductor || context.blockEntityData.contains("TrainHat");
		int hashCode = context.hashCode();

		renderShared(matrices.getViewProjection(), matrices.getModel(), bufferSource, level, state, heatLevel, 0,
			horizontalAngle, false, drawGoggles, drawHat ? AllPartialModels.TRAIN_HAT : null, hashCode);
	}

	public static void renderShared(PoseStack ms, @Nullable PoseStack modelTransform, MultiBufferSource bufferSource,
		Level level, BlockState blockState, HeatLevel heatLevel, float animation, float horizontalAngle,
		boolean canDrawFlame, boolean drawGoggles, @Nullable PartialModel drawHat, int hashCode) {

		boolean blockAbove = animation > 0.125f;
		float time = AnimationTickHolder.getRenderTime(level);
		float renderTick = time + (hashCode % 13) * 16f;
		float offsetMult = heatLevel.isAtLeast(HeatLevel.FADING) ? 64 : 16;
		float offset = Mth.sin((float) ((renderTick / 16f) % (2 * Math.PI))) / offsetMult;
		float offset1 = Mth.sin((float) ((renderTick / 16f + Math.PI) % (2 * Math.PI))) / offsetMult;
		float offset2 = Mth.sin((float) ((renderTick / 16f + Math.PI / 2) % (2 * Math.PI))) / offsetMult;
		float headY = offset - (animation * .75f);

		ms.pushPose();

		PartialModel headModel = getHeadModel(heatLevel, blockAbove);

		SuperByteBuffer headBuffer = CachedBuffers.partial(headModel, blockState);
		if (modelTransform != null)
			headBuffer.transform(modelTransform);
		headBuffer.translate(0, headY, 0);
		draw(headBuffer, horizontalAngle, ms, bufferSource.getBuffer(RenderType.solid()));

		if (drawGoggles) {
			PartialModel gogglesModel = headModel == CKPartialModels.STRAY_CHILLER_INERT
				? CKPartialModels.STRAY_CHILLER_GOGGLES_SMALL : CKPartialModels.STRAY_CHILLER_GOGGLES;

			SuperByteBuffer gogglesBuffer = CachedBuffers.partial(gogglesModel, blockState);
			if (modelTransform != null)
				gogglesBuffer.transform(modelTransform);
			gogglesBuffer.translate(0, headY + 8 / 16f, 0);
			draw(gogglesBuffer, horizontalAngle, ms, bufferSource.getBuffer(RenderType.solid()));
		}

		if (drawHat != null) {
			SuperByteBuffer hatBuffer = CachedBuffers.partial(drawHat, blockState);
			if (modelTransform != null)
				hatBuffer.transform(modelTransform);
			hatBuffer.translate(0, headY, 0);
			if (headModel == CKPartialModels.STRAY_CHILLER_INERT) {
				hatBuffer.translateY(0.5f)
					.center()
					.scale(0.75f)
					.uncenter();
			} else {
				hatBuffer.translateY(0.75f);
			}
			VertexConsumer cutout = bufferSource.getBuffer(RenderType.cutoutMipped());
			hatBuffer.rotateCentered(horizontalAngle + Mth.PI, Direction.UP)
				.translate(0.5f, 0, 0.5f)
				.light(LightTexture.FULL_BRIGHT)
				.renderInto(ms, cutout);
		}

		if (heatLevel.isAtLeast(HeatLevel.FADING)) {
			PartialModel rodsModel = heatLevel == HeatLevel.SEETHING ? CKPartialModels.STRAY_CHILLER_SUPER_RODS
				: CKPartialModels.STRAY_CHILLER_RODS;
			PartialModel rodsModel2 = heatLevel == HeatLevel.SEETHING ? CKPartialModels.STRAY_CHILLER_SUPER_RODS_2
				: CKPartialModels.STRAY_CHILLER_RODS_2;

			SuperByteBuffer rodsBuffer = CachedBuffers.partial(rodsModel, blockState);
			if (modelTransform != null)
				rodsBuffer.transform(modelTransform);
			rodsBuffer.translate(0, offset1 + animation + .125f, 0)
				.light(LightTexture.FULL_BRIGHT)
				.renderInto(ms, bufferSource.getBuffer(RenderType.solid()));

			SuperByteBuffer rodsBuffer2 = CachedBuffers.partial(rodsModel2, blockState);
			if (modelTransform != null)
				rodsBuffer2.transform(modelTransform);
			rodsBuffer2.translate(0, offset2 + animation - 3 / 16f, 0)
				.light(LightTexture.FULL_BRIGHT)
				.renderInto(ms, bufferSource.getBuffer(RenderType.solid()));
		}

		if (canDrawFlame && blockAbove) {
			SpriteShiftEntry spriteShift = heatLevel == HeatLevel.SEETHING ? StrayChillerSpriteShifts.SUPER_FLAME
				: StrayChillerSpriteShifts.FLAME;

			float spriteWidth = spriteShift.getTarget()
				.getU1()
				- spriteShift.getTarget()
					.getU0();

			float spriteHeight = spriteShift.getTarget()
				.getV1()
				- spriteShift.getTarget()
					.getV0();

			float speed = 1 / 32f + 1 / 64f * heatLevel.ordinal();

			double vScroll = speed * time;
			vScroll = vScroll - Math.floor(vScroll);
			vScroll = vScroll * spriteHeight / 2;

			double uScroll = speed * time / 2;
			uScroll = uScroll - Math.floor(uScroll);
			uScroll = uScroll * spriteWidth / 2;

			SuperByteBuffer flameBuffer = CachedBuffers.partial(CKPartialModels.STRAY_CHILLER_FLAME, blockState);
			if (modelTransform != null)
				flameBuffer.transform(modelTransform);
			flameBuffer.shiftUVScrolling(spriteShift, (float) uScroll, (float) vScroll);

			VertexConsumer cutout = bufferSource.getBuffer(RenderType.cutoutMipped());
			draw(flameBuffer, horizontalAngle, ms, cutout);
		}

		ms.popPose();
	}

	/**
	 * The stray's own faces - active, idle and inert - at every level. Seething, which is how a
	 * working chiller is drawn, keeps the blue rods and flame but not Create's superheated blaze face.
	 */
	public static PartialModel getHeadModel(HeatLevel heatLevel, boolean blockAbove) {
		if (heatLevel.isAtLeast(HeatLevel.SEETHING)) {
			return blockAbove ? CKPartialModels.STRAY_CHILLER_ACTIVE : CKPartialModels.STRAY_CHILLER_IDLE;
		} else if (heatLevel.isAtLeast(HeatLevel.FADING)) {
			return blockAbove && heatLevel.isAtLeast(HeatLevel.KINDLED) ? CKPartialModels.STRAY_CHILLER_ACTIVE
				: CKPartialModels.STRAY_CHILLER_IDLE;
		} else {
			return CKPartialModels.STRAY_CHILLER_INERT;
		}
	}

	/** The Blaze Burner level a chiller at this level looks like: Chilled shows as Seething, blue. */
	public static HeatLevel visualLevel(HeatLevel heatLevel) {
		return heatLevel == CKHeatLevels.CHILLED ? HeatLevel.SEETHING : heatLevel;
	}

	private static void draw(SuperByteBuffer buffer, float horizontalAngle, PoseStack ms, VertexConsumer vc) {
		buffer.rotateCentered(horizontalAngle, Direction.UP)
			.light(LightTexture.FULL_BRIGHT)
			.renderInto(ms, vc);
	}
}
