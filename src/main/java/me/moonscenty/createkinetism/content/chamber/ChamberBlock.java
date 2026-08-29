package me.moonscenty.createkinetism.content.chamber;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import org.jetbrains.annotations.Nullable;

/**
 * The standalone kinetic machine chassis, lifted almost verbatim from Create's
 * {@code MillstoneBlock}: driven from below or by an adjacent cogwheel, fed from above (belts,
 * chutes, dropped items), emptied by hand or by extracting from any face.
 *
 * <p>Every "no fluids involved" Mekanism machine is an instance of this block; the only thing that
 * differs between them is the recipe type and how many input slots they expose.</p>
 */
public class ChamberBlock extends KineticBlock implements IBE<ChamberBlockEntity>, ICogWheel {

	private final CKRecipeTypes recipeType;
	private final int inputSlots;

	public ChamberBlock(Properties properties, CKRecipeTypes recipeType, int inputSlots) {
		super(properties);
		this.recipeType = recipeType;
		this.inputSlots = inputSlots;
	}

	public CKRecipeTypes getRecipeType() {
		return recipeType;
	}

	public int getInputSlots() {
		return inputSlots;
	}

	/** Whether the model has a recess for {@code ChamberRenderer} to spin a cogwheel in. */
	public boolean rendersCog() {
		return true;
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face == Direction.DOWN;
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return Axis.Y;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
		Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (!stack.isEmpty())
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (level.isClientSide)
			return ItemInteractionResult.SUCCESS;

		withBlockEntityDo(level, pos, chamber -> {
			boolean emptyOutput = true;
			IItemHandlerModifiable inv = chamber.outputInv;
			for (int slot = 0; slot < inv.getSlots(); slot++) {
				ItemStack stackInSlot = inv.getStackInSlot(slot);
				if (!stackInSlot.isEmpty())
					emptyOutput = false;
				player.getInventory()
					.placeItemBackInInventory(stackInSlot);
				inv.setStackInSlot(slot, ItemStack.EMPTY);
			}

			if (emptyOutput) {
				inv = chamber.inputInv;
				for (int slot = 0; slot < inv.getSlots(); slot++) {
					player.getInventory()
						.placeItemBackInInventory(inv.getStackInSlot(slot));
					inv.setStackInSlot(slot, ItemStack.EMPTY);
				}
			}

			chamber.setChanged();
			chamber.sendData();
		});

		return ItemInteractionResult.SUCCESS;
	}

	@Override
	public void updateEntityAfterFallOn(BlockGetter worldIn, Entity entityIn) {
		super.updateEntityAfterFallOn(worldIn, entityIn);

		if (entityIn.level().isClientSide)
			return;
		if (!(entityIn instanceof ItemEntity itemEntity))
			return;
		if (!entityIn.isAlive())
			return;

		ChamberBlockEntity chamber = null;
		for (BlockPos pos : Iterate.hereAndBelow(entityIn.blockPosition()))
			if (chamber == null)
				chamber = getBlockEntity(worldIn, pos);

		if (chamber == null)
			return;

		IItemHandler capability =
			chamber.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, chamber.getBlockPos(), null);
		if (capability == null)
			return;

		ItemStack remainder = capability.insertItem(0, itemEntity.getItem(), false);
		if (remainder.isEmpty())
			itemEntity.discard();
		if (remainder.getCount() < itemEntity.getItem()
			.getCount())
			itemEntity.setItem(remainder);
	}

	@Override
	public Class<ChamberBlockEntity> getBlockEntityClass() {
		return ChamberBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ChamberBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.CHAMBER.get();
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}
}
