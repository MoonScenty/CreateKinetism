package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * Mekanism: Metallurgic Infuser. One item, one infusion fluid, one item out.
 *
 * <p>Mekanism keeps the infusion inside the machine as a stored "infuse type" and feeds it from a
 * second slot. Ours is a spout, so the infusion is a fluid that gets dripped onto whatever is on the
 * depot or belt below, and the shape is Create's {@code FillingRecipe} rather than a chamber's: the
 * item cost is one, and everything that used to be paid in stacks of redstone or coal is now paid in
 * millibuckets of {@code redstone_infusion} or {@code carbon_infusion}.</p>
 */
public class InfusingRecipe extends StandardProcessingRecipe<RecipeInput> {

	public InfusingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.INFUSING, params);
	}

	/**
	 * Only the item is checked here. The fluid is not part of the inventory the belt hands us, so the
	 * machine tests it separately against its own tank - see {@code MechanicalInfuserBlockEntity}.
	 */
	@Override
	public boolean matches(RecipeInput inv, Level level) {
		return !inv.isEmpty() && ingredients.get(0)
			.test(inv.getItem(0));
	}

	/** Whether this recipe can be paid for out of the given tank. */
	public boolean matchesFluid(FluidStack available) {
		return getRequiredFluid().test(available);
	}

	public SizedFluidIngredient getRequiredFluid() {
		if (fluidIngredients.isEmpty())
			throw new IllegalStateException("Infusing recipe has no infusion fluid");
		return fluidIngredients.get(0);
	}

	public ItemStack getResultItem() {
		return getRollableResults().get(0)
			.getStack();
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

	@Override
	protected int getMaxFluidInputCount() {
		return 1;
	}

	@Override
	protected boolean canSpecifyDuration() {
		return true;
	}
}
