package me.moonscenty.createkinetism.content.evaporation;

import static java.lang.Math.abs;

import java.util.HashSet;
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
 * tank. There is no basin and no operating cycle - whatever is sitting inside just slowly boils
 * into the next stage of the {@code evaporating} chain (water to brine, brine to lithium, ...) on
 * its own, faster with a heat source under the stack. The fluid held is matched generically against
 * every {@code evaporating} recipe, the same way those recipes used to match against a Basin.</p>
 *
 * <p>The tank holds one fluid, so the product cannot live in it: it goes to a fluid handler placed
 * against the outside of the stack instead, and the plant only runs while there is one that will
 * take it. That is also what lets a chain be built - a plant boiling water into brine can feed the
 * plant next to it that boils brine into lithium - and what stops a single plant from running its
 * whole chain to the end whether you wanted the intermediate or not.</p>
 */
public class EvaporationPlantBlockEntity extends FluidTankBlockEntity {

	/** mB of the held fluid boiled off per tick with nothing heating the stack from below. */
	private static final float BASE_RATE = 1f;

	/** How far a recipe's declared heat tier multiplies that base rate once it is reached. */
	private static final float HEATED_MULTIPLIER = 8f;
	private static final float SUPERHEATED_MULTIPLIER = 20f;
	/** Some heat, but short of what this particular recipe wants - still better than none. */
	private static final float PARTIAL_MULTIPLIER = 3f;

	/** Summed heat found directly under the stack's footprint - see {@link BoilerHeater}. */
	public float heat;

	/** Rebuilt whenever the recipe manager changes under us - see {@link #evaporable()}. */
	private Set<net.minecraft.world.level.material.Fluid> evaporable = Set.of();
	private RecipeManager evaporableFrom;

	/** Everything but down: a Blaze Burner lives under the stack. */
	private static final Direction[] OUTPUT_SIDES =
		{ Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };

	@Nullable
	private BlockPos outputTarget;

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
	 * <p>Ingredients only. The product leaves through {@link #findOutput}, so nothing this machine
	 * makes ever needs to come back in, and "what may be put in" is exactly "what can be boiled".</p>
	 */
	@Override
	protected SmartFluidTank createInventory() {
		SmartFluidTank tank = new SmartFluidTank(getCapacityMultiplier(), this::onFluidStackChanged);
		tank.setValidator(stack -> !stack.isEmpty() && evaporable().contains(stack.getFluid()));
		return tank;
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

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (!isController())
			return;
		BlockPos below = worldPosition.below();
		heat = Math.max(BoilerHeater.findHeat(level, below, level.getBlockState(below)), 0);
	}

	@Override
	public void tick() {
		super.tick();
		if (level.isClientSide || !isController())
			return;
		evaporate();
	}

	private void evaporate() {
		FluidStack held = tankInventory.getFluid();
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

		// Nowhere to put what this would make, so do not make it. Boiling the input away into a full
		// pipe would destroy it, and that is the one failure a player cannot see happening.
		IFluidHandler target = findOutput(result);
		if (target == null)
			return;

		float ratio = result.getAmount() / (float) ingredient.amount();
		int consumed = (int) Math.min(BASE_RATE * heatMultiplier(heat, recipe.getRequiredHeat()),
			(float) held.getAmount());
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
			int accepted = target.fill(new FluidStack(result.getFluid(), whole), FluidAction.EXECUTE);
			if (accepted <= 0)
				return;
			produced -= accepted;
		}

		pendingProduct = produced;
		tankInventory.drain(consumed, FluidAction.EXECUTE);
		setChanged();
	}

	/**
	 * A fluid handler against the outside of the stack that will take the product.
	 *
	 * <p>Every side of the multiblock is searched except the bottom, which is where a Blaze Burner
	 * goes. Positions inside the stack are skipped outright, so a plant can never pick itself - the
	 * cheap bounds test also means the expensive capability lookup only runs for blocks that really
	 * are outside.</p>
	 *
	 * <p>The last one found is remembered and tried first, because the answer almost never changes
	 * between ticks and the scan is the only part of this that is not free.</p>
	 */
	@Nullable
	private IFluidHandler findOutput(FluidStack product) {
		if (outputTarget != null) {
			IFluidHandler cached = outputAt(outputTarget, product);
			if (cached != null)
				return cached;
			outputTarget = null;
		}

		BlockPos origin = getController();
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++)
				for (int z = 0; z < width; z++)
					for (Direction side : OUTPUT_SIDES) {
						BlockPos neighbour = origin.offset(x, y, z)
							.relative(side);
						if (isInsideStack(neighbour))
							continue;
						IFluidHandler handler = outputAt(neighbour, product);
						if (handler != null) {
							outputTarget = neighbour;
							return handler;
						}
					}
		return null;
	}

	/** The handler at that position, but only if it would actually take some of the product. */
	@Nullable
	private IFluidHandler outputAt(BlockPos pos, FluidStack product) {
		if (isInsideStack(pos))
			return null;
		IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
		if (handler == null)
			return null;
		return handler.fill(new FluidStack(product.getFluid(), 1), FluidAction.SIMULATE) > 0 ? handler : null;
	}

	private boolean isInsideStack(BlockPos pos) {
		BlockPos origin = getController();
		return pos.getX() >= origin.getX() && pos.getX() < origin.getX() + width
			&& pos.getY() >= origin.getY() && pos.getY() < origin.getY() + height
			&& pos.getZ() >= origin.getZ() && pos.getZ() < origin.getZ() + width;
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

	private static float heatMultiplier(float heatFound, HeatCondition required) {
		int requiredTier = required == HeatCondition.SUPERHEATED ? 2 : required == HeatCondition.HEATED ? 1 : 0;
		if (heatFound <= 0)
			return 1f;
		if (requiredTier > 0 && heatFound >= requiredTier)
			return requiredTier == 1 ? HEATED_MULTIPLIER : SUPERHEATED_MULTIPLIER;
		return PARTIAL_MULTIPLIER;
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
		if (!isController() || clientPacket)
			return;
		if (pendingProduct > 0 && !pendingProductFluid.isEmpty()) {
			compound.putFloat("PendingProduct", pendingProduct);
			compound.put("PendingProductFluid", pendingProductFluid.save(registries, new CompoundTag()));
		}
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);
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
						if (width == 3 && abs(abs(xOffset) - abs(zOffset)) == 1)
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
		setChanged();
	}

	/** Not a Create boiler - the heat it gathers feeds evaporation instead. */
	@Override
	public void updateBoilerState() {
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
			if (be.fluidCapability == null)
				be.refreshCapability();
			return be.fluidCapability;
		});
	}
}
