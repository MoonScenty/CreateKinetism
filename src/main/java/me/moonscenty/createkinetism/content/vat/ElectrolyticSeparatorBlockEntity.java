package me.moonscenty.createkinetism.content.vat;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;

import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

import me.moonscenty.createkinetism.content.recipe.SeparatingRecipe;

import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Electrolysis splits one fluid into two chemicals, and the two have to go somewhere separate - a
 * Create basin cannot hold a chemical at all.
 *
 * <p>So unlike every other vat, this one's products never enter the basin. The two faces either side
 * of the shaft each want their own chemical container - a Mekanism tube, tank, anything that accepts
 * a chemical - and the first result of the recipe goes to one, the second to the other. Feed it water
 * and hydrogen comes out one side, oxygen the other.</p>
 *
 * <p>That is a hard requirement rather than a convenience: with nowhere to put a product, a container
 * holding the wrong chemical, or one that is simply full, the recipe will not start and nothing is
 * consumed. Inputs are unchanged - the fluid still comes from the basin below, and any <em>item</em>
 * output still goes back into it.</p>
 */
public class ElectrolyticSeparatorBlockEntity extends VatBlockEntity {

	public ElectrolyticSeparatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** The two faces across from each other, perpendicular to the shaft. First result goes to the first. */
	public Couple<Direction> outputSides() {
		Axis axis = getBlockState().hasProperty(ElectrolyticSeparatorBlock.HORIZONTAL_AXIS)
			? getBlockState().getValue(ElectrolyticSeparatorBlock.HORIZONTAL_AXIS)
			: Axis.Z;
		return axis == Axis.Z ? Couple.create(Direction.WEST, Direction.EAST)
			: Couple.create(Direction.NORTH, Direction.SOUTH);
	}

	private IChemicalHandler chemicalHandlerOn(Direction side) {
		if (level == null)
			return null;
		return level.getCapability(mekanism.common.capabilities.Capabilities.CHEMICAL.block(),
			worldPosition.relative(side), side.getOpposite());
	}

	/**
	 * Hand the products out sideways, one per face, all or nothing.
	 *
	 * <p>{@code insertChemical} already refuses a mismatched chemical or one that does not fully fit,
	 * returning the leftover it could not take - anything other than an empty remainder means the
	 * neighbour was the wrong type, full, or missing, so the whole recipe is refused rather than
	 * letting half a product through with nowhere for the remainder to go.</p>
	 */
	private boolean distributeChemicals(List<ChemicalStack> outputs, boolean simulate) {
		Couple<Direction> sides = outputSides();
		for (int i = 0; i < outputs.size(); i++) {
			ChemicalStack output = outputs.get(i);
			if (output.isEmpty())
				continue;
			if (i > 1)
				return false; // only two faces to give to
			IChemicalHandler target = chemicalHandlerOn(i == 0 ? sides.getFirst() : sides.getSecond());
			if (target == null)
				return false;
			Action action = simulate ? Action.SIMULATE : Action.EXECUTE;
			if (!target.insertChemical(output.copy(), action)
				.isEmpty())
				return false;
		}
		return true;
	}

	/**
	 * Create's {@code BasinRecipe.apply}, with the one line that hands fluids to the basin replaced by
	 * {@link #distributeChemicals}. Everything else - the simulate-then-commit pass, how ingredients
	 * are matched against slots, the tank refresh - is Create's and is kept deliberately identical, so
	 * this machine consumes its input exactly like every other basin machine does.
	 *
	 * @param test when true nothing is consumed; used for the "can this recipe run" check
	 */
	private boolean runRecipe(BasinBlockEntity basin, Recipe<?> recipe, boolean test) {
		if (level == null)
			return false;

		IItemHandler availableItems =
			level.getCapability(Capabilities.ItemHandler.BLOCK, basin.getBlockPos(), null);
		IFluidHandler availableFluids =
			level.getCapability(Capabilities.FluidHandler.BLOCK, basin.getBlockPos(), null);
		if (availableItems == null || availableFluids == null)
			return false;

		boolean isBasinRecipe = recipe instanceof BasinRecipe;
		// Create reads this off the basin itself, but that accessor is package-private; the burner
		// under the basin is where the value comes from either way.
		if (isBasinRecipe && !((BasinRecipe) recipe).getRequiredHeat()
			.testBlazeBurner(BasinBlockEntity.getHeatLevelOf(level.getBlockState(basin.getBlockPos()
				.below()))))
			return false;

		List<ItemStack> outputItems = new ArrayList<>();
		List<ChemicalStack> outputChemicals = new ArrayList<>();

		List<Ingredient> ingredients = new LinkedList<>(recipe.getIngredients());
		List<SizedFluidIngredient> fluidIngredients =
			isBasinRecipe ? ((BasinRecipe) recipe).getFluidIngredients() : List.of();

		for (boolean simulate : Iterate.trueAndFalse) {

			if (!simulate && test)
				return true;

			int[] extractedItemsFromSlot = new int[availableItems.getSlots()];
			int[] extractedFluidsFromTank = new int[availableFluids.getTanks()];

			Ingredients:
			for (Ingredient ingredient : ingredients) {
				for (int slot = 0; slot < availableItems.getSlots(); slot++) {
					if (simulate && availableItems.getStackInSlot(slot)
						.getCount() <= extractedItemsFromSlot[slot])
						continue;
					ItemStack extracted = availableItems.extractItem(slot, 1, true);
					if (!ingredient.test(extracted))
						continue;
					if (!simulate)
						availableItems.extractItem(slot, 1, false);
					extractedItemsFromSlot[slot]++;
					continue Ingredients;
				}
				return false;
			}

			boolean fluidsAffected = false;
			FluidIngredients:
			for (SizedFluidIngredient fluidIngredient : fluidIngredients) {
				int amountRequired = fluidIngredient.amount();

				for (int tank = 0; tank < availableFluids.getTanks(); tank++) {
					FluidStack fluidStack = availableFluids.getFluidInTank(tank);
					if (simulate && fluidStack.getAmount() <= extractedFluidsFromTank[tank])
						continue;
					if (!fluidIngredient.test(fluidStack))
						continue;
					int drainedAmount = Math.min(amountRequired, fluidStack.getAmount());
					if (!simulate) {
						fluidStack.shrink(drainedAmount);
						fluidsAffected = true;
					}
					amountRequired -= drainedAmount;
					if (amountRequired != 0)
						continue;
					extractedFluidsFromTank[tank] += drainedAmount;
					continue FluidIngredients;
				}
				return false;
			}

			if (fluidsAffected) {
				basin.getBehaviour(SmartFluidTankBehaviour.INPUT)
					.forEach(TankSegment::onFluidStackChanged);
				basin.getBehaviour(SmartFluidTankBehaviour.OUTPUT)
					.forEach(TankSegment::onFluidStackChanged);
			}

			if (simulate && isBasinRecipe) {
				BasinRecipe basinRecipe = (BasinRecipe) recipe;
				outputItems.addAll(basinRecipe.rollResults(level.random));
				if (basinRecipe instanceof SeparatingRecipe separatingRecipe)
					outputChemicals.addAll(separatingRecipe.getChemicalResults());
			}

			// The one departure from Create: chemicals go out of the sides, items still into the basin.
			if (!distributeChemicals(outputChemicals, simulate))
				return false;
			if (!basin.acceptOutputs(outputItems, List.of(), simulate))
				return false;
		}

		return true;
	}

	@Override
	protected <I extends RecipeInput> boolean matchBasinRecipe(Recipe<I> recipe) {
		if (recipe == null)
			return false;
		Optional<BasinBlockEntity> basin = getBasin();
		return basin.isPresent() && runRecipe(basin.get(), recipe, true);
	}

	@Override
	protected void applyBasinRecipe() {
		if (currentRecipe == null)
			return;
		Optional<BasinBlockEntity> optionalBasin = getBasin();
		if (optionalBasin.isEmpty())
			return;

		BasinBlockEntity basin = optionalBasin.get();
		boolean wasEmpty = basin.canContinueProcessing();
		if (!runRecipe(basin, currentRecipe, false))
			return;

		getProcessedRecipeTrigger().ifPresent(this::award);
		basin.inputTank.sendDataImmediately();

		if (wasEmpty && matchBasinRecipe(currentRecipe)) {
			continueWithPreviousRecipe();
			sendData();
		}

		basin.notifyChangeOfContents();
	}
}
