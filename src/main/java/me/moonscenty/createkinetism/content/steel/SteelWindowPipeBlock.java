package me.moonscenty.createkinetism.content.steel;

import java.util.Map;

import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.HitResult;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>The windowed steel pipe, so you can see what is running through a refinery line. Wrenching it
 * once more takes it back to a plain pipe.</p>
 */
public class SteelWindowPipeBlock extends GlassFluidPipeBlock {

	public SteelWindowPipeBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockState toRegularPipe(LevelAccessor level, BlockPos pos, BlockState state) {
		Direction side = Direction.get(Direction.AxisDirection.POSITIVE, state.getValue(AXIS));
		Map<Direction, BooleanProperty> byDirection = FluidPipeBlock.PROPERTY_BY_DIRECTION;
		return CKBlocks.STEEL_PIPE.get()
			.updateBlockState(CKBlocks.STEEL_PIPE.getDefaultState()
				.setValue(byDirection.get(side), true)
				.setValue(byDirection.get(side.getOpposite()), true), side, null, level, pos);
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
		return ItemRequirement.of(CKBlocks.STEEL_PIPE.getDefaultState(), be);
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
		Player player) {
		return CKBlocks.STEEL_PIPE.asStack();
	}

	@Override
	public BlockEntityType<? extends StraightPipeBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.STEEL_WINDOW_PIPE.get();
	}
}
