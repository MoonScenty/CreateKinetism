package me.moonscenty.createkinetism.content.vat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws the cogwheel, pole and stirring head of a vat. This is Create's
 * {@code MechanicalMixerRenderer} with the block entity type swapped out; reusing Create's partial
 * models is what makes the machines animate identically to a Mechanical Mixer.
 *
 * <p>Two blocks are the exception - the Mechanical Electrolyzer and the Nutrition Bar Mixer own
 * copies of the mixer's pole and head, so those can be reshaped or recoloured without dragging
 * every other vat along with them.</p>
 */
public class VatRenderer extends KineticBlockEntityRenderer<VatBlockEntity> {

	public VatRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRenderOffScreen(VatBlockEntity be) {
		return true;
	}

	@Override
	protected void renderSafe(VatBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light,
		int overlay) {

		// No Flywheel bail-out here on purpose. That guard is only correct when a Visual counterpart
		// exists to draw these parts instead; without one it just deletes the cog and the stirring
		// pole on any default install, which is exactly what it did before this comment was written.
		BlockState blockState = be.getBlockState();

		// Two vats own copies of the mixer's parts so they can be reshaped or recoloured without
		// dragging the rest along; everything else still borrows Create's.
		boolean separator = blockState.is(CKBlocks.MECHANICAL_ELECTROLYZER.get());
		boolean nutritionBars = blockState.is(CKBlocks.NUTRITION_BAR_MIXER.get());

		VertexConsumer vb = buffer.getBuffer(RenderType.solid());

		// The separator is driven end on end, so it shows a turning shaft where the others show a cog.
		// It has to go through getRotatedModel rather than CachedBuffers.partial: a partial model is
		// baked in one fixed orientation and the state handed to it is only read for light, so asking
		// for AllPartialModels.SHAFT draws it standing on end no matter which axis you pass.
		SuperByteBuffer superBuffer = separator
			? getRotatedModel(be, shaft(getRotationAxisOf(be)))
			: CachedBuffers.partial(AllPartialModels.SHAFTLESS_COGWHEEL, blockState);
		standardKineticRotationTransform(superBuffer, be, light).renderInto(ms, vb);

		float renderedHeadOffset = be.getRenderedHeadOffset(partialTicks);

		PartialModel poleModel = separator ? CKPartialModels.MECHANICAL_ELECTROLYZER_POLE
			: nutritionBars ? CKPartialModels.NUTRITION_BAR_MIXER_POLE
				: AllPartialModels.MECHANICAL_MIXER_POLE;
		PartialModel headModel = separator ? CKPartialModels.MECHANICAL_ELECTROLYZER_HEAD
			: nutritionBars ? CKPartialModels.NUTRITION_BAR_MIXER_HEAD
				: AllPartialModels.MECHANICAL_MIXER_HEAD;

		SuperByteBuffer poleRender = CachedBuffers.partial(poleModel, blockState);
		poleRender.translate(0, -renderedHeadOffset, 0)
			.light(light)
			.renderInto(ms, vb);

		// Whether the whisk turns is a statement about what the machine does. Most of these are not
		// stirring anything - they press or inject, and the cogwheel above is what shows they run - so
		// their heads only travel. The Nutrition Bar Mixer really is mixing, so it gets Create's spin.
		VertexConsumer vbCutout = buffer.getBuffer(RenderType.cutoutMipped());
		SuperByteBuffer headRender = CachedBuffers.partial(headModel, blockState);
		if (nutritionBars) {
			float speed = be.getRenderedHeadRotationSpeed(partialTicks);
			float time = AnimationTickHolder.getRenderTime(be.getLevel());
			headRender.rotateCentered(((time * speed * 6 / 10f) % 360) / 180 * (float) Math.PI, Direction.UP);
		}
		headRender.translate(0, -renderedHeadOffset, 0)
			.light(light)
			.renderInto(ms, vbCutout);
	}
}
