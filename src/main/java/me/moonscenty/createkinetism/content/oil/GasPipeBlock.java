package me.moonscenty.createkinetism.content.oil;

import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import mekanism.common.capabilities.Capabilities;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Create's Fluid Pipe, carrying a Mekanism chemical instead.
 *
 * <p>A shell over Create's block, like the rest of the gas line. Connections, the model set's
 * "never fewer than two ends" rule, waterlogging, wrenching to a window - all Create's, and all
 * correct. {@link GasPipeBlockEntity} is what makes a gas move through it.</p>
 */
public class GasPipeBlock extends FluidPipeBlock {

	public GasPipeBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntityType<? extends FluidPipeBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.GAS_PIPE.get();
	}

	/**
	 * Create's own connection pass, with one call swapped.
	 *
	 * <p>{@code FluidPipeBlock.canConnectTo} is static, so there is no way to teach it about
	 * chemicals; the method that calls it is not, so this is the smallest place the swap fits. The
	 * shape of the rule is Create's and matters - a lone connection is completed to a straight run
	 * and no connections falls back to an axis, because the model set has nothing to draw for a
	 * segment with fewer than two ends.</p>
	 */
	@Override
	public BlockState updateBlockState(BlockState state, Direction preferredDirection,
		@Nullable Direction ignore, BlockAndTintGetter world, BlockPos pos) {

		BracketedBlockEntityBehaviour bracket =
			BlockEntityBehaviour.get(world, pos, BracketedBlockEntityBehaviour.TYPE);
		if (bracket != null && bracket.isBracketPresent())
			return state;

		BlockState previous = state;
		int previousSides = 0;
		for (Direction d : Iterate.directions)
			if (previous.getValue(PROPERTY_BY_DIRECTION.get(d)))
				previousSides++;

		for (Direction d : Iterate.directions)
			if (d != ignore)
				state = state.setValue(PROPERTY_BY_DIRECTION.get(d), canConnectToGas(world, pos, d));

		Direction only = null;
		for (Direction d : Iterate.directions) {
			if (!isOpenAt(state, d))
				continue;
			if (only != null)
				return state;
			only = d;
		}

		if (only != null)
			return state.setValue(PROPERTY_BY_DIRECTION.get(only.getOpposite()), true);
		if (previousSides == 2)
			return previous;
		return state.setValue(PROPERTY_BY_DIRECTION.get(preferredDirection), true)
			.setValue(PROPERTY_BY_DIRECTION.get(preferredDirection.getOpposite()), true);
	}

	/**
	 * Anything that would take a chemical through that face.
	 *
	 * <p>Every block of the gas line offers one, so this covers pipe-to-pipe as well as pipe-to-
	 * machine. A capability query needs a real level; a bake-time call that has none answers no, the
	 * same way Create's own fluid check does.</p>
	 */
	public static boolean canConnectToGas(BlockAndTintGetter world, BlockPos pos, Direction side) {
		return world instanceof Level level && level.getCapability(Capabilities.CHEMICAL.block(),
			pos.relative(side), side.getOpposite()) != null;
	}
}
