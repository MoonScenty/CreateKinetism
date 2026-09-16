package me.moonscenty.createkinetism.content.turbine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * The rotor, drawn from the controller: one {@code turbine_blade/block} - the axle and its four blades -
 * up the middle column on every floor but the top one, which is the condensate tank with nothing
 * turning in it. It turns only while steam is going through.
 *
 * <p>The model is sized for a 3-wide turbine, its blades reaching into the ring of casings around the
 * axle. Wider turbines draw the same model for now.</p>
 *
 * <p>Between the rotor and the tank floor lies a membrane: a plate across the whole footprint at the
 * bottom of the top floor, one {@code turbine_casing/membrane} a cell, with a hole for the axle in the
 * middle cell. It does not turn.</p>
 */
public class TurbineCasingRenderer extends SafeBlockEntityRenderer<TurbineCasingBlockEntity> {

	public TurbineCasingRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public boolean shouldRenderOffScreen(TurbineCasingBlockEntity be) {
		return true;
	}

	@Override
	protected void renderSafe(TurbineCasingBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		if (!be.isController())
			return;
		int width = be.getWidth();
		if (width < 3 || width % 2 == 0 || be.getHeight() < 2)
			return;

		// Create's own rate for a part at this speed: 0.3 degrees a tick per RPM.
		float angle = be.isRunning()
			? AnimationTickHolder.getRenderTime(be.getLevel()) * TurbineCasingBlockEntity.SPEED * 0.3f % 360 : 0;
		int middle = width / 2;

		for (int y = 0; y < be.rotorFloors(); y++)
			CachedBuffers.partial(CKPartialModels.TURBINE_BLADE, be.getBlockState())
				.translate(middle, y, middle)
				.rotateCentered(angle * Mth.DEG_TO_RAD, Direction.UP)
				.light(light)
				.renderInto(ms, buffer.getBuffer(RenderType.cutout()));

		int tankFloor = be.rotorFloors();
		for (int x = 0; x < width; x++)
			for (int z = 0; z < width; z++)
				CachedBuffers.partial(x == middle && z == middle ? CKPartialModels.TURBINE_MEMBRANE_CENTER
					: CKPartialModels.TURBINE_MEMBRANE, be.getBlockState())
					.translate(x, tankFloor, z)
					.light(light)
					.renderInto(ms, buffer.getBuffer(RenderType.cutout()));
	}
}
