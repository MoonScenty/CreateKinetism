package me.moonscenty.createkinetism.content.steel;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKBlocks;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Create's fluid pipe in steel. It moves the same amount of fluid as a copper pipe; the
 * difference is that it cannot be encased, so a refinery built out of it stays visually distinct
 * from the copper plumbing around it.</p>
 */
public class SteelPipeBlock extends FluidPipeBlock {

	public SteelPipeBlock(Properties properties) {
		super(properties);
	}

	@Override
	public ItemInteractionResult tryEncase(BlockState state, Level level, BlockPos pos, ItemStack heldItem,
		Player player, InteractionHand hand, BlockHitResult ray) {
		return ItemInteractionResult.FAIL;
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		if (tryRemoveBracket(context))
			return InteractionResult.SUCCESS;

		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction clickedFace = context.getClickedFace();
		Direction.Axis axis = getAxis(level, pos, state);

		if (axis == null) {
			// No straight run to infer from, so take the axis of whichever face the click was nearest.
			Vec3 clickLocation = context.getClickLocation()
				.subtract(pos.getX(), pos.getY(), pos.getZ());
			double closest = Float.MAX_VALUE;
			Direction argClosest = Direction.UP;
			for (Direction direction : Iterate.directions) {
				if (clickedFace.getAxis() == direction.getAxis())
					continue;
				double distance = Vec3.atCenterOf(direction.getNormal())
					.distanceToSqr(clickLocation);
				if (distance < closest) {
					closest = distance;
					argClosest = direction;
				}
			}
			axis = argClosest.getAxis();
		}

		if (clickedFace.getAxis() == axis)
			return InteractionResult.PASS;

		if (!level.isClientSide) {
			FluidTransportBehaviour.cacheFlows(level, pos);
			level.setBlockAndUpdate(pos, CKBlocks.STRAIGHT_STEEL_PIPE.getDefaultState()
				.setValue(GlassFluidPipeBlock.AXIS, axis)
				.setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED)));
			FluidTransportBehaviour.loadFlows(level, pos);
		}

		return InteractionResult.SUCCESS;
	}

	@Nullable
	private Direction.Axis getAxis(BlockGetter level, BlockPos pos, BlockState state) {
		return FluidPropagator.getStraightPipeAxis(state);
	}

	@Override
	public BlockEntityType<? extends FluidPipeBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.STEEL_PIPE.get();
	}
}
