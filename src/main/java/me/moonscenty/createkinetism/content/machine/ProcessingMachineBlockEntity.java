package me.moonscenty.createkinetism.content.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;

/**
 * A processing machine that owns its inventory and tanks rather than working out of a Create Basin
 * placed below it.
 *
 * <p>Doing without a basin means reimplementing what one would have provided - input and output
 * slots, two input tanks and two output tanks - behind the same capabilities, so that pipes, funnels
 * and the Mechanical Arm cannot tell the difference. The recipe matching here is the hand-rolled
 * equivalent of {@code BasinRecipe.match}, which cannot be reused because it takes a real
 * {@code BasinBlockEntity}.</p>
 *
 * <p>Subclasses supply the recipe type they run, and may gate operation on something of their own -
 * see {@link #canOperate()}.</p>
 */
public abstract class ProcessingMachineBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {

	public SmartInventory inputInventory;
	public SmartInventory outputInventory;
	public SmartFluidTankBehaviour inputTank;
	public SmartFluidTankBehaviour outputTank;

	protected IItemHandlerModifiable itemCapability;
	protected IFluidHandler fluidCapability;

	public int processingTicks = -1;
	public boolean running;
	protected boolean contentsChanged = true;

	public ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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

		// Two of each, matching a real basin, because a recipe may take two fluids.
		inputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 2, 1000, true)
			.whenFluidUpdates(() -> contentsChanged = true);
		outputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, this, 2, 1000, true)
			.whenFluidUpdates(() -> contentsChanged = true)
			.forbidInsertion();
		behaviours.add(inputTank);
		behaviours.add(outputTank);

		fluidCapability = new CombinedTankWrapper(outputTank.getCapability(), inputTank.getCapability());
	}

	public static <T extends ProcessingMachineBlockEntity> void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<T> type) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type,
			(be, context) -> be.canOperate() ? be.itemCapability : null);
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type,
			(be, context) -> be.canOperate() ? be.fluidCapability : null);
	}

	/** Which recipe type this machine looks for. The only thing that separates one from another. */
	protected abstract CKRecipeTypes getRecipeType();

	/**
	 * Whether the machine is in a state to run at all, and to expose an inventory. A machine that
	 * needs something fitted before it can hold anything says so here.
	 */
	protected boolean canOperate() {
		return true;
	}

	/** Everything the machine is holding, for when it is emptied or the block is broken. */
	public List<ItemStack> spillContents() {
		List<ItemStack> spilled = new ArrayList<>();
		for (SmartInventory inv : List.of(inputInventory, outputInventory))
			for (int slot = 0; slot < inv.getSlots(); slot++) {
				ItemStack stack = inv.getStackInSlot(slot);
				if (!stack.isEmpty())
					spilled.add(stack.copy());
				inv.setStackInSlot(slot, ItemStack.EMPTY);
			}
		// Fluids have nowhere to spill to, so they are simply voided.
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

	// ------------------------------------------------------------------ processing

	protected Optional<VatRecipe> findRecipe() {
		if (level == null || !canOperate())
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
	 * <p>The rules are a basin's: every ingredient has to find a slot, an ingredient listed twice
	 * takes two of them, and the results have to fit before anything at all is consumed.</p>
	 */
	protected boolean apply(VatRecipe recipe, boolean simulate) {
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

	protected void stopRunning() {
		running = false;
		processingTicks = -1;
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;

		if (getSpeed() == 0 || !canOperate()) {
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
		// and finishing a recipe the inputs no longer satisfy would create matter out of nothing.
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
		compound.put("InputItems", inputInventory.serializeNBT(registries));
		compound.put("OutputItems", outputInventory.serializeNBT(registries));
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		running = compound.getBoolean("Running");
		processingTicks = compound.getInt("ProcessingTicks");
		if (compound.contains("InputItems"))
			inputInventory.deserializeNBT(registries, compound.getCompound("InputItems"));
		if (compound.contains("OutputItems"))
			outputInventory.deserializeNBT(registries, compound.getCompound("OutputItems"));
		super.read(compound, registries, clientPacket);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		if (!canOperate())
			return true;
		containedFluidTooltip(tooltip, isPlayerSneaking, fluidCapability);
		return true;
	}
}
