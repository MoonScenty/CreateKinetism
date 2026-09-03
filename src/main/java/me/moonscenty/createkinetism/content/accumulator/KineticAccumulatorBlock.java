package me.moonscenty.createkinetism.content.accumulator;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;


/**
 * Sits in a shaft line like an encased shaft and buffers the network's stress.
 *
 * <p>Mekanism stores power in an Energy Cube. Create has no notion of stored power at all - stress
 * is an instantaneous load, not a quantity - so this is the closest honest analogue: it soaks up
 * spare capacity while the network is idle and gives it back when the network would otherwise
 * overstress and shut down.</p>
 */
public class KineticAccumulatorBlock extends RotatedPillarKineticBlock implements IBE<KineticAccumulatorBlockEntity> {

	public KineticAccumulatorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(AXIS);
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() == state.getValue(AXIS);
	}

	/** Comparator strength tracks the stored charge, so redstone can react to a flat battery. */
	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return getBlockEntityOptional(level, pos).map(KineticAccumulatorBlockEntity::getComparatorOutput)
			.orElse(0);
	}

	/**
	 * Set a Disassembler on top to wind it, take it back with an empty hand. There is no GUI on
	 * purpose: the accumulator has one slot and one job, and a tool resting on a block reads as
	 * charging without anything having to say so.
	 */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
		Player player, InteractionHand hand, BlockHitResult hitResult) {

		return onBlockEntityUseItemOn(level, pos, be -> {
			if (stack.isEmpty()) {
				ItemStack held = be.chargingInv.extractItem(0, Integer.MAX_VALUE, level.isClientSide);
				if (held.isEmpty())
					return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
				if (!level.isClientSide)
					player.getInventory()
						.placeItemBackInInventory(held);
				return ItemInteractionResult.SUCCESS;
			}

			// Simulating on the client keeps both sides agreeing about whether anything happened.
			ItemStack remainder = be.chargingInv.insertItem(0, stack, level.isClientSide);
			if (remainder.getCount() == stack.getCount())
				return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
			if (!level.isClientSide)
				player.setItemInHand(hand, remainder);
			return ItemInteractionResult.SUCCESS;
		});
	}

	@Override
	public Class<KineticAccumulatorBlockEntity> getBlockEntityClass() {
		return KineticAccumulatorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends KineticAccumulatorBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.ACCUMULATOR.get();
	}
}
