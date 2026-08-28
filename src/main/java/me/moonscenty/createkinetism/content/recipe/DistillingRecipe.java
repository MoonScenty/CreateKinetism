package me.moonscenty.createkinetism.content.recipe;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import me.moonscenty.createkinetism.content.oil.DistilMode;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>One fluid in, up to eight out. The outputs are ordered lightest-last: result 1 goes to the
 * lowest output stage of the column, result N to the highest.</p>
 */
public class DistillingRecipe extends ProcessingRecipe<RecipeInput, DistillationRecipeParams> {

	private final DistilMode mode;

	public DistillingRecipe(DistillationRecipeParams params) {
		super(CKRecipeTypes.DISTILLING, params);
		this.mode = parseMode(params.mode());
	}

	private static DistilMode parseMode(String mode) {
		for (DistilMode candidate : DistilMode.values())
			if (candidate.name()
				.equalsIgnoreCase(mode))
				return candidate;
		return null;
	}

	public DistilMode getMode() {
		return mode;
	}

	@Override
	public List<String> validate() {
		List<String> errors = super.validate();
		if (mode == null)
			errors.add("Unknown distilling mode. Expected one of distil_flash, distil_atmospheric, distil_vacuum.");
		return errors;
	}

	/** Whether the given tanks hold enough of this recipe's feedstock to run it. */
	public boolean hasFeedstock(IFluidHandler fluids) {
		if (fluids == null)
			return false;
		SizedFluidIngredient ingredient = getFluidIngredients().get(0);
		for (int tank = 0; tank < fluids.getTanks(); tank++) {
			FluidStack fluid = fluids.getFluidInTank(tank);
			if (ingredient.test(fluid) && fluid.getAmount() >= ingredient.amount())
				return true;
		}
		return false;
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
	protected int getMaxFluidInputCount() {
		return 1;
	}

	@Override
	protected int getMaxFluidOutputCount() {
		return 8;
	}

	@Override
	protected boolean canSpecifyDuration() {
		return true;
	}

	@Override
	public boolean matches(RecipeInput recipeInput, Level level) {
		return false;
	}

	@FunctionalInterface
	public interface Factory<R extends DistillingRecipe> extends ProcessingRecipe.Factory<DistillationRecipeParams, R> {
		R create(DistillationRecipeParams params);
	}

	public static class Serializer<R extends DistillingRecipe> implements RecipeSerializer<R> {

		private final MapCodec<R> codec;
		private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

		public Serializer(ProcessingRecipe.Factory<DistillationRecipeParams, R> factory) {
			this.codec = ProcessingRecipe.codec(factory, DistillationRecipeParams.CODEC);
			this.streamCodec = ProcessingRecipe.streamCodec(factory, DistillationRecipeParams.STREAM_CODEC);
		}

		@Override
		public MapCodec<R> codec() {
			return codec;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
			return streamCodec;
		}
	}
}
