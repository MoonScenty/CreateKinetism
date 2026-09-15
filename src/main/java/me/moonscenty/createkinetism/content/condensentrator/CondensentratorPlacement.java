package me.moonscenty.createkinetism.content.condensentrator;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlocks;

import me.moonscenty.createkinetism.registry.CKBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Keeps the block under a Mechanical Condensentrator free for the basin it carries there.
 *
 * <p>The basin is drawn a block below the machine, so a heater placed straight against it would sit
 * inside the basin. Instead, placement skips that block in either order: a condensentrator placed
 * right on top of a heater goes one higher, and a heater placed right under a condensentrator goes
 * one lower. If the block past the gap is not free, nothing is placed - better than the two ending up
 * pressed together.</p>
 *
 * <p>Heaters are the blocks that give a basin heat or cold: Blaze Burners (lit or not), the Sodium
 * Burner and the Stray Chiller. Hooked into {@code BlockItem#updatePlacementContext} by
 * {@code BlockItemMixin}, the same hook vanilla scaffolding uses to move its own placement.</p>
 */
public final class CondensentratorPlacement {

	private CondensentratorPlacement() {}

	public static boolean isHeater(BlockState state) {
		return AllBlocks.BLAZE_BURNER.has(state) || AllBlocks.LIT_BLAZE_BURNER.has(state)
			|| CKBlocks.SODIUM_BURNER.has(state) || CKBlocks.STRAY_CHILLER.has(state);
	}

	/**
	 * The context the item should actually place with: unchanged when no gap is needed, moved a block
	 * up or down when it is, and {@code null} - no placement - when the moved spot is taken.
	 */
	@Nullable
	public static BlockPlaceContext adjust(BlockItem item, BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState placing = item.getBlock()
			.defaultBlockState();

		BlockPos target;
		if (CKBlocks.MECHANICAL_CONDENSENTRATOR.has(placing) && isHeater(level.getBlockState(pos.below())))
			target = pos.above();
		else if (isHeater(placing) && CKBlocks.MECHANICAL_CONDENSENTRATOR.has(level.getBlockState(pos.above())))
			target = pos.below();
		else
			return context;

		if (level.isOutsideBuildHeight(target))
			return null;
		BlockPlaceContext moved = BlockPlaceContext.at(context, target, context.getClickedFace());
		// Only a spot that can be placed into keeps its own position; anything else would push the
		// placement on to the block beyond it.
		return moved.replacingClickedOnBlock() ? moved : null;
	}
}
