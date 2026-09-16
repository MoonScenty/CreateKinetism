package me.moonscenty.createkinetism.content.turbine;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Where a Turbine's rotation comes out. It sits under the middle of the turbine's floor - the top of the
 * turbine is its vent - and hands the rotor's turning down a shaft on its underside. See
 * {@link TurbineBearingBlockEntity}.
 */
public class TurbineBearingBlock extends KineticBlock implements IBE<TurbineBearingBlockEntity> {

	public TurbineBearingBlock(Properties properties) {
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

	@Override
	public Class<TurbineBearingBlockEntity> getBlockEntityClass() {
		return TurbineBearingBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends TurbineBearingBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.TURBINE_BEARING.get();
	}
}
