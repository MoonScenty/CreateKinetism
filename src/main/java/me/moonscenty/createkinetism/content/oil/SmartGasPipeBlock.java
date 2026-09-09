package me.moonscenty.createkinetism.content.oil;

import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlockEntity;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Create's Smart Fluid Pipe, carrying a Mekanism chemical instead.
 *
 * <p>A shell, deliberately. Everything about the block half of a pipe - how it lines up when placed,
 * its shape, its blockstate, its model wrapper, waterlogging, the wrench - is Create's and is
 * correct; hand-writing it again only reintroduced bugs. What changes is what flows through it, and
 * that lives in {@link SmartGasPipeBlockEntity}.</p>
 */
public class SmartGasPipeBlock extends SmartFluidPipeBlock {

	public SmartGasPipeBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntityType<? extends SmartFluidPipeBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.SMART_GAS_PIPE.get();
	}

	/** What this lines itself up with when placed: gas, not fluid. */
	@Override
	protected boolean prefersConnectionTo(LevelReader reader, BlockPos pos, Direction facing) {
		return reader instanceof BlockAndTintGetter world && GasPipeBlock.canConnectToGas(world, pos, facing);
	}
}
