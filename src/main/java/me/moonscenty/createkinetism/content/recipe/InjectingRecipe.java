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
 * Mekanism: Chemical Injection Chamber. One item, one gas, one item out - the 4x ore step.
 *
 * <p>The shape is identical to {@link InfusingRecipe the Mechanical Infuser's own recipe}: a single
 * item ingredient paired with a single sized fluid ingredient. That is deliberate - the
 * Injection Chamber is built the same way the infuser is, a kinetic spout that drips its fluid onto
 * the item below rather than holding a slot of its own - so the two recipe types only differ in which
 * machine and which chemical they name.</p>
 */
public class InjectingRecipe extends StandardProcessingRecipe<RecipeInput> {

	public InjectingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.INJECTING, params);
	}

	/**
	 * Only the item is checked here. The fluid is not part of the inventory the belt hands us, so the
	 * machine tests it separately against its own tank - see {@code InjectionChamberBlockEntity}.
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
			throw new IllegalStateException("Injecting recipe has no fluid ingredient");
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
