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
 * Mekanism: Purification Chamber. Item plus oxygen to clumps - the 3x ore step.
 *
 * <p>Built the same way {@link InjectingRecipe} is, and for the same reason: the item side runs on a
 * basin so it stays a Create {@link ProcessingRecipe}, while the oxygen is a Mekanism
 * {@link ChemicalStack} bolted on beside the params. {@code ProcessingRecipeParams} knows items and
 * fluids and has nowhere to put a chemical.</p>
 *
 * <pre>{@code
 * {
 *   "type": "createkinetism:purifying",
 *   "ingredients": [ { "tag": "c:ores/copper" } ],
 *   "chemical_input": { "chemical": "mekanism:oxygen", "amount": 1 },
 *   "results": [ { "id": "mekanism:clump_copper", "count": 3 } ],
 *   "processing_time": 200
 * }
 * }</pre>
 *
 * <p>Oxygen does have a fluid form, so this could have stayed a fluid ingredient in the basin. It
 * did not, because the machine reads better with the gas held in the machine and the ore in the
 * basin - which is where Mekanism puts them too - and because the Injection Chamber next to it in
 * the chain now wants a pressurized tube either way.</p>
 */
public class PurifyingRecipe extends VatRecipe {

	private final ChemicalStackIngredient chemicalInput;

	public PurifyingRecipe(ProcessingRecipeParams params, ChemicalStackIngredient chemicalInput) {
		super(CKRecipeTypes.PURIFYING, params);
		this.chemicalInput = chemicalInput;
	}

	public ChemicalStackIngredient getRequiredChemical() {
		return chemicalInput;
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

	/** The chemical is not one of these; it lives beside the params, not in them. */
	@Override
	protected int getMaxFluidInputCount() {
		return 0;
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}

	/** Create's own recipe codec with one field added - see {@link InjectingRecipe.Serializer}. */
	public static class Serializer implements RecipeSerializer<PurifyingRecipe> {

		private static final MapCodec<PurifyingRecipe> CODEC =
			RecordCodecBuilder.<PurifyingRecipe>mapCodec(instance -> instance.group(
				ProcessingRecipeParams.CODEC.forGetter(ProcessingRecipe::getParams),
				ChemicalStackIngredient.CODEC.fieldOf("chemical_input")
					.forGetter(PurifyingRecipe::getRequiredChemical))
				.apply(instance, PurifyingRecipe::new))
				.validate(recipe -> {
					var errors = recipe.validate();
					if (errors.isEmpty())
						return DataResult.success(recipe);
					errors.add(recipe.getClass()
						.getSimpleName() + " failed validation:");
					return DataResult.error(() -> Joiner.on('\n')
						.join(errors), recipe);
				});

		private static final StreamCodec<RegistryFriendlyByteBuf, PurifyingRecipe> STREAM_CODEC =
			StreamCodec.composite(ProcessingRecipeParams.STREAM_CODEC, ProcessingRecipe::getParams,
				ChemicalStackIngredient.STREAM_CODEC, PurifyingRecipe::getRequiredChemical,
				PurifyingRecipe::new);

		@Override
		public MapCodec<PurifyingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, PurifyingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
