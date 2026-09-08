package me.moonscenty.createkinetism.content.recipe;

import com.google.common.base.Joiner;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import mekanism.api.chemical.ChemicalStack;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Mekanism: Chemical Oxidizer. A solid into a gas, and nothing else.
 *
 * <p>{@link CrystallizingRecipe} turned inside out. There the only input was a chemical, so the
 * basin could only be an output; here the only output is a chemical, so the basin can only be an
 * input. The item goes in the basin as it always did and the gas leaves through the block.</p>
 *
 * <pre>{@code
 * {
 *   "type": "createkinetism:oxidizing",
 *   "ingredients": [ { "tag": "c:dusts/sulfur" } ],
 *   "results": [],
 *   "chemical_output": { "id": "mekanism:sulfur_dioxide", "amount": 100 },
 *   "processing_time": 100
 * }
 * }</pre>
 *
 * <p>A gas going <em>in</em> would mean the Chemical Infuser, not this, so the limits say one item
 * in and nothing else rather than leaving the vat's generous 2/4/2/2 in place.</p>
 */
public class OxidizingRecipe extends VatRecipe {

	private final ChemicalStack chemicalOutput;

	public OxidizingRecipe(ProcessingRecipeParams params, ChemicalStack chemicalOutput) {
		super(CKRecipeTypes.OXIDIZING, params);
		this.chemicalOutput = chemicalOutput;
	}

	/** A fresh copy each time - the caller inserts it into a tank, which would otherwise alias. */
	public ChemicalStack getChemicalOutput() {
		return chemicalOutput.copy();
	}

	/** The stored stack itself, for anything that only wants to look at it. */
	public ChemicalStack chemicalOutput() {
		return chemicalOutput;
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxFluidInputCount() {
		return 0;
	}

	/** Nothing comes out but the gas, and that is neither an item nor a fluid. */
	@Override
	protected int getMaxOutputCount() {
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

	/** Create's own recipe codec with one field added - see {@link InjectingRecipe.Serializer}. */
	public static class Serializer implements RecipeSerializer<OxidizingRecipe> {

		private static final MapCodec<OxidizingRecipe> CODEC =
			RecordCodecBuilder.<OxidizingRecipe>mapCodec(instance -> instance.group(
				ProcessingRecipeParams.CODEC.forGetter(ProcessingRecipe::getParams),
				ChemicalStack.CODEC.fieldOf("chemical_output")
					.forGetter(OxidizingRecipe::chemicalOutput))
				.apply(instance, OxidizingRecipe::new))
				.validate(recipe -> {
					var errors = recipe.validate();
					if (errors.isEmpty())
						return DataResult.success(recipe);
					errors.add(recipe.getClass()
						.getSimpleName() + " failed validation:");
					return DataResult.error(() -> Joiner.on('\n')
						.join(errors), recipe);
				});

		private static final StreamCodec<RegistryFriendlyByteBuf, OxidizingRecipe> STREAM_CODEC =
			StreamCodec.composite(ProcessingRecipeParams.STREAM_CODEC, ProcessingRecipe::getParams,
				ChemicalStack.STREAM_CODEC, OxidizingRecipe::chemicalOutput, OxidizingRecipe::new);

		@Override
		public MapCodec<OxidizingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, OxidizingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
