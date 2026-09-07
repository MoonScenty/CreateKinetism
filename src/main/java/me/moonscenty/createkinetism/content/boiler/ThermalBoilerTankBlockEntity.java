package me.moonscenty.createkinetism.content.boiler;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.foundation.fluid.SmartFluidTank;

import me.moonscenty.createkinetism.registry.CKFluids;
import me.moonscenty.createkinetism.registry.CKItems;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.BlockPos;
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
 * A Create Fluid Tank in every respect but one: what it will hold. Water and steam still run
 * Create's own boiler logic unmodified - stack it over a heat source and it drives a Steam Engine
 * exactly like Create's own tank would - and sodium rides along beside them as the reactor's other
 * coolant loop.
 *
 * <p>A {@link BoilerControllerItem} can also flip a stack that is at least three tall into <b>boiler
 * mode</b>: the bottom floor becomes a dedicated feed tank (water or sodium) and every floor above it
 * merges into one product tank (steam or superheated sodium). The two halves are deliberately
 * different sizes - a floor of feed is worth ten of product - so the tank's normal one-size-fits-all
 * capacity is set aside for two tanks of our own while boiler mode is active.</p>
 */
public class ThermalBoilerTankBlockEntity extends FluidTankBlockEntity {

	public static final int MIN_BOILER_HEIGHT = 3;

	private static final int INPUT_CAPACITY_PER_CELL = 1000;
	private static final int OUTPUT_CAPACITY_PER_CELL = 10000;

	public boolean boilerMode;

	/** The feed tank - only meaningful on the controller once {@link #boilerMode} is set. */
	private SmartFluidTank boilerInput;
	/** The merged product tank spanning every floor above the feed. */
	private SmartFluidTank boilerOutput;

	public ThermalBoilerTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	private static boolean isAllowed(FluidStack stack) {
		if (stack.isEmpty())
			return true;
		return stack.getFluid() == Fluids.WATER || stack.getFluid() == CKFluids.STEAM.get()
			.getSource()
			|| stack.getFluid() == CKFluids.SODIUM.get()
				.getSource();
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

	/**
	 * Splits the stack's storage into a one-floor feed tank and a product tank spanning everything
	 * above it. Whatever the tank already held carries over into the feed side if it still fits.
	 */
	public void activateBoiler() {
		if (level.isClientSide || !canBecomeBoiler())
			return;
		boilerMode = true;
		boilerInput = new WhitelistedFluidTank(width * width * INPUT_CAPACITY_PER_CELL, this::onFluidStackChanged);
		boilerOutput = new SmartFluidTank(width * width * (height - 1) * OUTPUT_CAPACITY_PER_CELL,
			this::onFluidStackChanged);

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

		int requiredTier = tierOf(recipe.getRequiredHeat());
		int heaters = countHeatersAtLeast(requiredTier);
		if (heaters <= 0)
			return;

		SizedFluidIngredient ingredient = recipe.getFluidIngredients()
			.getFirst();
		FluidStack result = recipe.getFluidResults()
			.getFirst();
		float ratio = result.getAmount() / (float) ingredient.amount();
		float ratePerHeater = ingredient.amount() / (float) Math.max(1, recipe.getProcessingDuration());

		int consumed = (int) Math.min(ratePerHeater * heaters, (float) held.getAmount());
		if (consumed <= 0)
			return;

		int producible = Math.round(consumed * ratio);
		int space = boilerOutput.getSpace();
		if (producible > space) {
			// The output side is the bottleneck - only make as much as still fits, and burn only
			// that much feed. When space is zero this halts the boiler entirely.
			producible = space;
			consumed = Math.round(producible / ratio);
		}
		if (consumed <= 0 || producible <= 0)
			return;

		boilerInput.drain(consumed, FluidAction.EXECUTE);
		boilerOutput.fill(new FluidStack(result.getFluid(), producible), FluidAction.EXECUTE);
	}

	@Nullable
	private RecipeHolder<ThermalBoilingRecipe> findRecipeFor(FluidStack held) {
		Optional<RecipeHolder<ThermalBoilingRecipe>> match = level.getRecipeManager()
			.getAllRecipesFor(CKRecipeTypes.THERMAL_BOILING.<RecipeInput, ThermalBoilingRecipe>getType())
			.stream()
			.filter(holder -> holder.value()
				.getFluidIngredients()
				.stream()
				.anyMatch(ingredient -> ingredient.test(held)))
			.findFirst();
		return match.orElse(null);
	}

	private static int tierOf(HeatCondition condition) {
		return condition == HeatCondition.SUPERHEATED ? 2 : condition == HeatCondition.HEATED ? 1 : 0;
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
	public FluidStack getBoilerProduct() {
		return boilerMode ? boilerOutput.getFluid() : null;
	}

	public float getBoilerInputFillState() {
		return boilerMode ? (float) boilerInput.getFluidAmount() / boilerInput.getCapacity() : 0;
	}

	public float getBoilerOutputFillState() {
		return boilerMode ? (float) boilerOutput.getFluidAmount() / boilerOutput.getCapacity() : 0;
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
		IFluidHandler shown = isFeedFloor ? controller.boilerInput : controller.boilerOutput;
		return containedFluidTooltip(tooltip, isPlayerSneaking, shown);
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
			tag.put("BoilerOutput", boilerOutput.writeToNBT(registries, new CompoundTag()));
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
		int outputCapacity = width * width * (height - 1) * OUTPUT_CAPACITY_PER_CELL;
		if (boilerInput == null)
			boilerInput = new WhitelistedFluidTank(inputCapacity, this::onFluidStackChanged);
		else
			boilerInput.setCapacity(inputCapacity);
		if (boilerOutput == null)
			boilerOutput = new SmartFluidTank(outputCapacity, this::onFluidStackChanged);
		else
			boilerOutput.setCapacity(outputCapacity);

		boilerInput.readFromNBT(registries, tag.getCompound("BoilerInput"));
		boilerOutput.readFromNBT(registries, tag.getCompound("BoilerOutput"));
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

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<ThermalBoilerTankBlockEntity> type) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (be, context) -> {
			ThermalBoilerTankBlockEntity controller = be.getControllerBE();
			if (controller == null)
				return null;
			if (controller.boilerMode)
				return be.worldPosition.getY() == controller.worldPosition.getY() ? controller.boilerInput
					: controller.boilerOutput;
			if (be.fluidCapability == null)
				be.refreshCapability();
			return be.fluidCapability;
		});
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
