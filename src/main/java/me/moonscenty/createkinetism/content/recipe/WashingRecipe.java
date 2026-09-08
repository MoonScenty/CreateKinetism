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
 * Mekanism: Chemical Washer. Dirty slurry plus a great deal of water, to clean slurry.
 *
 * <p>The only recipe here with all three kinds at once. Water is a real fluid and stays a Create
 * fluid ingredient in the params, where the machine's own fluid tank can hold it; both slurries are
 * Mekanism chemicals with no fluid form at all, so they sit beside the params the way
 * {@link DissolvingRecipe} does.</p>
 *
 * <pre>{@code
 * {
 *   "type": "createkinetism:washing",
 *   "ingredients": [ { "type": "neoforge:single", "fluid": "minecraft:water", "amount": 1000 } ],
 *   "chemical_input": { "chemical": "mekanism:dirty_copper", "amount": 200 },
 *   "chemical_output": { "id": "mekanism:clean_copper", "amount": 200 },
 *   "processing_time": 20
 * }
 * }</pre>
 *
 * <p>Five parts water to one of slurry is Mekanism's own ratio, and the reason the 5x line needs a
 * real water supply rather than a bucket.</p>
 */
public class WashingRecipe extends VatRecipe {

	private final ChemicalStackIngredient chemicalInput;
	private final ChemicalStack chemicalOutput;

	public WashingRecipe(ProcessingRecipeParams params, ChemicalStackIngredient chemicalInput,
		ChemicalStack chemicalOutput) {
		super(CKRecipeTypes.WASHING, params);
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

	public boolean matchesChemical(ChemicalStack available) {
		return chemicalInput.test(available);
	}

	public long getRequiredAmount() {
		return chemicalInput.amount();
	}

	/** Water, and nothing else. */
	@Override
	protected int getMaxFluidInputCount() {
		return 1;
	}

	@Override
	protected int getMaxFluidOutputCount() {
		return 0;
	}

	/** Nothing item-shaped is involved at all - this machine washes a liquid with a liquid. */
	@Override
	protected int getMaxInputCount() {
		return 0;
	}

	@Override
	protected int getMaxOutputCount() {
		return 0;
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}

	/** Create's own recipe codec with two fields added - see {@link InjectingRecipe.Serializer}. */
	public static class Serializer implements RecipeSerializer<WashingRecipe> {

		private static final MapCodec<WashingRecipe> CODEC =
			RecordCodecBuilder.<WashingRecipe>mapCodec(instance -> instance.group(
				ProcessingRecipeParams.CODEC.forGetter(ProcessingRecipe::getParams),
				ChemicalStackIngredient.CODEC.fieldOf("chemical_input")
					.forGetter(WashingRecipe::getRequiredChemical),
				ChemicalStack.CODEC.fieldOf("chemical_output")
					.forGetter(WashingRecipe::chemicalOutput))
				.apply(instance, WashingRecipe::new))
				.validate(recipe -> {
					var errors = recipe.validate();
					if (errors.isEmpty())
						return DataResult.success(recipe);
					errors.add(recipe.getClass()
						.getSimpleName() + " failed validation:");
					return DataResult.error(() -> Joiner.on('\n')
						.join(errors), recipe);
				});

		private static final StreamCodec<RegistryFriendlyByteBuf, WashingRecipe> STREAM_CODEC =
			StreamCodec.composite(ProcessingRecipeParams.STREAM_CODEC, ProcessingRecipe::getParams,
				ChemicalStackIngredient.STREAM_CODEC, WashingRecipe::getRequiredChemical,
				ChemicalStack.STREAM_CODEC, WashingRecipe::chemicalOutput, WashingRecipe::new);

		@Override
		public MapCodec<WashingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, WashingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
