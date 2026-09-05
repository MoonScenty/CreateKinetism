package me.moonscenty.createkinetism.content.accumulator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The shaft through the sides, the bracket under the cogwheel, and the tool lying on top.
 *
 * <p>The shaft is not in the block model - it turns, so it is drawn here at whatever the input
 * network is doing, the same way Create's Speed Controller draws its own. The bracket only appears
 * once there is a large cogwheel above to hold, which is also when the block has anywhere to put its
 * charge.</p>
 */
public class KineticAccumulatorRenderer extends KineticBlockEntityRenderer<KineticAccumulatorBlockEntity> {

	public KineticAccumulatorRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(KineticAccumulatorBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {

		VertexConsumer vb = buffer.getBuffer(Sheets.solidBlockSheet());

		renderRotatingBuffer(be, getRotatedModel(be, shaft(getRotationAxisOf(be))), ms, vb, light);
		renderBracket(be, ms, vb);
		renderHeldTool(be, ms, buffer, light, overlay);
	}

	/** Only when something is actually sat on top of it, exactly as Create's bracket behaves. */
	private void renderBracket(KineticAccumulatorBlockEntity be, PoseStack ms, VertexConsumer vb) {
		BlockPos above = be.getBlockPos()
			.above();
		if (be.getLevel() == null || !ICogWheel.isLargeCog(be.getLevel()
			.getBlockState(above)))
			return;

		BlockState state = be.getBlockState();
		boolean alongX = state.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS) == Axis.X;

		SuperByteBuffer bracket = CachedBuffers.partial(CKPartialModels.KINETIC_ACCUMULATOR_BRACKET, state);
		bracket.translate(0, 1, 0);
		bracket.rotateCentered((float) (alongX ? Math.PI : Math.PI / 2), Direction.UP);
		bracket.light(LevelRenderer.getLightColor(be.getLevel(), above));
		bracket.renderInto(ms, vb);
	}

	/** Laid flat rather than stood upright, so it reads as put down rather than held. */
	private void renderHeldTool(KineticAccumulatorBlockEntity be, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {

		ItemStack tool = be.getHeldTool();
		if (tool.isEmpty())
			return;

		ms.pushPose();
		ms.translate(0.5f, 1.02f, 0.5f);
		ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
		ms.scale(0.6f, 0.6f, 0.6f);
		Minecraft.getInstance()
			.getItemRenderer()
			.renderStatic(tool, ItemDisplayContext.FIXED, light, overlay, ms, buffer, be.getLevel(), 0);
		ms.popPose();
	}
}
