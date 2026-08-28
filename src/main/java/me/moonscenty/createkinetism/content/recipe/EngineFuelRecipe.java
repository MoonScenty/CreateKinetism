package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>What an engine will burn, and how fast. The recipe is one fluid ingredient plus a duration, and
 * the consumption rate falls out of the two: an engine burns {@code amount / duration} millibuckets
 * per tick. A fuel with the same amount but a longer duration is simply more efficient.</p>
 *
 * <p>Each engine has its own recipe type, so a datapack can teach one engine a fuel without teaching
 * it to all of them.</p>
 */
public class EngineFuelRecipe extends StandardProcessingRecipe<RecipeInput> {

	public EngineFuelRecipe(IRecipeTypeInfo typeInfo, ProcessingRecipeParams params) {
		super(typeInfo, params);
	}

	public static EngineFuelRecipe gasoline(ProcessingRecipeParams params) {
		return new EngineFuelRecipe(CKRecipeTypes.GASOLINE_ENGINE_FUEL, params);
	}

	public static EngineFuelRecipe diesel(ProcessingRecipeParams params) {
		return new EngineFuelRecipe(CKRecipeTypes.DIESEL_ENGINE_FUEL, params);
	}

	public static EngineFuelRecipe turbine(ProcessingRecipeParams params) {
		return new EngineFuelRecipe(CKRecipeTypes.TURBINE_FUEL, params);
	}

	public boolean match(FluidStack fuel) {
		return getFluidIngredients().getFirst()
			.test(fuel);
	}

	/** Millibuckets burned per tick. */
	public float getConsumptionRate() {
		return (float) getFluidIngredients().getFirst()
			.amount() / (float) getProcessingDuration();
	}

	@Override
	protected boolean canSpecifyDuration() {
		return true;
	}

	@Override
	protected int getMaxFluidInputCount() {
		return 1;
	}

	@Override
	protected int getMaxInputCount() {
		return 0;
	}

	@Override
	protected int getMaxOutputCount() {
		return 0;
	}

	@Override
	public boolean matches(RecipeInput recipeInput, Level level) {
		return false;
	}
}
