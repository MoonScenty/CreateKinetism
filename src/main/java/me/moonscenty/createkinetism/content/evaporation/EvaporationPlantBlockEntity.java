package me.moonscenty.createkinetism.content.evaporation;

import static java.lang.Math.abs;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.processing.recipe.HeatCondition;

import me.moonscenty.createkinetism.content.recipe.EvaporatingRecipe;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeHolder;
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
 * <p>Water sitting on top of a matching recipe is drained a little at a time; what would have come
 * out the other end of a Basin cycle accumulates in {@link #pendingProduct} instead, and is only
 * poured back into the tank once the input it came from has fully boiled away - a tank never needs
 * to hold two fluids at once.</p>
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

	private float pendingProduct;
	private FluidStack pendingProductFluid = FluidStack.EMPTY;

	public EvaporationPlantBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		setLazyTickRate(10);
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
		float ratio = result.getAmount() / (float) ingredient.amount();

		int consumed = (int) Math.min(BASE_RATE * heatMultiplier(heat, recipe.getRequiredHeat()),
			(float) held.getAmount());
		if (consumed <= 0)
			return;

		if (result.getFluid() != pendingProductFluid.getFluid())
			pendingProduct = 0;
		pendingProductFluid = result;
		pendingProduct += consumed * ratio;

		tankInventory.drain(consumed, FluidAction.EXECUTE);
		if (tankInventory.getFluid()
			.isEmpty() && pendingProduct >= 1) {
			int whole = (int) pendingProduct;
			tankInventory.fill(new FluidStack(result.getFluid(), whole), FluidAction.EXECUTE);
			pendingProduct -= whole;
		}
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
