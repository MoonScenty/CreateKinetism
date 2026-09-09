package me.moonscenty.createkinetism.content.evaporation;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.foundation.fluid.SmartFluidTank;

import me.moonscenty.createkinetism.config.CKConfigs;
import me.moonscenty.createkinetism.config.CKMachines;
import me.moonscenty.createkinetism.content.recipe.EvaporatingRecipe;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * Ported from the Steel Tank template, which itself reuses Create's own Fluid Tank - see
 * LICENSE-THIRD-PARTY.md.
 *
 * <p>Mekanism's Thermal Evaporation Plant, collapsed into the same stacking shape as Create's own
 * tank. There is no basin and no operating cycle - whatever is sitting in the feed floor just slowly
 * boils into the next stage of the {@code evaporating} chain (water to brine, brine to lithium, ...)
 * on its own, faster with a heat source under the stack. The fluid held is matched generically
 * against every {@code evaporating} recipe, the same way those recipes used to match against a
 * Basin.</p>
 *
 * <p>Below {@value #MIN_HEIGHT} floors the stack is just an inert tank - too short to set aside a
 * whole floor for the feed and still have anywhere left for the product. At {@value #MIN_HEIGHT} or
 * taller, floor 1 (the controller's own floor) holds the feed and every floor above holds the
 * product, exactly like {@link me.moonscenty.createkinetism.content.boiler.ThermalBoilerTankBlockEntity}
 * splits itself in boiler mode. The plant simply stops producing once the product floors are full -
 * there is no longer a search for some neighbouring tank to dump it into.</p>
 */
public class EvaporationPlantBlockEntity extends FluidTankBlockEntity {

	/** Below this many floors there is nowhere to put a product floor at all. */
	public static final int MIN_HEIGHT = 3;

	/** mB of the held fluid boiled off per tick at 1x. */
	private static final float BASE_RATE = 1f;

	/** Each Blaze Burner under the footprint adds this much to the rate multiplier, summed - Kindled
	 * (heated) counts once, Seething (superheated) counts double. */
	private static final float HEATED_WEIGHT = 1f;
	private static final float SUPERHEATED_WEIGHT = 2f;

	/** Summed weight of every Blaze Burner under the stack's footprint - see {@link #scanHeaters()}. */
	public float heat;
	/** The single hottest heater found under the footprint, for gating a recipe's own requirement. */
	private int heatTier;

	/** Rebuilt whenever the recipe manager changes under us - see {@link #evaporable()}. */
	private Set<net.minecraft.world.level.material.Fluid> evaporable = Set.of();
	private RecipeManager evaporableFrom;

	/** Floor 1's feed, only allocated at {@value #MIN_HEIGHT} floors or taller - see {@link #isActive()}. */
	@Nullable
	private SmartFluidTank inputTank;
	/** Every floor above the feed's, sharing one product tank. */
	@Nullable
	private SmartFluidTank outputTank;

	private float pendingProduct;
	private FluidStack pendingProductFluid = FluidStack.EMPTY;

	public EvaporationPlantBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		setLazyTickRate(10);
	}

	/**
	 * Only fluids the {@code evaporating} chain knows about may be put in.
	 *
	 * <p>A tank that accepts anything is a tank a player fills with lava and then cannot empty: there
	 * is no drain on this block and no way to pour it back out. Refusing at the inlet is the only
	 * place that can be said.</p>
	 *
	 * <p>This is the plain, too-short-to-split tank. At {@value #MIN_HEIGHT} floors or taller the feed
	 * lives in {@link #inputTank} instead, built with the same validator - see {@link #newInputTank}.</p>
	 */
	@Override
	protected SmartFluidTank createInventory() {
		SmartFluidTank tank = new SmartFluidTank(getCapacityMultiplier(), this::onFluidStackChanged);
		tank.setValidator(this::isEvaporable);
		return tank;
	}

	private SmartFluidTank newInputTank(int capacity) {
		SmartFluidTank tank = new SmartFluidTank(capacity, this::onFluidStackChanged);
		tank.setValidator(this::isEvaporable);
		return tank;
	}

	private boolean isEvaporable(FluidStack stack) {
		return !stack.isEmpty() && evaporable().contains(stack.getFluid());
	}

	/**
	 * Every fluid the chain can boil. Cached per block entity rather than statically: the
	 * recipe manager differs between the client and the server, and one static cache would rebuild
	 * itself every time the two took turns asking.
	 */
	private Set<net.minecraft.world.level.material.Fluid> evaporable() {
		if (level == null)
			return Set.of();
		RecipeManager manager = level.getRecipeManager();
		if (manager == evaporableFrom)
			return evaporable;

		Set<net.minecraft.world.level.material.Fluid> fluids = new HashSet<>();
		for (RecipeHolder<EvaporatingRecipe> holder : manager
			.getAllRecipesFor(CKRecipeTypes.EVAPORATING.<RecipeInput, EvaporatingRecipe>getType())) {
			for (SizedFluidIngredient ingredient : holder.value()
				.getFluidIngredients())
				for (FluidStack stack : ingredient.getFluids())
					fluids.add(stack.getFluid());

		}
		evaporable = fluids;
		evaporableFrom = manager;
		return evaporable;
	}

	/**
	 * Ten by default, and a config knob rather than a constant.
	 *
	 * <p>Create's own tank reads its cap from Create's config, which is not ours to write into - a
	 * pack that wants taller plants would otherwise have to raise the limit for every fluid tank in
	 * the game to get at this one.</p>
	 */
	@Override
	public int getMaxLength(Direction.Axis longAxis, int width) {
		if (longAxis == Direction.Axis.Y)
			return maxHeight();
		return getMaxWidth();
	}

	public static int maxHeight() {
		CKMachines machines = CKConfigs.machines();
		return machines == null ? 10 : machines.evaporationPlantMaxHeight.get();
	}

	public void updateConnectivityExternally() {
		updateConnectivity();
	}

	/** Tall enough to give the product its own floor(s) - see {@value #MIN_HEIGHT}. */
	public boolean isActive() {
		return isController() && height >= MIN_HEIGHT;
	}

	/** For the renderer - only the product is worth drawing through the windows, see the class doc. */
	@Nullable
	public SmartFluidTank getOutputTank() {
		return outputTank;
	}

	private int inputCapacity() {
		return width * width * getCapacityMultiplier();
	}

	private int outputCapacity() {
		return width * width * Math.max(0, height - 1) * getCapacityMultiplier();
	}

	/** The plain tank's feed moves into the new floor-1 tank; the plain tank itself goes to empty. */
	private void activate() {
		if (level.isClientSide)
			return;
		FluidStack existing = tankInventory.getFluid()
			.copy();
		inputTank = newInputTank(inputCapacity());
		outputTank = new SmartFluidTank(outputCapacity(), this::onFluidStackChanged);
		if (!existing.isEmpty())
			inputTank.fill(existing, FluidAction.EXECUTE);
		tankInventory.setFluid(FluidStack.EMPTY);
		refreshCapability();
		setChanged();
		sendData();
	}

	/** The feed comes back; the product is lost - there is no floor left to hold it. */
	private void deactivate() {
		if (level.isClientSide)
			return;
		FluidStack feed = inputTank.getFluid()
			.copy();
		inputTank = null;
		outputTank = null;
		applyFluidTankSize(getTotalTankSize());
		if (!feed.isEmpty())
			tankInventory.fill(feed, FluidAction.EXECUTE);
		refreshCapability();
		setChanged();
		sendData();
	}

	private void resizeActiveTanks() {
		inputTank.setCapacity(inputCapacity());
		outputTank.setCapacity(outputCapacity());
		int overflow = inputTank.getFluidAmount() - inputTank.getCapacity();
		if (overflow > 0)
			inputTank.drain(overflow, FluidAction.EXECUTE);
		overflow = outputTank.getFluidAmount() - outputTank.getCapacity();
		if (overflow > 0)
			outputTank.drain(overflow, FluidAction.EXECUTE);
	}

	private void updateEvaporatorState() {
		if (level.isClientSide || !isController())
			return;
		if (height >= MIN_HEIGHT) {
			if (inputTank == null)
				activate();
			else
				resizeActiveTanks();
		} else if (inputTank != null) {
			deactivate();
		}
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (!isController())
			return;
		scanHeaters();
	}

	/**
	 * Every Blaze Burner under the whole footprint, not just the one corner under the controller -
	 * a wide plant can sit on several. Each contributes {@link #HEATED_WEIGHT} or
	 * {@link #SUPERHEATED_WEIGHT} to {@link #heat}, and the hottest one found sets {@link #heatTier}
	 * for gating recipes that need at least a certain tier.
	 */
	private void scanHeaters() {
		float weight = 0;
		int tier = 0;
		for (int xOffset = 0; xOffset < width; xOffset++)
			for (int zOffset = 0; zOffset < width; zOffset++) {
				BlockPos pos = worldPosition.offset(xOffset, -1, zOffset);
				int found = (int) BoilerHeater.findHeat(level, pos, level.getBlockState(pos));
				if (found <= 0)
					continue;
				tier = Math.max(tier, found);
				weight += found >= 2 ? SUPERHEATED_WEIGHT : HEATED_WEIGHT;
			}
		heat = weight;
		heatTier = tier;
	}

	@Override
	public void tick() {
		super.tick();
		if (level.isClientSide || !isController())
			return;
		// Self-healing rather than relying solely on notifyMultiUpdated: a stack already 3+ tall in a
		// save from before the split existed loads with height >= MIN_HEIGHT but no inputTank, since
		// nothing about its structure actually changed to fire that callback.
		updateEvaporatorState();
		if (isActive())
			evaporate();
	}

	private void evaporate() {
		FluidStack held = inputTank.getFluid();
		if (held.isEmpty())
			return;

		RecipeHolder<EvaporatingRecipe> match = findRecipeFor(held);
		if (match == null)
			return;
		EvaporatingRecipe recipe = match.value();

		SizedFluidIngredient ingredient = recipe.getFluidIngredients()
			.getFirst();
		FluidStack result = recipe.getFluidResults()
			.getFirst();
		if (result.isEmpty())
			return;

		// Nowhere to put what this would make, so do not make it - the one failure a player cannot
		// see happening otherwise. The product floors are the only place it can go now.
		if (outputTank.getSpace() <= 0)
			return;

		float multiplier = rateMultiplier(recipe.getRequiredHeat());
		if (multiplier <= 0)
			return;

		float ratio = result.getAmount() / (float) ingredient.amount();
		int consumed = (int) Math.min(BASE_RATE * multiplier, (float) held.getAmount());
		if (consumed <= 0)
			return;

		if (!FluidStack.isSameFluidSameComponents(result, pendingProductFluid))
			pendingProduct = 0;
		pendingProductFluid = result;

		// Ten water to one brine means most ticks produce a fraction of a bucket-unit; the remainder is
		// carried rather than rounded away, which is the only way a 1:10 recipe adds up over time.
		float produced = pendingProduct + consumed * ratio;
		int whole = (int) produced;
		if (whole > 0) {
			int accepted = outputTank.fill(new FluidStack(result.getFluid(), whole), FluidAction.EXECUTE);
			if (accepted <= 0)
				return;
			produced -= accepted;
		}

		pendingProduct = produced;
		inputTank.drain(consumed, FluidAction.EXECUTE);
		setChanged();
	}

	@Nullable
	private RecipeHolder<EvaporatingRecipe> findRecipeFor(FluidStack held) {
		Optional<RecipeHolder<EvaporatingRecipe>> match = level.getRecipeManager()
			.getAllRecipesFor(CKRecipeTypes.EVAPORATING.<RecipeInput, EvaporatingRecipe>getType())
			.stream()
			.filter(holder -> holder.value()
				.getFluidIngredients()
				.stream()
				.anyMatch(ingredient -> ingredient.test(held)))
			.findFirst();
		return match.orElse(null);
	}

	/**
	 * A recipe with no heat requirement runs on daylight alone, the way Mekanism's own Thermal
	 * Evaporation Plant runs on the sun - so it stops overnight unless a Blaze Burner is doing the
	 * heating instead, which does not care what time it is. A recipe that actually requires a heat
	 * tier always needs a real heater regardless of the hour; daylight is not hot enough to substitute
	 * for one. Either way, once a heater qualifies, the multiplier is every heater's summed weight
	 * ({@link #heat}), not just the one that happened to meet the requirement.
	 *
	 * @return the rate multiplier, or 0 if the recipe cannot run right now at all
	 */
	private float rateMultiplier(HeatCondition required) {
		int requiredTier = required == HeatCondition.SUPERHEATED ? 2 : required == HeatCondition.HEATED ? 1 : 0;
		if (requiredTier > 0)
			return heatTier >= requiredTier ? heat : 0;
		if (heat > 0)
			return heat;
		return level.isDay() ? 1f : 0;
	}

	/**
	 * The carried fraction, kept across a save.
	 *
	 * <p>A 1:10 recipe spends most of its life holding a partial unit, and without this a chunk
	 * unloading at the wrong moment silently threw away everything boiled since the last whole one.</p>
	 */
	@Override
	public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(compound, registries, clientPacket);
		if (!isController())
			return;
		if (inputTank != null) {
			compound.put("InputTank", inputTank.writeToNBT(registries, new CompoundTag()));
			compound.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
		}
		if (clientPacket)
			return;
		if (pendingProduct > 0 && !pendingProductFluid.isEmpty()) {
			compound.putFloat("PendingProduct", pendingProduct);
			compound.put("PendingProductFluid", pendingProductFluid.save(registries, new CompoundTag()));
		}
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		if (!isController())
			return;

		if (height >= MIN_HEIGHT && compound.contains("InputTank")) {
			if (inputTank == null)
				inputTank = newInputTank(inputCapacity());
			else
				inputTank.setCapacity(inputCapacity());
			if (outputTank == null)
				outputTank = new SmartFluidTank(outputCapacity(), this::onFluidStackChanged);
			else
				outputTank.setCapacity(outputCapacity());
			inputTank.readFromNBT(registries, compound.getCompound("InputTank"));
			outputTank.readFromNBT(registries, compound.getCompound("OutputTank"));
		} else {
			inputTank = null;
			outputTank = null;
		}

		if (clientPacket)
			return;
		pendingProduct = compound.getFloat("PendingProduct");
		pendingProductFluid = compound.contains("PendingProductFluid")
			? FluidStack.parseOptional(registries, compound.getCompound("PendingProductFluid"))
			: FluidStack.EMPTY;
	}

	@Override
	public EvaporationPlantBlockEntity getControllerBE() {
		if (isController())
			return this;
		BlockEntity be = level.getBlockEntity(controller);
		return be instanceof EvaporationPlantBlockEntity found ? found : null;
	}

	@Override
	public void removeController(boolean keepFluids) {
		if (level.isClientSide)
			return;
		// Whatever the feed/product split was holding does not follow a block that stops being part
		// of the stack - there is no floor left to keep it on.
		inputTank = null;
		outputTank = null;
		updateConnectivity = true;
		if (!keepFluids)
			applyFluidTankSize(1);
		controller = null;
		width = 1;
		height = 1;
		onFluidStackChanged(tankInventory.getFluid());

		BlockState state = getBlockState();
		if (EvaporationPlantBlock.isTank(state)) {
			state = state.setValue(EvaporationPlantBlock.BOTTOM, true);
			state = state.setValue(EvaporationPlantBlock.TOP, true);
			state = state.setValue(EvaporationPlantBlock.SHAPE,
				window ? FluidTankBlock.Shape.WINDOW : FluidTankBlock.Shape.PLAIN);
			getLevel().setBlock(worldPosition, state, 22);
		}

		refreshCapability();
		setChanged();
		sendData();
	}

	@Override
	public void toggleWindows() {
		EvaporationPlantBlockEntity controllerBE = getControllerBE();
		if (controllerBE == null)
			return;
		controllerBE.setWindows(!controllerBE.window);
	}

	public boolean hasWindows() {
		return window;
	}

	public int getLuminosity() {
		return luminosity;
	}

	@Override
	public void setWindows(boolean window) {
		this.window = window;
		for (int yOffset = 0; yOffset < height; yOffset++)
			for (int xOffset = 0; xOffset < width; xOffset++)
				for (int zOffset = 0; zOffset < width; zOffset++) {
					BlockPos pos = worldPosition.offset(xOffset, yOffset, zOffset);
					BlockState blockState = level.getBlockState(pos);
					if (!EvaporationPlantBlock.isTank(blockState))
						continue;

					FluidTankBlock.Shape shape = FluidTankBlock.Shape.PLAIN;
					if (window) {
						if (width == 1)
							shape = FluidTankBlock.Shape.WINDOW;
						if (width == 2)
							shape = xOffset == 0
								? zOffset == 0 ? FluidTankBlock.Shape.WINDOW_NW : FluidTankBlock.Shape.WINDOW_SW
								: zOffset == 0 ? FluidTankBlock.Shape.WINDOW_NE : FluidTankBlock.Shape.WINDOW_SE;
						if (width == 3 && Math.abs(Math.abs(xOffset) - Math.abs(zOffset)) == 1)
							shape = FluidTankBlock.Shape.WINDOW;
					}

					level.setBlock(pos, blockState.setValue(EvaporationPlantBlock.SHAPE, shape), 22);
					level.getChunkSource()
						.getLightEngine()
						.checkBlock(pos);
				}
	}

	@Override
	public void notifyMultiUpdated() {
		BlockState state = getBlockState();
		if (EvaporationPlantBlock.isTank(state)) {
			state = state.setValue(EvaporationPlantBlock.BOTTOM, getController().getY() == getBlockPos().getY());
			state = state.setValue(EvaporationPlantBlock.TOP,
				getController().getY() + height - 1 == getBlockPos().getY());
			level.setBlock(getBlockPos(), state, 6);
		}
		if (isController())
			setWindows(window);
		onFluidStackChanged(tankInventory.getFluid());
		updateEvaporatorState();
		setChanged();
	}

	/**
	 * The base implementation always shows the controller's own tank, which in the active split is
	 * only the feed - goggling a product floor would show the feed even though that floor is sitting
	 * on the product. Show whichever tank the floor being looked at actually belongs to.
	 */
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		EvaporationPlantBlockEntity controller = getControllerBE();
		if (controller == null)
			return false;
		if (!controller.isActive())
			return super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		boolean isFeedFloor = worldPosition.getY() == controller.worldPosition.getY();
		IFluidHandler shown = isFeedFloor ? controller.inputTank : controller.outputTank;
		return containedFluidTooltip(tooltip, isPlayerSneaking, shown);
	}

	public void refreshCapability() {
		fluidCapability = handlerForCapability();
		invalidateCapabilities();
	}

	private IFluidHandler handlerForCapability() {
		if (isController())
			return tankInventory;
		EvaporationPlantBlockEntity controllerBE = getControllerBE();
		return controllerBE != null ? controllerBE.handlerForCapability() : tankInventory;
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<EvaporationPlantBlockEntity> type) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (be, context) -> {
			EvaporationPlantBlockEntity controller = be.getControllerBE();
			if (controller == null)
				return null;
			if (controller.isActive())
				return be.worldPosition.getY() == controller.worldPosition.getY() ? controller.inputTank
					: controller.outputTank;
			if (be.fluidCapability == null)
				be.refreshCapability();
			return be.fluidCapability;
		});
	}
}
