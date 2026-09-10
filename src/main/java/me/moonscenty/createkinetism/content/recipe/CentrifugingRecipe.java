package me.moonscenty.createkinetism.content.recipe;

import com.google.common.base.Joiner;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Mekanism: Isotopic Centrifuge. One gas in, one gas out, and nothing else.
 *
 * <p>Mekanism's machine has a single chemical tank on each side and no item slots at all - it
 * separates isotopes out of something that is already a gas. This used to be written as a fluid
 * recipe from a time when this mod's chemicals were fluids; both halves are Mekanism chemicals now,
 * so both sit beside the params the way {@link DissolvingRecipe}'s do, and every count the vat
 * offers is zero.</p>
 *
 * <pre>{@code
 * {
 *   "type": "createkinetism:centrifuging",
 *   "chemical_input":  { "chemical": "mekanism:nuclear_waste", "amount": 10 },
 *   "chemical_output": { "id": "mekanism:plutonium", "amount": 1 },
 *   "processing_time": 200
 * }
 * }</pre>
 */
public class CentrifugingRecipe extends VatRecipe {

	private final ChemicalStackIngredient chemicalInput;
	private final ChemicalStack chemicalOutput;

	public CentrifugingRecipe(ProcessingRecipeParams params, ChemicalStackIngredient chemicalInput,
		ChemicalStack chemicalOutput) {
		super(CKRecipeTypes.CENTRIFUGING, params);
		this.chemicalInput = chemicalInput;
		this.chemicalOutput = chemicalOutput;
	}

	public ChemicalStackIngredient getRequiredChemical() {
		return chemicalInput;
	}

	/** A fresh copy each time - the caller inserts it into a tank, which would otherwise alias. */
	public ChemicalStack getChemicalOutput() {
		return chemicalOutput.copy();
	}

	/** The stored stack itself, for anything that only wants to look at it. */
	public ChemicalStack chemicalOutput() {
		return chemicalOutput;
	}

	/**
	 * Whether this recipe can be paid for out of the given tank.
	 *
	 * <p>{@code ChemicalStackIngredient.test} checks the amount as well as the type, so a tank
	 * holding half the cost does not count.</p>
	 */
	public boolean matchesChemical(ChemicalStack available) {
		return chemicalInput.test(available);
	}

	public long getRequiredAmount() {
		return chemicalInput.amount();
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
		return 0;
	}

	@Override
	protected int getMaxFluidOutputCount() {
		return 0;
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}

	/** Create's own recipe codec with the two chemical fields added - see {@link DissolvingRecipe}. */
	public static class Serializer implements RecipeSerializer<CentrifugingRecipe> {

		private static final MapCodec<CentrifugingRecipe> CODEC =
			RecordCodecBuilder.<CentrifugingRecipe>mapCodec(instance -> instance.group(
				ProcessingRecipeParams.CODEC.forGetter(ProcessingRecipe::getParams),
				ChemicalStackIngredient.CODEC.fieldOf("chemical_input")
					.forGetter(CentrifugingRecipe::getRequiredChemical),
				ChemicalStack.CODEC.fieldOf("chemical_output")
					.forGetter(CentrifugingRecipe::chemicalOutput))
				.apply(instance, CentrifugingRecipe::new))
				.validate(recipe -> {
					var errors = recipe.validate();
					if (errors.isEmpty())
						return DataResult.success(recipe);
					errors.add(recipe.getClass()
						.getSimpleName() + " failed validation:");
					return DataResult.error(() -> Joiner.on('\n')
						.join(errors), recipe);
				});

		private static final StreamCodec<RegistryFriendlyByteBuf, CentrifugingRecipe> STREAM_CODEC =
			StreamCodec.composite(ProcessingRecipeParams.STREAM_CODEC, ProcessingRecipe::getParams,
				ChemicalStackIngredient.STREAM_CODEC, CentrifugingRecipe::getRequiredChemical,
				ChemicalStack.STREAM_CODEC, CentrifugingRecipe::chemicalOutput,
				CentrifugingRecipe::new);

		@Override
		public MapCodec<CentrifugingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, CentrifugingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
