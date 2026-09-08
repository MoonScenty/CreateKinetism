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
 * Mekanism: Chemical Dissolution Chamber. Ore plus sulfuric acid to a slurry - the first 5x step.
 *
 * <p>The one machine in this mod where a chemical comes out as well as going in, so it is the only
 * recipe carrying two of them. The items still ride a basin, which is why the item half is a Create
 * {@link ProcessingRecipe} exactly as in {@link PurifyingRecipe}; both chemicals sit beside the
 * params because {@code ProcessingRecipeParams} has nowhere to put them.</p>
 *
 * <pre>{@code
 * {
 *   "type": "createkinetism:dissolving",
 *   "ingredients": [ { "tag": "c:ores/copper" } ],
 *   "chemical_input": { "chemical": "mekanism:sulfuric_acid", "amount": 200 },
 *   "chemical_output": { "id": "mekanism:dirty_copper", "amount": 1000 },
 *   "processing_time": 200
 * }
 * }</pre>
 *
 * <p>Slurries have no fluid form at all - they are Mekanism {@code Chemical} and nothing else - so
 * this recipe could not have been written any other way once this mod stopped registering its own.
 * That is also why the Dissolution Chamber sat idle between the two changes.</p>
 */
public class DissolvingRecipe extends VatRecipe {

	private final ChemicalStackIngredient chemicalInput;
	private final ChemicalStack chemicalOutput;

	public DissolvingRecipe(ProcessingRecipeParams params, ChemicalStackIngredient chemicalInput,
		ChemicalStack chemicalOutput) {
		super(CKRecipeTypes.DISSOLVING, params);
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
	 * <p>{@code ChemicalStackIngredient.test} checks the amount as well as the type, which is what is
	 * wanted: a tank holding half the cost does not satisfy the recipe.</p>
	 */
	public boolean matchesChemical(ChemicalStack available) {
		return chemicalInput.test(available);
	}

	public long getRequiredAmount() {
		return chemicalInput.amount();
	}

	/** Neither chemical is one of these; they live beside the params, not in them. */
	@Override
	protected int getMaxFluidInputCount() {
		return 0;
	}

	@Override
	protected int getMaxFluidOutputCount() {
		return 0;
	}

	/** Nothing item-shaped comes out; the whole result is the slurry. */
	@Override
	protected int getMaxOutputCount() {
		return 0;
	}

	/** Create spells "three raw copper" as the same ingredient listed three times. */
	@Override
	protected int getMaxInputCount() {
		return 9;
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}

	/** Create's own recipe codec with two fields added - see {@link InjectingRecipe.Serializer}. */
	public static class Serializer implements RecipeSerializer<DissolvingRecipe> {

		private static final MapCodec<DissolvingRecipe> CODEC =
			RecordCodecBuilder.<DissolvingRecipe>mapCodec(instance -> instance.group(
				ProcessingRecipeParams.CODEC.forGetter(ProcessingRecipe::getParams),
				ChemicalStackIngredient.CODEC.fieldOf("chemical_input")
					.forGetter(DissolvingRecipe::getRequiredChemical),
				ChemicalStack.CODEC.fieldOf("chemical_output")
					.forGetter(DissolvingRecipe::chemicalOutput))
				.apply(instance, DissolvingRecipe::new))
				.validate(recipe -> {
					var errors = recipe.validate();
					if (errors.isEmpty())
						return DataResult.success(recipe);
					errors.add(recipe.getClass()
						.getSimpleName() + " failed validation:");
					return DataResult.error(() -> Joiner.on('\n')
						.join(errors), recipe);
				});

		private static final StreamCodec<RegistryFriendlyByteBuf, DissolvingRecipe> STREAM_CODEC =
			StreamCodec.composite(ProcessingRecipeParams.STREAM_CODEC, ProcessingRecipe::getParams,
				ChemicalStackIngredient.STREAM_CODEC, DissolvingRecipe::getRequiredChemical,
				ChemicalStack.STREAM_CODEC, DissolvingRecipe::chemicalOutput, DissolvingRecipe::new);

		@Override
		public MapCodec<DissolvingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, DissolvingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
