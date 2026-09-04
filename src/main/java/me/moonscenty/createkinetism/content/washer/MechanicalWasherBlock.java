package me.moonscenty.createkinetism.content.washer;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mekanism's Chemical Washer as a sealed vessel with an auger down the middle.
 *
 * <p>Driven from below on the Y axis. The vessel fills its own volume, so there is nowhere to put a
 * basin under it and no reason to want one - which leaves the underside as the only face free for a
 * shaft.</p>
 */
public class MechanicalWasherBlock extends KineticBlock implements IBE<MechanicalWasherBlockEntity> {

	public MechanicalWasherBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Direction.Axis getRotationAxis(BlockState state) {
		return Direction.Axis.Y;
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face == Direction.DOWN;
	}

	/** Whatever the vessel is holding comes back when it is broken. */
	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (state.getBlock() == newState.getBlock()) {
			super.onRemove(state, level, pos, newState, movedByPiston);
			return;
		}
		withBlockEntityDo(level, pos,
			be -> be.spillContents()
				.forEach(spilled -> Block.popResource(level, pos, spilled)));
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Override
	public Class<MechanicalWasherBlockEntity> getBlockEntityClass() {
		return MechanicalWasherBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends MechanicalWasherBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.MECHANICAL_WASHER.get();
	}
}
