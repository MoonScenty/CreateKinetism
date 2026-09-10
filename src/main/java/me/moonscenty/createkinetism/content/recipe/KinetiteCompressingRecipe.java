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
 * Kinetite Compressor: an item pressed under a stream of Kinetite gas.
 *
 * <p>Mekanism's Osmium Compressor with the metal swapped. The Kinetite goes in as a gas rather than
 * as an ingot on a shelf - the machine converts an ingot into 200mB in its own holder, the way the
 * Osmium Compressor does, so what a recipe asks for is a {@code chemical_input} and one item.</p>
 *
 * <pre>
 * {
 *   "type": "createkinetism:kinetite_compressing",
 *   "ingredients": [ { "item": "create:powdered_obsidian" } ],
 *   "chemical_input": { "chemical": "createkinetism:kinetite", "amount": 200 },
 *   "results": [ { "id": "mekanism:ingot_refined_obsidian" } ],
 *   "processing_time": 200
 * }
 * </pre>
 *
 * <p>Mekanism writes its own as one millibucket with {@code per_tick_usage}, which over its
 * two-hundred-tick press comes to the same two hundred. Ours states the whole cost once, as every
 * other recipe in this mod does.</p>
 */
public class KinetiteCompressingRecipe extends ChamberRecipe {

	private final ChemicalStackIngredient chemicalInput;

	public KinetiteCompressingRecipe(ProcessingRecipeParams params, ChemicalStackIngredient chemicalInput) {
		super(CKRecipeTypes.KINETITE_COMPRESSING, params);
		this.chemicalInput = chemicalInput;
	}

	public ChemicalStackIngredient getRequiredChemical() {
		return chemicalInput;
	}

	/** Type and amount both: a holder with a splash left in it cannot start a press. */
	public boolean matchesChemical(ChemicalStack available) {
		return chemicalInput.test(available);
	}

	public long getRequiredAmount() {
		return chemicalInput.amount();
	}

	/** The gas is not one of these - it lives beside the params, not in them. */
	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

	/** Create's own recipe codec with one field added - see {@link PurifyingRecipe.Serializer}. */
	public static class Serializer implements RecipeSerializer<KinetiteCompressingRecipe> {

		private static final MapCodec<KinetiteCompressingRecipe> CODEC =
			RecordCodecBuilder.<KinetiteCompressingRecipe>mapCodec(instance -> instance.group(
				ProcessingRecipeParams.CODEC.forGetter(ProcessingRecipe::getParams),
				ChemicalStackIngredient.CODEC.fieldOf("chemical_input")
					.forGetter(KinetiteCompressingRecipe::getRequiredChemical))
				.apply(instance, KinetiteCompressingRecipe::new))
				.validate(recipe -> {
					var errors = recipe.validate();
					if (errors.isEmpty())
						return DataResult.success(recipe);
					errors.add(recipe.getClass()
						.getSimpleName() + " failed validation:");
					return DataResult.error(() -> Joiner.on('\n')
						.join(errors), recipe);
				});

		private static final StreamCodec<RegistryFriendlyByteBuf, KinetiteCompressingRecipe> STREAM_CODEC =
			StreamCodec.composite(ProcessingRecipeParams.STREAM_CODEC, ProcessingRecipe::getParams,
				ChemicalStackIngredient.STREAM_CODEC, KinetiteCompressingRecipe::getRequiredChemical,
				KinetiteCompressingRecipe::new);

		@Override
		public MapCodec<KinetiteCompressingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, KinetiteCompressingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
