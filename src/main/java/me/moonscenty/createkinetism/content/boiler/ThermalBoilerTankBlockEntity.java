package me.moonscenty.createkinetism.content.boiler;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.fluid.SmartFluidTank;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;

import me.moonscenty.createkinetism.foundation.CKLang;
import me.moonscenty.createkinetism.foundation.MekanismFluids;
import me.moonscenty.createkinetism.foundation.SidedChemicalAccess;
import me.moonscenty.createkinetism.registry.CKFluids;
import me.moonscenty.createkinetism.registry.CKItems;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * A Create Fluid Tank in every respect but one: what it will hold. Water still runs Create's own
 * boiler logic unmodified - stack it over a heat source and it drives a Steam Engine exactly like
 * Create's own tank would - and sodium rides along beside it as the reactor's other coolant loop.
 *
 * <p>A {@link BoilerControllerItem} can also flip a stack that is at least three tall into <b>boiler
 * mode</b>: the bottom floor becomes a dedicated feed tank (water or sodium) and every floor above it
 * merges into one product tank. The product is real Mekanism gas - steam is a chemical there, not a
 * liquid, the same as Hydrogen or Oxygen - so it is handed out through a Mekanism chemical capability
 * (see {@link #registerChemicalCapabilities}) rather than stored as a liquid stand-in the way this
 * tank's own fluid inventory works. The two halves are deliberately different sizes - a floor of feed
 * is worth ten of product - so the tank's normal one-size-fits-all capacity is set aside for a fluid
 * tank and a chemical tank of our own while boiler mode is active.</p>
 */
public class ThermalBoilerTankBlockEntity extends FluidTankBlockEntity implements IMekanismChemicalHandler {

	public static final int MIN_BOILER_HEIGHT = 3;

	private static final int INPUT_CAPACITY_PER_CELL = 1000;
	private static final int OUTPUT_CAPACITY_PER_CELL = 10000;

	public boolean boilerMode;

	/** The feed tank - only meaningful on the controller once {@link #boilerMode} is set. */
	private SmartFluidTank boilerInput;
	/** The merged product tank spanning every floor above the feed - a gas, not a liquid. */
	private IChemicalTank boilerOutput;

	public ThermalBoilerTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	private static boolean isAllowed(FluidStack stack) {
		if (stack.isEmpty())
			return true;
		return stack.getFluid() == Fluids.WATER || stack.getFluid() == MekanismFluids.STEAM.get()
			|| stack.getFluid() == MekanismFluids.SODIUM.get();
	}

	@Override
	protected SmartFluidTank createInventory() {
		return new WhitelistedFluidTank(getCapacityMultiplier(), this::onFluidStackChanged);
	}

	public void updateConnectivityExternally() {
		updateConnectivity();
	}

	public boolean canBecomeBoiler() {
		return isController() && !boilerMode && height >= MIN_BOILER_HEIGHT;
	}

	/** Every floor above the feed, in chemical amount rather than the feed side's fluid mB. */
	private long outputCapacity() {
		return (long) width * width * (height - 1) * OUTPUT_CAPACITY_PER_CELL;
	}

	private IChemicalTank newOutputTank(long capacity) {
		return BasicChemicalTank.output(capacity, this);
	}

	/**
	 * Splits the stack's storage into a one-floor feed tank and a product tank spanning everything
	 * above it. Whatever the tank already held carries over into the feed side if it still fits - the
	 * product side never has anything to carry over, since a plain fluid tank could never have held a
	 * gas in the first place.
	 */
	public void activateBoiler() {
		if (level.isClientSide || !canBecomeBoiler())
			return;
		boilerMode = true;
		boilerInput = new WhitelistedFluidTank(width * width * INPUT_CAPACITY_PER_CELL, this::onFluidStackChanged);
		boilerOutput = newOutputTank(outputCapacity());

		FluidStack existing = tankInventory.getFluid();
		if (!existing.isEmpty())
			boilerInput.fill(existing, FluidAction.EXECUTE);
		tankInventory.setFluid(FluidStack.EMPTY);

		refreshCapability();
		setChanged();
		sendData();
	}

	/** The feed floor keeps what it was holding - only the product tank is lost. */
	public void deactivateBoiler() {
		if (level.isClientSide || !isController() || !boilerMode)
			return;
		FluidStack feed = boilerInput.getFluid()
			.copy();
		boilerMode = false;
		boilerInput = null;
		boilerOutput = null;
		applyFluidTankSize(getTotalTankSize());
		if (!feed.isEmpty())
			tankInventory.fill(feed, FluidAction.EXECUTE);
		refreshCapability();
		setChanged();
		sendData();
	}

	/**
	 * Called on the controller when {@code breaking} is about to be removed from the stack. A boiler
	 * is all or nothing: every remaining segment falls back to being its own standalone tank rather
	 * than letting Create's usual smart-reform carve a smaller boiler out of the wreckage, and the
	 * controller item is dropped so it isn't lost for good. The feed floor keeps what it was holding;
	 * the product does not survive the collapse.
	 *
	 * <p>{@code breaking} itself is left untouched - {@code removeController} would re-place it via
	 * {@code level.setBlock} moments before the caller's own removal goes through, undoing the break.
	 * </p>
	 */
	public void collapseBoiler(BlockPos breaking) {
		if (level.isClientSide || !boilerMode)
			return;

		int w = width;
		int h = height;
		BlockPos origin = worldPosition;
		FluidStack feed = boilerInput.getFluid()
			.copy();

		boilerMode = false;
		boilerInput = null;
		boilerOutput = null;

		for (int y = 0; y < h; y++)
			for (int x = 0; x < w; x++)
				for (int z = 0; z < w; z++) {
					BlockPos pos = origin.offset(x, y, z);
					if (pos.equals(breaking))
						continue;
					if (level.getBlockEntity(pos) instanceof ThermalBoilerTankBlockEntity part)
						part.removeController(false);
				}

		if (!feed.isEmpty() && level.getBlockEntity(origin) instanceof ThermalBoilerTankBlockEntity feedFloor)
			feedFloor.tankInventory.fill(feed, FluidAction.EXECUTE);

		Containers.dropItemStack(level, origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5,
			new ItemStack(CKItems.BOILER_CONTROLLER.get()));
	}

	@Override
	public void tick() {
		super.tick();
		if (level.isClientSide || !isController() || !boilerMode)
			return;
		boil();
	}

	private void boil() {
		FluidStack held = boilerInput.getFluid();
		if (held.isEmpty())
			return;

		RecipeHolder<ThermalBoilingRecipe> match = findRecipeFor(held);
		if (match == null)
			return;
		ThermalBoilingRecipe recipe = match.value();

		int requiredTier = recipe.getMinimumTier();
		int heaters = countHeatersAtLeast(requiredTier);
		if (heaters <= 0)
			return;

		SizedFluidIngredient ingredient = recipe.getFluidIngredients()
			.getFirst();
		ChemicalStack result = recipe.getChemicalResult();
		if (result.isEmpty())
			return;
		float ratio = result.getAmount() / (float) ingredient.amount();

		int consumed = (int) Math.min((long) recipe.getRate() * heaters, (long) held.getAmount());
		if (consumed <= 0)
			return;

		long producible = Math.round(consumed * ratio);
		long space = boilerOutput.getNeeded();
		if (producible > space) {
			// The output side is the bottleneck - only make as much as still fits, and burn only
			// that much feed. When space is zero this halts the boiler entirely.
			producible = space;
			consumed = Math.round(producible / ratio);
		}
		if (consumed <= 0 || producible <= 0)
			return;

		boilerInput.drain(consumed, FluidAction.EXECUTE);
		boilerOutput.insert(new ChemicalStack(result.getChemicalHolder(), producible), Action.EXECUTE,
			AutomationType.INTERNAL);
	}

	/**
	 * Several recipes can share the same ingredient at different heat tiers - see
	 * {@link ThermalBoilingRecipe}. Among every one that matches what's held, this picks the highest
	 * tier the stack's heaters actually clear right now, so a hotter fire runs the faster recipe
	 * instead of the boiler latching onto whichever recipe the recipe manager happened to list first.
	 */
	@Nullable
	private RecipeHolder<ThermalBoilingRecipe> findRecipeFor(FluidStack held) {
		RecipeHolder<ThermalBoilingRecipe> best = null;
		int bestTier = -1;
		for (RecipeHolder<ThermalBoilingRecipe> holder : level.getRecipeManager()
			.getAllRecipesFor(CKRecipeTypes.THERMAL_BOILING.<RecipeInput, ThermalBoilingRecipe>getType())) {
			ThermalBoilingRecipe recipe = holder.value();
			if (recipe.getFluidIngredients()
				.stream()
				.noneMatch(ingredient -> ingredient.test(held)))
				continue;
			int tier = recipe.getMinimumTier();
			if (tier > bestTier && countHeatersAtLeast(tier) > 0) {
				best = holder;
				bestTier = tier;
			}
		}
		return best;
	}

	/** How many cells directly under the footprint carry at least the given heat tier. */
	private int countHeatersAtLeast(int requiredTier) {
		int count = 0;
		for (int xOffset = 0; xOffset < width; xOffset++)
			for (int zOffset = 0; zOffset < width; zOffset++) {
				BlockPos pos = worldPosition.offset(xOffset, -1, zOffset);
				float found = BoilerHeater.findHeat(level, pos, level.getBlockState(pos));
				if (found >= requiredTier)
					count++;
			}
		return count;
	}

	@Nullable
	public FluidStack getBoilerFeed() {
		return boilerMode ? boilerInput.getFluid() : null;
	}

	@Nullable
	public ChemicalStack getBoilerProduct() {
		return boilerMode ? boilerOutput.getStack() : null;
	}

	public float getBoilerInputFillState() {
		return boilerMode ? (float) boilerInput.getFluidAmount() / boilerInput.getCapacity() : 0;
	}

	public float getBoilerOutputFillState() {
		return boilerMode ? (float) boilerOutput.getStored() / boilerOutput.getCapacity() : 0;
	}

	@Override
	public ThermalBoilerTankBlockEntity getControllerBE() {
		if (isController())
			return this;
		BlockEntity be = level.getBlockEntity(controller);
		return be instanceof ThermalBoilerTankBlockEntity found ? found : null;
	}

	@Override
	public void removeController(boolean keepFluids) {
		if (level.isClientSide)
			return;
		updateConnectivity = true;
		if (!keepFluids)
			applyFluidTankSize(1);
		controller = null;
		width = 1;
		height = 1;
		onFluidStackChanged(tankInventory.getFluid());

		BlockState state = getBlockState();
		if (ThermalBoilerTankBlock.isTank(state)) {
			state = state.setValue(ThermalBoilerTankBlock.BOTTOM, true);
			state = state.setValue(ThermalBoilerTankBlock.TOP, true);
			getLevel().setBlock(worldPosition, state, 22);
		}

		refreshCapability();
		setChanged();
		sendData();
	}

	@Override
	public void notifyMultiUpdated() {
		BlockState state = getBlockState();
		if (ThermalBoilerTankBlock.isTank(state)) {
			state = state.setValue(ThermalBoilerTankBlock.BOTTOM, getController().getY() == getBlockPos().getY());
			state = state.setValue(ThermalBoilerTankBlock.TOP,
				getController().getY() + height - 1 == getBlockPos().getY());
			level.setBlock(getBlockPos(), state, 6);
		}
		onFluidStackChanged(tankInventory.getFluid());
		updateBoilerState();
		setChanged();
	}

	/**
	 * The base implementation always reads the controller's own capability, which in boiler mode is
	 * only the feed tank - goggling an upper floor would show the feed even though that floor is
	 * sitting on the product. Show whichever tank the floor you are actually looking at belongs to.
	 */
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		ThermalBoilerTankBlockEntity controller = getControllerBE();
		if (controller == null)
			return false;
		if (!controller.boilerMode)
			return super.addToGoggleTooltip(tooltip, isPlayerSneaking);

		boolean isFeedFloor = worldPosition.getY() == controller.worldPosition.getY();
		if (isFeedFloor)
			return containedFluidTooltip(tooltip, isPlayerSneaking, controller.boilerInput);
		return addChemicalTooltip(tooltip, isPlayerSneaking, controller.boilerOutput);
	}

	/** {@code containedFluidTooltip}'s equivalent for a Mekanism chemical tank - mirrors GasTurbineBlockEntity. */
	private static boolean addChemicalTooltip(List<Component> tooltip, boolean isPlayerSneaking, IChemicalTank tank) {
		ChemicalStack held = tank.getStack();
		if (held.isEmpty())
			return false;
		CKLang.builder()
			.text("")
			.add(Component.translatable(held.getChemical()
				.getTranslationKey()))
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip);
		CKLang.builder()
			.text(held.getAmount() + " / " + tank.getCapacity() + "mB")
			.style(ChatFormatting.GOLD)
			.forGoggles(tooltip, 1);
		return true;
	}

	/** A stack in boiler mode runs its own steam production - Create's native boiler stays out. */
	@Override
	public void updateBoilerState() {
		if (boilerMode)
			return;
		super.updateBoilerState();
	}

	@Override
	public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if (!isController())
			return;
		tag.putBoolean("BoilerMode", boilerMode);
		if (boilerMode) {
			tag.put("BoilerInput", boilerInput.writeToNBT(registries, new CompoundTag()));
			tag.put("BoilerOutput", boilerOutput.serializeNBT(registries));
		}
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		if (!isController())
			return;
		boilerMode = tag.getBoolean("BoilerMode");
		if (!boilerMode)
			return;

		int inputCapacity = width * width * INPUT_CAPACITY_PER_CELL;
		long outputCapacity = outputCapacity();
		if (boilerInput == null)
			boilerInput = new WhitelistedFluidTank(inputCapacity, this::onFluidStackChanged);
		else
			boilerInput.setCapacity(inputCapacity);
		// A chemical tank's capacity is fixed at construction - resizing means replacing it outright,
		// which is fine here since the NBT read just below is about to overwrite its contents anyway.
		if (boilerOutput == null || boilerOutput.getCapacity() != outputCapacity)
			boilerOutput = newOutputTank(outputCapacity);

		boilerInput.readFromNBT(registries, tag.getCompound("BoilerInput"));
		boilerOutput.deserializeNBT(registries, tag.getCompound("BoilerOutput"));
	}

	public void refreshCapability() {
		fluidCapability = handlerForCapability();
		invalidateCapabilities();
	}

	private IFluidHandler handlerForCapability() {
		if (isController())
			return tankInventory;
		ThermalBoilerTankBlockEntity controllerBE = getControllerBE();
		return controllerBE != null ? controllerBE.handlerForCapability() : tankInventory;
	}

	/** In boiler mode, only the feed floor still has a fluid to hand out - the product is a gas now. */
	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<ThermalBoilerTankBlockEntity> type) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (be, context) -> {
			ThermalBoilerTankBlockEntity controller = be.getControllerBE();
			if (controller == null)
				return null;
			if (controller.boilerMode)
				return be.worldPosition.getY() == controller.worldPosition.getY() ? controller.boilerInput : null;
			if (be.fluidCapability == null)
				be.refreshCapability();
			return be.fluidCapability;
		});
	}

	/** The product floors of a boiler expose the gas tank instead - see {@link #registerCapabilities}. */
	public static void registerChemicalCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<ThermalBoilerTankBlockEntity> type) {
		event.registerBlockEntity(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), type, (be, side) -> {
			ThermalBoilerTankBlockEntity controller = be.getControllerBE();
			if (controller == null || !controller.boilerMode)
				return null;
			if (be.worldPosition.getY() == controller.worldPosition.getY())
				return null;
			return new SidedChemicalAccess(controller, side);
		});
	}

	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		return boilerOutput != null ? List.of(boilerOutput) : List.of();
	}

	@Override
	public void onContentsChanged() {
		setChanged();
		sendData();
	}

	private static class WhitelistedFluidTank extends SmartFluidTank {

		WhitelistedFluidTank(int capacity, Consumer<FluidStack> updateCallback) {
			super(capacity, updateCallback);
		}

		@Override
		public boolean isFluidValid(FluidStack stack) {
			return isAllowed(stack);
		}
	}
}
