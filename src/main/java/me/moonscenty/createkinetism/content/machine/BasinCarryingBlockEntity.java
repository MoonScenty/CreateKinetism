package me.moonscenty.createkinetism.content.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.item.SmartInventory;

import me.moonscenty.createkinetism.content.recipe.VatRecipe;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;

/**
 * A machine that carries its own Create Basin instead of standing over one.
 *
 * <p>Every other machine in this mod works on a real Basin placed below it, which is what buys them
 * piping and hoppers for free. These cannot: the basin has to move with the head, and a block in the
 * world cannot be moved a pixel. So the basin is installed <em>into</em> the machine - held as an
 * item, drawn by the renderer, moved along with everything else - and the inventory and tanks it
 * would have provided are reimplemented here, behind the same capabilities so that pipes, funnels
 * and the Mechanical Arm cannot tell the difference.</p>
 *
 * <p>With no basin installed there is no inventory at all. That is deliberate: an empty cradle should
 * refuse items rather than swallow them.</p>
 *
 * <p>Subclasses supply two things - the recipe type they run, and how they move. Everything else,
 * including the basin bookkeeping and the hand-rolled recipe matching that {@code BasinRecipe.match}
 * would otherwise do against a real basin, lives here.</p>
 */
public abstract class BasinCarryingBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {

	private ItemStack installedBasin = ItemStack.EMPTY;

	public SmartInventory inputInventory;
	public SmartInventory outputInventory;
	public SmartFluidTankBehaviour inputTank;
	public SmartFluidTankBehaviour outputTank;

	protected IItemHandlerModifiable itemCapability;
	protected IFluidHandler fluidCapability;

	public int processingTicks = -1;
	public boolean running;
	private boolean contentsChanged = true;

	public BasinCarryingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		inputInventory = new SmartInventory(9, this);
		inputInventory.whenContentsChanged($ -> contentsChanged = true);
		outputInventory = new SmartInventory(9, this).forbidInsertion()
			.withMaxStackSize(64);
		itemCapability = new CombinedInvWrapper(inputInventory, outputInventory);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);

		// Two of each, matching a real basin, because a purifying recipe may take two fluids.
		inputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 2, 1000, true)
			.whenFluidUpdates(() -> contentsChanged = true);
		outputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, this, 2, 1000, true)
			.whenFluidUpdates(() -> contentsChanged = true)
			.forbidInsertion();
		behaviours.add(inputTank);
		behaviours.add(outputTank);

		fluidCapability = new CombinedTankWrapper(outputTank.getCapability(), inputTank.getCapability());
	}

	public static <T extends BasinCarryingBlockEntity> void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<T> type) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type,
			(be, context) -> be.hasBasin() ? be.itemCapability : null);
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type,
			(be, context) -> be.hasBasin() ? be.fluidCapability : null);
	}

	// ------------------------------------------------------------------ the installed basin

	public boolean hasBasin() {
		return !installedBasin.isEmpty();
	}

	public ItemStack getInstalledBasin() {
		return installedBasin;
	}

	public boolean installBasin(ItemStack stack) {
		if (hasBasin() || !stack.is(AllBlocks.BASIN.asItem()))
			return false;
		installedBasin = stack.copyWithCount(1);
		contentsChanged = true;
		setChanged();
		sendData();
		return true;
	}

	/** Takes the basin back out. Whatever it was holding comes with it. */
	public ItemStack removeBasin() {
		if (!hasBasin())
			return ItemStack.EMPTY;
		ItemStack basin = installedBasin;
		installedBasin = ItemStack.EMPTY;
		stopRunning();
		setChanged();
		sendData();
		return basin;
	}

	/** Everything the machine is holding, for when the basin comes out or the block is broken. */
	public List<ItemStack> spillContents() {
		List<ItemStack> spilled = new ArrayList<>();
		for (SmartInventory inv : List.of(inputInventory, outputInventory))
			for (int slot = 0; slot < inv.getSlots(); slot++) {
				ItemStack stack = inv.getStackInSlot(slot);
				if (!stack.isEmpty())
					spilled.add(stack.copy());
				inv.setStackInSlot(slot, ItemStack.EMPTY);
			}
		// Fluids have nowhere to spill to, so they go with the basin.
		for (SmartFluidTankBehaviour tank : List.of(inputTank, outputTank)) {
			if (tank == null)
				continue;
			IFluidHandler handler = tank.getCapability();
			for (int i = 0; i < handler.getTanks(); i++) {
				FluidStack held = handler.getFluidInTank(i);
				if (!held.isEmpty())
					handler.drain(held.copy(), FluidAction.EXECUTE);
			}
		}
		return spilled;
	}

	// ------------------------------------------------------------------ animation

	/** Which recipe type this machine looks for. The only thing that separates one from another. */
	protected abstract CKRecipeTypes getRecipeType();

	@Override
	protected AABB createRenderBoundingBox() {
		// The basin rides a block above us.
		return new AABB(worldPosition).expandTowards(0, 1.5, 0);
	}

	// ------------------------------------------------------------------ processing

	private Optional<VatRecipe> findRecipe() {
		if (level == null || !hasBasin())
			return Optional.empty();
		for (RecipeHolder<VatRecipe> holder : level.getRecipeManager()
			.getAllRecipesFor(getRecipeType().<RecipeInput, VatRecipe>getType()))
			if (apply(holder.value(), true))
				return Optional.of(holder.value());
		return Optional.empty();
	}

	/**
	 * Test or perform a recipe against our own inventory and tanks.
	 *
	 * <p>This is what {@code BasinRecipe.match} would normally do for us, but that takes a real
	 * {@code BasinBlockEntity}. The rules are the same: every ingredient has to find a slot, an
	 * ingredient listed twice takes two of them, and the results have to fit before anything at all is
	 * consumed.</p>
	 */
	private boolean apply(VatRecipe recipe, boolean simulate) {
		int[] takenFromSlot = new int[inputInventory.getSlots()];

		ingredients:
		for (Ingredient ingredient : recipe.getIngredients()) {
			for (int slot = 0; slot < inputInventory.getSlots(); slot++) {
				ItemStack stack = inputInventory.getStackInSlot(slot);
				if (stack.isEmpty() || takenFromSlot[slot] >= stack.getCount())
					continue;
				if (!ingredient.test(stack))
					continue;
				takenFromSlot[slot]++;
				continue ingredients;
			}
			return false;
		}

		IFluidHandler tanks = inputTank.getCapability();
		int tankCount = tanks.getTanks();
		int[] reservedInTank = new int[tankCount];
		List<FluidStack> toDrain = new ArrayList<>();

		fluids:
		for (SizedFluidIngredient ingredient : recipe.getFluidIngredients()) {
			for (int i = 0; i < tankCount; i++) {
				FluidStack held = tanks.getFluidInTank(i);
				if (held.isEmpty())
					continue;
				int available = held.getAmount() - reservedInTank[i];
				if (available < ingredient.amount())
					continue;
				if (!ingredient.test(held.copyWithAmount(available)))
					continue;
				reservedInTank[i] += ingredient.amount();
				toDrain.add(held.copyWithAmount(ingredient.amount()));
				continue fluids;
			}
			return false;
		}

		List<ItemStack> results = recipe.rollResults(level.getRandom());
		List<FluidStack> fluidResults = recipe.getFluidResults();
		if (!acceptOutputs(results, fluidResults, true))
			return false;
		if (simulate)
			return true;

		for (int slot = 0; slot < takenFromSlot.length; slot++)
			if (takenFromSlot[slot] > 0)
				inputInventory.getStackInSlot(slot)
					.shrink(takenFromSlot[slot]);
		for (FluidStack drained : toDrain)
			tanks.drain(drained, FluidAction.EXECUTE);

		acceptOutputs(results, fluidResults, false);
		inputInventory.setChanged();
		contentsChanged = true;
		return true;
	}

	private boolean acceptOutputs(List<ItemStack> items, List<FluidStack> fluids, boolean simulate) {
		outputInventory.allowInsertion();
		outputTank.allowInsertion();
		boolean fits = acceptItems(items, simulate) && acceptFluids(fluids, simulate);
		outputInventory.forbidInsertion();
		outputTank.forbidInsertion();
		return fits;
	}

	private boolean acceptItems(List<ItemStack> items, boolean simulate) {
		for (ItemStack stack : items) {
			ItemStack remainder = stack.copy();
			for (int slot = 0; slot < outputInventory.getSlots() && !remainder.isEmpty(); slot++)
				remainder = outputInventory.insertItem(slot, remainder, simulate);
			if (!remainder.isEmpty())
				return false;
		}
		return true;
	}

	private boolean acceptFluids(List<FluidStack> fluids, boolean simulate) {
		for (FluidStack stack : fluids) {
			int filled = outputTank.getCapability()
				.fill(stack.copy(), simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
			if (filled < stack.getAmount())
				return false;
		}
		return true;
	}

	private void stopRunning() {
		running = false;
		processingTicks = -1;
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;

		if (getSpeed() == 0 || !hasBasin()) {
			if (running) {
				stopRunning();
				sendData();
			}
			return;
		}

		if (!running) {
			// Only look for work when something actually changed, the way a basin does. Scanning every
			// recipe every tick for nine machines would be felt.
			if (!contentsChanged)
				return;
			contentsChanged = false;
			Optional<VatRecipe> recipe = findRecipe();
			if (recipe.isEmpty())
				return;
			running = true;
			processingTicks = Math.max(recipe.get()
				.getProcessingDuration(), 20);
			sendData();
			return;
		}

		if (processingTicks > 0) {
			processingTicks--;
			return;
		}

		// Looked up again rather than remembered: the contents can change underneath a running machine,
		// and finishing a recipe the basin no longer satisfies would create matter out of nothing.
		findRecipe().ifPresent(recipe -> apply(recipe, false));
		stopRunning();
		contentsChanged = true;
		sendData();
	}

	// ------------------------------------------------------------------ persistence

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putBoolean("Running", running);
		compound.putInt("ProcessingTicks", processingTicks);
		if (hasBasin())
			compound.put("Basin", installedBasin.save(registries));
		compound.put("InputItems", inputInventory.serializeNBT(registries));
		compound.put("OutputItems", outputInventory.serializeNBT(registries));
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		running = compound.getBoolean("Running");
		processingTicks = compound.getInt("ProcessingTicks");
		installedBasin = compound.contains("Basin")
			? ItemStack.parseOptional(registries, compound.getCompound("Basin"))
			: ItemStack.EMPTY;
		if (compound.contains("InputItems"))
			inputInventory.deserializeNBT(registries, compound.getCompound("InputItems"));
		if (compound.contains("OutputItems"))
			outputInventory.deserializeNBT(registries, compound.getCompound("OutputItems"));
		super.read(compound, registries, clientPacket);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		if (!hasBasin())
			return true;
		containedFluidTooltip(tooltip, isPlayerSneaking, fluidCapability);
		return true;
	}
}
