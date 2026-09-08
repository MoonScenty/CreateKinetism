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
 * Mekanism: Chemical Crystallizer. Clean slurry into crystals, and nothing else - the last 5x step.
 *
 * <p>The odd one out among the machines that took a chemical tank. Every other one still had
 * something item- or fluid-shaped going in, so its basin kept a job; this recipe's only input is the
 * slurry. Move that into the block and the basin has nothing left to hold on the way in - so here it
 * holds nothing but the way out. The block is fed by tube and the basin only catches crystals.</p>
 *
 * <pre>{@code
 * {
 *   "type": "createkinetism:crystallizing",
 *   "ingredients": [],
 *   "chemical_input": { "chemical": "mekanism:clean_copper", "amount": 200 },
 *   "results": [ { "id": "mekanism:crystal_copper", "count": 1 } ],
 *   "processing_time": 100
 * }
 * }</pre>
 *
 * <p>Two hundred millibuckets to a crystal is Mekanism's own figure and needs no scaling - unlike the
 * injection and purification steps, a crystallizer spends its chemical per operation rather than per
 * tick. It is also exactly what the Mechanical Washer puts out in one batch, so the two line up
 * without a buffer between them.</p>
 */
public class CrystallizingRecipe extends VatRecipe {

	private final ChemicalStackIngredient chemicalInput;

	public CrystallizingRecipe(ProcessingRecipeParams params, ChemicalStackIngredient chemicalInput) {
		super(CKRecipeTypes.CRYSTALLIZING, params);
		this.chemicalInput = chemicalInput;
	}

	public ChemicalStackIngredient getRequiredChemical() {
		return chemicalInput;
	}

	public boolean matchesChemical(ChemicalStack available) {
		return chemicalInput.test(available);
	}

	public long getRequiredAmount() {
		return chemicalInput.amount();
	}

	/** Nothing goes in but the slurry, and that is neither an item nor a fluid. */
	@Override
	protected int getMaxInputCount() {
		return 0;
	}

	@Override
	protected int getMaxFluidInputCount() {
		return 0;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

	@Override
	protected int getMaxFluidOutputCount() {
		return 0;
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}

	/** Create's own recipe codec with one field added - see {@link InjectingRecipe.Serializer}. */
	public static class Serializer implements RecipeSerializer<CrystallizingRecipe> {

		private static final MapCodec<CrystallizingRecipe> CODEC =
			RecordCodecBuilder.<CrystallizingRecipe>mapCodec(instance -> instance.group(
				ProcessingRecipeParams.CODEC.forGetter(ProcessingRecipe::getParams),
				ChemicalStackIngredient.CODEC.fieldOf("chemical_input")
					.forGetter(CrystallizingRecipe::getRequiredChemical))
				.apply(instance, CrystallizingRecipe::new))
				.validate(recipe -> {
					var errors = recipe.validate();
					if (errors.isEmpty())
						return DataResult.success(recipe);
					errors.add(recipe.getClass()
						.getSimpleName() + " failed validation:");
					return DataResult.error(() -> Joiner.on('\n')
						.join(errors), recipe);
				});

		private static final StreamCodec<RegistryFriendlyByteBuf, CrystallizingRecipe> STREAM_CODEC =
			StreamCodec.composite(ProcessingRecipeParams.STREAM_CODEC, ProcessingRecipe::getParams,
				ChemicalStackIngredient.STREAM_CODEC, CrystallizingRecipe::getRequiredChemical,
				CrystallizingRecipe::new);

		@Override
		public MapCodec<CrystallizingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, CrystallizingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
