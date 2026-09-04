package me.moonscenty.createkinetism.content.tool;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Disassembler's spinning cog and reciprocating blades, drawn wherever the item itself is drawn
 * - in hand, in the inventory, on the ground - since a {@code builtin/entity} item model runs through
 * the same renderer in every {@link ItemDisplayContext} rather than switching between a baked model
 * and this one.
 *
 * <p>There is no block or level here, unlike every other renderer in this mod: an item has neither,
 * so the moving parts are timed off {@link AnimationTickHolder}'s clock rather than a shaft's speed,
 * and {@link CachedBuffers#partial} is handed a throwaway {@code BlockState} purely because its
 * signature asks for one - these parts do not read anything from it.</p>
 */
public class KineticDisassemblerItemRenderer extends BlockEntityWithoutLevelRenderer {

	private static final BlockState DUMMY_STATE = Blocks.AIR.defaultBlockState();

	/** Pivot for the cog's spin, in Blockbench pixels (7.5, 16, 5.5) converted to block fractions. */
	private static final float PIVOT_X = 7.5f / 16f;
	private static final float PIVOT_Y = 16f / 16f;
	private static final float PIVOT_Z = 5.5f / 16f;

	/** Degrees per tick. One full turn every two seconds. */
	private static final float SPIN_SPEED = 3f;

	/** Ticks per full back-and-forth cycle for the blades. */
	private static final float BLADE_FREQUENCY = 1 / 8f;

	public KineticDisassemblerItemRenderer() {
		this(Minecraft.getInstance()
			.getBlockEntityRenderDispatcher(),
			Minecraft.getInstance()
				.getEntityModels());
	}

	public KineticDisassemblerItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
		super(dispatcher, modelSet);
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
		MultiBufferSource buffer, int packedLight, int packedOverlay) {

		float renderTime = AnimationTickHolder.getRenderTime();

		// Wound but not delivering anything - the same condition mining already checks - means the
		// cog and blades sit still rather than pretending to work.
		boolean powered = KineticDisassemblerItem.effectiveMode(stack) != DisassemblerMode.OFF;

		CachedBuffers.partial(CKPartialModels.KINETIC_DISASSEMBLER_BASE, DUMMY_STATE)
			.light(packedLight)
			.renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));

		float spinDegrees = powered ? (renderTime * SPIN_SPEED) % 360f : 0;
		CachedBuffers.partial(CKPartialModels.KINETIC_DISASSEMBLER_COG, DUMMY_STATE)
			.rotateAround(Axis.XP.rotationDegrees(spinDegrees), PIVOT_X, PIVOT_Y, PIVOT_Z)
			.light(packedLight)
			.renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));

		float cycle = powered ? renderTime * BLADE_FREQUENCY * Mth.TWO_PI : 0;

		// Blade 1: a single axis, north-south, 0.8px either way.
		float blade1 = Mth.sin(cycle) * (0.8f / 16f);
		CachedBuffers.partial(CKPartialModels.KINETIC_DISASSEMBLER_BLADE_1, DUMMY_STATE)
			.translate(0, 0, blade1)
			.light(packedLight)
			.renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));

		// Blades 2 and 3: diagonal, north+up together then south+down together, 0.4px per axis - and
		// exactly out of phase with each other, so together they read as chainsaw teeth alternating.
		float diagonal = Mth.sin(cycle) * (0.4f / 16f);
		CachedBuffers.partial(CKPartialModels.KINETIC_DISASSEMBLER_BLADE_2, DUMMY_STATE)
			.translate(0, -diagonal, diagonal)
			.light(packedLight)
			.renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));

		CachedBuffers.partial(CKPartialModels.KINETIC_DISASSEMBLER_BLADE_3, DUMMY_STATE)
			.translate(0, -diagonal, -diagonal)
			.light(packedLight)
			.renderInto(poseStack, buffer.getBuffer(RenderType.cutoutMipped()));
	}
}
