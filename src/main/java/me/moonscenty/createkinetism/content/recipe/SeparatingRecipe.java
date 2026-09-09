package me.moonscenty.createkinetism.content.recipe;

import java.util.List;

import com.google.common.base.Joiner;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import mekanism.api.chemical.ChemicalStack;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Mekanism: Electrolytic Separator. One fluid split into two chemicals, one per side of the shaft.
 *
 * <pre>{@code
 * {
 *   "type": "createkinetism:separating",
 *   "ingredients": [ { "type": "neoforge:single", "amount": 1000, "fluid": "minecraft:water" } ],
 *   "results": [],
 *   "chemical_results": [
 *     { "id": "mekanism:hydrogen", "amount": 500 },
 *     { "id": "mekanism:oxygen", "amount": 500 }
 *   ],
 *   "processing_time": 200
 * }
 * }</pre>
 *
 * <p>The first entry leaves through
 * {@link me.moonscenty.createkinetism.content.vat.MechanicalElectrolyzerBlockEntity#outputSides()}'s
 * first side, the second entry the second - the basin cannot hold either, since it has nowhere to
 * put a chemical at all.</p>
 */
public class SeparatingRecipe extends VatRecipe {

	private final List<ChemicalStack> chemicalResults;

	public SeparatingRecipe(ProcessingRecipeParams params, List<ChemicalStack> chemicalResults) {
		super(CKRecipeTypes.SEPARATING, params);
		this.chemicalResults = chemicalResults;
	}

	/** Fresh copies each time - the caller inserts them into handlers, which would otherwise alias. */
	public List<ChemicalStack> getChemicalResults() {
		return chemicalResults.stream()
			.map(ChemicalStack::copy)
			.toList();
	}

	/** The stored stacks themselves, for anything that only wants to look at them. */
	public List<ChemicalStack> chemicalResults() {
		return chemicalResults;
	}

	/** Only the fluid ingredient - no items, no fluid or item output, the gases leave through the sides. */
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
		return 0;
	}

	@Override
	protected boolean canRequireHeat() {
		return false;
	}

	/** Create's own recipe codec with one field added - see {@link OxidizingRecipe.Serializer}. */
	public static class Serializer implements RecipeSerializer<SeparatingRecipe> {

		private static final MapCodec<SeparatingRecipe> CODEC =
			RecordCodecBuilder.<SeparatingRecipe>mapCodec(instance -> instance.group(
				ProcessingRecipeParams.CODEC.forGetter(ProcessingRecipe::getParams),
				ChemicalStack.CODEC.listOf()
					.fieldOf("chemical_results")
					.forGetter(SeparatingRecipe::chemicalResults))
				.apply(instance, SeparatingRecipe::new))
				.validate(recipe -> {
					var errors = recipe.validate();
					if (recipe.chemicalResults()
						.size() != 2)
						errors.add("chemical_results must have exactly 2 entries, has "
							+ recipe.chemicalResults()
								.size());
					if (errors.isEmpty())
						return DataResult.success(recipe);
					errors.add(recipe.getClass()
						.getSimpleName() + " failed validation:");
					return DataResult.error(() -> Joiner.on('\n')
						.join(errors), recipe);
				});

		private static final StreamCodec<RegistryFriendlyByteBuf, SeparatingRecipe> STREAM_CODEC =
			StreamCodec.composite(ProcessingRecipeParams.STREAM_CODEC, ProcessingRecipe::getParams,
				ChemicalStack.STREAM_CODEC.apply(ByteBufCodecs.list()), SeparatingRecipe::chemicalResults,
				SeparatingRecipe::new);

		@Override
		public MapCodec<SeparatingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, SeparatingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
