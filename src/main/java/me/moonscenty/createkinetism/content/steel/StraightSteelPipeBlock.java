package me.moonscenty.createkinetism.content.steel;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.HitResult;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>A steel pipe that has been wrenched into a straight run. Wrench it again for the windowed
 * version.</p>
 */
public class StraightSteelPipeBlock extends GlassFluidPipeBlock {

	public StraightSteelPipeBlock(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		if (tryRemoveBracket(context))
			return InteractionResult.SUCCESS;

		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		FluidTransportBehaviour.cacheFlows(level, pos);
		level.setBlockAndUpdate(pos, CKBlocks.STEEL_WINDOW_PIPE.getDefaultState()
			.setValue(GlassFluidPipeBlock.AXIS, state.getValue(AXIS))
			.setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED)));
		FluidTransportBehaviour.loadFlows(level, pos);
		return InteractionResult.SUCCESS;
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
		return CKBlockEntityTypes.STRAIGHT_STEEL_PIPE.get();
	}
}
