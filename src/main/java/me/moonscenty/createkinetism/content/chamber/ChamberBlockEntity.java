package me.moonscenty.createkinetism.content.chamber;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;

import me.moonscenty.createkinetism.content.recipe.ChamberRecipe;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

/**
 * Processing loop for {@link ChamberBlock}, structurally identical to Create's
 * {@code MillstoneBlockEntity}.
 *
 * <p>The Mekanism half is in the numbers rather than the shape: throughput is a pure function of
 * the rotational speed feeding the machine ({@link #getProcessingSpeed()}), so an over-clocked
 * network runs an ore chain faster in exactly the way a stack of Speed Upgrades would.</p>
 */
public class ChamberBlockEntity extends KineticBlockEntity implements Clearable {

	public ItemStackHandler inputInv;
	public ItemStackHandler outputInv;
	public int timer;


	/** Reaches every input slot. Used by hand interaction and by anything that asks with no side. */
	public IItemHandler capability;
	/** Insertion from any face except the one the block points at. Null on single-slot chambers. */
	private IItemHandler primaryCapability;
	/** Insertion from the face the block points at. Null on single-slot chambers. */
	private IItemHandler secondaryCapability;

	private ChamberRecipe lastRecipe;

	public ChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		int slots = state.getBlock() instanceof ChamberBlock chamber ? chamber.getInputSlots() : 1;
		inputInv = new ItemStackHandler(slots);
		outputInv = new ItemStackHandler(9);
		capability = new ChamberInventoryHandler(inputInv);
		if (slots > 1) {
			primaryCapability = new ChamberInventoryHandler(new RangedWrapper(inputInv, 0, 1));
			secondaryCapability = new ChamberInventoryHandler(new RangedWrapper(inputInv, 1, 2));
		}
	}

	/**
	 * Routes insertion by face, which is how a chamber with two different inputs stays unambiguous
	 * without a GUI. Extraction works the same from every face, because there is only one output.
	 */
	public IItemHandler getItemHandler(@Nullable Direction side) {
		if (side == null || secondaryCapability == null)
			return capability;
		return side == DualInputChamberBlock.getSecondaryInputSide(getBlockState()) ? secondaryCapability
			: primaryCapability;
	}

	public CKRecipeTypes getRecipeType() {
		return getBlockState().getBlock() instanceof ChamberBlock chamber ? chamber.getRecipeType()
			: CKRecipeTypes.ENRICHING;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(new DirectBeltInputBehaviour(this));
		super.addBehaviours(behaviours);
	}

	@Override
	public void tick() {
		super.tick();

		if (getSpeed() == 0)
			return;
		for (int i = 0; i < outputInv.getSlots(); i++)
			if (outputInv.getStackInSlot(i)
				.getCount() == outputInv.getSlotLimit(i))
				return;

		if (timer > 0) {
			timer -= getProcessingSpeed();

			if (level.isClientSide) {
				spawnParticles();
				return;
			}
			if (timer <= 0)
				process();
			return;
		}

		if (isInputEmpty())
			return;

		RecipeWrapper inventoryIn = new RecipeWrapper(inputInv);
		if (lastRecipe == null || !lastRecipe.matches(inventoryIn, level)) {
			Optional<RecipeHolder<ChamberRecipe>> recipe = getRecipeType().find(inventoryIn, level);
			if (recipe.isEmpty()) {
				timer = 100;
				sendData();
			} else {
				lastRecipe = recipe.get()
					.value();
				timer = lastRecipe.getProcessingDuration();
				sendData();
			}
			return;
		}

		timer = lastRecipe.getProcessingDuration();
		sendData();
	}


	private void process() {
		RecipeWrapper inventoryIn = new RecipeWrapper(inputInv);

		if (lastRecipe == null || !lastRecipe.matches(inventoryIn, level)) {
			Optional<RecipeHolder<ChamberRecipe>> recipe = getRecipeType().find(inventoryIn, level);
			if (recipe.isEmpty())
				return;
			lastRecipe = recipe.get()
				.value();
		}

		// The recipe tells us how much to take out of each slot; an ingredient listed N times costs
		// N items, the way a Mekanism machine consumes a fixed amount per operation.
		int[] consumed = lastRecipe.resolve(inventoryIn);
		if (consumed == null)
			return;

		for (int slot = 0; slot < inputInv.getSlots(); slot++) {
			int count = consumed[slot];
			if (count <= 0)
				continue;
			ItemStack stackInSlot = inputInv.getStackInSlot(slot);
			ItemStack craftingRemainingItem = stackInSlot.getCraftingRemainingItem();
			stackInSlot.shrink(count);
			inputInv.setStackInSlot(slot, stackInSlot);
			if (!craftingRemainingItem.isEmpty())
				ItemHandlerHelper.insertItemStacked(outputInv, craftingRemainingItem.copyWithCount(count), false);
		}

		lastRecipe.rollResults(level.random)
			.forEach(stack -> ItemHandlerHelper.insertItemStacked(outputInv, stack, false));

		sendData();
		setChanged();
	}

	private boolean isInputEmpty() {
		for (int slot = 0; slot < inputInv.getSlots(); slot++)
			if (!inputInv.getStackInSlot(slot)
				.isEmpty())
				return false;
		return true;
	}

	public int getProcessingSpeed() {
		return Mth.clamp((int) Math.abs(getSpeed() / 16f), 1, 512);
	}

	public void spawnParticles() {
		ItemStack stackInSlot = inputInv.getStackInSlot(0);
		if (stackInSlot.isEmpty())
			return;

		ItemParticleOption data = new ItemParticleOption(ParticleTypes.ITEM, stackInSlot);
		float angle = level.random.nextFloat() * 360;
		Vec3 offset = new Vec3(0, 0, 0.5f);
		offset = VecHelper.rotate(offset, angle, Axis.Y);
		Vec3 target = VecHelper.rotate(offset, getSpeed() > 0 ? 25 : -25, Axis.Y);

		Vec3 center = offset.add(VecHelper.getCenterOf(worldPosition));
		target = VecHelper.offsetRandomly(target.subtract(offset), level.random, 1 / 128f);
		level.addParticle(data, center.x, center.y, center.z, target.x, target.y, target.z);
	}

	@Override
	public void invalidate() {
		super.invalidate();
		invalidateCapabilities();
	}

	@Override
	public void clearContent() {
		for (int slot = 0; slot < inputInv.getSlots(); slot++)
			inputInv.setStackInSlot(slot, ItemStack.EMPTY);
		for (int slot = 0; slot < outputInv.getSlots(); slot++)
			outputInv.setStackInSlot(slot, ItemStack.EMPTY);
	}

	@Override
	public void destroy() {
		super.destroy();
		ItemHelper.dropContents(level, worldPosition, inputInv);
		ItemHelper.dropContents(level, worldPosition, outputInv);
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putInt("Timer", timer);
		compound.put("InputInventory", inputInv.serializeNBT(registries));
		compound.put("OutputInventory", outputInv.serializeNBT(registries));
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		timer = compound.getInt("Timer");
		inputInv.deserializeNBT(registries, compound.getCompound("InputInventory"));
		outputInv.deserializeNBT(registries, compound.getCompound("OutputInventory"));
		super.read(compound, registries, clientPacket);
	}

	/**
	 * Accept an incoming stack if any recipe for this machine mentions it.
	 *
	 * <p>The Millstone can be stricter and test the whole inventory, because one item is always
	 * enough to start it. A chamber may be waiting on eight cobblestone and a dust, so it has to
	 * accept items that do not complete a recipe on their own - otherwise it could never be
	 * filled.</p>
	 */
	private boolean canProcess(ItemStack stack) {
		if (level == null)
			return false;
		for (RecipeHolder<ChamberRecipe> holder : level.getRecipeManager()
			.getAllRecipesFor(getRecipeType().<RecipeInput, ChamberRecipe>getType()))
			for (Ingredient ingredient : holder.value()
				.getIngredients())
				if (ingredient.test(stack))
					return true;
		return false;
	}

	private class ChamberInventoryHandler extends CombinedInvWrapper {

		public ChamberInventoryHandler(IItemHandlerModifiable inputView) {
			super(inputView, outputInv);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			if (outputInv == getHandlerFromIndex(getIndexForSlot(slot)))
				return false;
			return canProcess(stack) && super.isItemValid(slot, stack);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (outputInv == getHandlerFromIndex(getIndexForSlot(slot)))
				return stack;
			if (!isItemValid(slot, stack))
				return stack;
			return super.insertItem(slot, stack, simulate);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			// Only finished goods leave the machine; the input slots are write-only from outside.
			if (outputInv != getHandlerFromIndex(getIndexForSlot(slot)))
				return ItemStack.EMPTY;
			return super.extractItem(slot, amount, simulate);
		}
	}
}
