package me.moonscenty.createkinetism.content.compressor;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The two axles, the spinning head, the ram, and whatever each is holding.
 *
 * <p>Every part is drawn here rather than left to the kinetic pass, because that pass returns early
 * under Flywheel and this machine ships no Visual to take over - the same trap the vats and the
 * Multimeter had to step around.</p>
 *
 * <p>The two axles turn about different axes on purpose: the one at the front runs along the
 * machine's facing, the one at the back crosses it. That is the whole point of the block being two
 * blocks - see {@link KinetiteCompressorBlock}.</p>
 */
public class KinetiteCompressorRenderer extends KineticBlockEntityRenderer<KinetiteCompressorBlockEntity> {

	public KinetiteCompressorRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public boolean shouldRenderOffScreen(KinetiteCompressorBlockEntity be) {
		// The far half of the machine sits outside this block, so it must survive frustum culling
		// of its own position.
		return true;
	}

	@Override
	protected void renderSafe(KinetiteCompressorBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {

		BlockState state = be.getBlockState();
		Direction facing = state.getValue(HORIZONTAL_FACING);
		VertexConsumer vb = buffer.getBuffer(RenderType.solid());

		// The front two turn with this half's shaft; the cross axle belongs to the cradle behind and
		// turns with that one, so it has to be asked for its own angle.
		spin(be, CKPartialModels.KINETITE_COMPRESSOR_INPUT_SHAFT, state, facing, light)
			.renderInto(ms, vb);
		spin(be, CKPartialModels.KINETITE_COMPRESSOR_ROTATING_HEAD, state, facing, light)
			.renderInto(ms, vb);

		KineticBlockEntity cradle = cradleOf(be);
		if (cradle != null)
			spinBehind(cradle, CKPartialModels.KINETITE_COMPRESSOR_OUTPUT_SHAFT, state, facing, light)
				.renderInto(ms, vb);

		// The ram travels toward the front. The translate is called after the yaw, so by the rule
		// above it lands on the vertex first - it is model space, where the front is -Z.
		float offset = be.getRamOffset(partialTicks);
		aimed(CKPartialModels.KINETITE_COMPRESSOR_MOVING_HEAD, state, facing).translate(0, 0, -offset)
			.light(light)
			.renderInto(ms, vb);

		renderHeldItems(be, partialTicks, ms, buffer, light, overlay, facing, offset);
	}

	/**
	 * Turn the part on its driver's axis, then aim it.
	 *
	 * <p>The call order is the whole trick, and it reads backwards. {@code SuperByteBuffer} keeps a
	 * {@code PoseStack}, which post-multiplies, so a vertex is transformed by the <em>last</em> call
	 * first: yaw the part out of the orientation it was modelled in and into the world, and only then
	 * spin it about the driver's real world axis. Doing it the other way round - yaw first in code -
	 * spins the part before it has been aimed, and the axle rolls sideways on any machine that is not
	 * facing north.</p>
	 *
	 * @param driver the block entity whose network this part turns with, and whose rotation axis it
	 *               therefore uses - not always the one being rendered, since the cross axle at the
	 *               back belongs to the cradle
	 */
	private static SuperByteBuffer spin(KineticBlockEntity driver,
		dev.engine_room.flywheel.lib.model.baked.PartialModel model, BlockState state, Direction facing,
		int light) {

		SuperByteBuffer buffer = CachedBuffers.partial(model, state);
		standardKineticRotationTransform(buffer, driver, light);
		return yaw(buffer, facing);
	}

	/**
	 * The same as {@link #spin}, for a part that lives in the block behind this one.
	 *
	 * <p>{@code rotateCentered} turns about the centre of <em>this</em> block, and the cross axle is
	 * drawn a block further back - spinning it that way swings it round in a wide arc instead of
	 * turning it on the spot. Shifting the axle onto this block's centre first, turning, and shifting
	 * it back puts the pivot where the axle actually is.</p>
	 *
	 * <p>Reading order is again the reverse of the code: yaw, then in to the centre, then the turn,
	 * then back out.</p>
	 */
	private static SuperByteBuffer spinBehind(KineticBlockEntity driver,
		dev.engine_room.flywheel.lib.model.baked.PartialModel model, BlockState state, Direction facing,
		int light) {

		Vec3i back = facing.getOpposite()
			.getNormal();
		Axis axis = getRotationAxisOf(driver);
		float angle = getAngleForBe(driver, driver.getBlockPos(), axis);

		SuperByteBuffer buffer = CachedBuffers.partial(model, state);
		buffer.translate(back.getX(), back.getY(), back.getZ());
		kineticRotationTransform(buffer, driver, axis, angle, light);
		buffer.translate(-back.getX(), -back.getY(), -back.getZ());
		return yaw(buffer, facing);
	}

	/** The half behind, or null while it is still being placed. */
	private static KineticBlockEntity cradleOf(KinetiteCompressorBlockEntity be) {
		if (be.getLevel() == null)
			return null;
		return be.getLevel()
			.getBlockEntity(KinetiteCompressorBlock.cradlePos(be.getBlockPos(), be.getBlockState()))
			instanceof KinetiteCompressorCradleBlockEntity cradle ? cradle : null;
	}

	/**
	 * Yaw a part from the orientation it was drawn in onto the machine's facing.
	 *
	 * <p>These parts are modelled pointing north, so north is the zero and the rest follow from it -
	 * south 180, west 90, east 270. Catnip's {@code partialFacing} assumes a different zero, which is
	 * why using it put every moving part on backwards.</p>
	 */
	private static SuperByteBuffer yaw(SuperByteBuffer buffer, Direction facing) {
		return buffer.rotateCentered((float) Math.toRadians(180 - facing.toYRot()), Direction.UP);
	}

	private static SuperByteBuffer aimed(dev.engine_room.flywheel.lib.model.baked.PartialModel model,
		BlockState state, Direction facing) {
		return yaw(CachedBuffers.partial(model, state), facing);
	}

	/**
	 * The target on the spinning head and the Kinetite on the ram, so the press reads as two things
	 * meeting rather than one machine humming.
	 */
	private void renderHeldItems(KinetiteCompressorBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay, Direction facing, float offset) {

		Vec3i step = facing.getNormal();
		// Where the two heads face each other, in the model the parts were drawn to.
		renderItem(be.getTargetItem(), ms, buffer, light, overlay,
			.5f + step.getX() * (9.5f / 16), .5f, .5f + step.getZ() * (9.5f / 16));
		renderItem(be.getKinetiteItem(), ms, buffer, light, overlay,
			.5f - step.getX() * (2.5f / 16 - offset), .5f, .5f - step.getZ() * (2.5f / 16 - offset));
	}

	private void renderItem(ItemStack stack, PoseStack ms, MultiBufferSource buffer, int light, int overlay,
		float x, float y, float z) {
		if (stack.isEmpty())
			return;
		ms.pushPose();
		ms.translate(x, y, z);
		ms.scale(.5f, .5f, .5f);
		Minecraft.getInstance()
			.getItemRenderer()
			.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, ms, buffer, null, 0);
		ms.popPose();
	}

	/** Nothing to draw on the kinetic pass - every moving part is handled above. */
	@Override
	protected BlockState getRenderedBlockState(KinetiteCompressorBlockEntity be) {
		return be.getBlockState();
	}
}
