package me.moonscenty.createkinetism.content.accumulator;

import java.util.function.Predicate;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;

import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mekanism's Energy Cube, as close as Create's stress model honestly allows.
 *
 * <p>A shaft runs through the sides and charges it: while that shaft turns, the block pays its
 * configured stress impact into that network every tick and banks what it buys. A redstone signal
 * spends the bank back out through a large cogwheel set on top, at whatever speed its dial is set
 * to - and that cogwheel is a network of its own, so nothing flows straight through.</p>
 *
 * <p>It borrows the Speed Controller's shape because that is the shape of a block with a shaft below
 * and a cog above, but it is not one and does not extend it. What it does borrow is the one thing
 * only Create can grant - see
 * {@link me.moonscenty.createkinetism.mixin.RotationPropagatorMixin}.</p>
 */
public class KineticAccumulatorBlock extends HorizontalAxisKineticBlock
	implements IBE<KineticAccumulatorBlockEntity> {

	private static final int placementHelperId = PlacementHelpers.register(new CogPlacementHelper());

	public KineticAccumulatorBlock(Properties properties) {
		super(properties);
	}

	/** The Speed Controller's shape: this block is the same size and sits the same way. */
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return AllShapes.SPEED_CONTROLLER;
	}

	/**
	 * Line the shaft up with a cogwheel that is already there. Placing under an existing large cog
	 * should just work rather than needing the block turned by hand afterwards.
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState above = context.getLevel()
			.getBlockState(context.getClickedPos()
				.above());
		if (ICogWheel.isLargeCog(above) && above.getValue(CogWheelBlock.AXIS)
			.isHorizontal())
			return defaultBlockState().setValue(HORIZONTAL_AXIS,
				above.getValue(CogWheelBlock.AXIS) == Axis.X ? Axis.Z : Axis.X);
		return super.getStateForPlacement(context);
	}

	/**
	 * Where a large cogwheel goes, and at what angle.
	 *
	 * <p>Copied from Create's Speed Controller rather than inherited, because it is a plain
	 * {@link IPlacementHelper} keyed on the block it belongs to. Without one the ghost preview never
	 * appears and the cog goes down wherever the crosshair happened to be pointing.</p>
	 */
	private static class CogPlacementHelper implements IPlacementHelper {

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return ((Predicate<ItemStack>) ICogWheel::isLargeCogItem).and(ICogWheel::isDedicatedCogItem);
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return state -> state.getBlock() instanceof KineticAccumulatorBlock;
		}

		@Override
		public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
			BlockHitResult ray) {

			BlockPos newPos = pos.above();
			if (!world.getBlockState(newPos)
				.canBeReplaced())
				return PlacementOffset.fail();

			// Crosswise to the shaft, which is the only angle the accumulator can drive.
			Axis newAxis = state.getValue(HORIZONTAL_AXIS) == Axis.X ? Axis.Z : Axis.X;
			if (!CogWheelBlock.isValidCogwheelPosition(true, world, newPos, newAxis))
				return PlacementOffset.fail();

			return PlacementOffset.success(newPos, s -> s.setValue(CogWheelBlock.AXIS, newAxis));
		}
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

		// A large cogwheel in hand means the player is building the output, not the tool slot.
		IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
		if (helper.matchesItem(stack))
			return helper.getOffset(player, level, state, pos, hitResult)
				.placeInWorld(level, (BlockItem) stack.getItem(), player, hand, hitResult);

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
