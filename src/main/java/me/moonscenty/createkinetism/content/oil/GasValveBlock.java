package me.moonscenty.createkinetism.content.oil;

import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlockEntity;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Create's Fluid Valve, opening and shutting on a chemical instead of a fluid.
 *
 * <p>A shell over Create's block. The handwheel, the quarter turn and the {@code ENABLED} flag are
 * all Create's; what the valve lets past is {@link GasValveBlockEntity}'s.</p>
 */
public class GasValveBlock extends FluidValveBlock {

	public GasValveBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntityType<? extends FluidValveBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.GAS_VALVE.get();
	}

	/** What this lines itself up with when placed: gas, not fluid. The shaft axis stays Create's. */
	@Override
	protected boolean prefersConnectionTo(LevelReader reader, BlockPos pos, Direction facing, boolean shaftAxis) {
		if (shaftAxis)
			return super.prefersConnectionTo(reader, pos, facing, true);
		return reader instanceof BlockAndTintGetter world && GasPipeBlock.canConnectToGas(world, pos, facing);
	}
}
