package me.moonscenty.createkinetism.content.recipe;

import java.util.Optional;

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
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Mekanism: Pressurized Reaction Chamber. An item, a fluid and a gas into an item and a gas.
 *
 * <p>Split the way the block is built, and the split now matches what each thing is. The
 * <b>basin</b> holds the liquid and the item and takes the item back; the <b>chamber</b> holds the
 * gas on both sides, in a tank in and a tank out. Gases in this mod are Mekanism chemicals and a
 * basin cannot hold one, so this is not a preference - it is the only place each half can live.</p>
 *
 * <pre>
 * {
 *   "type": "createkinetism:reacting",
 *   "ingredients": [ { "item": "mekanism:substrate" },
 *                    { "type": "neoforge:single", "amount": 200, "fluid": "minecraft:water" } ],
 *   "chemical_input":  { "chemical": "mekanism:ethene", "amount": 100 },
 *   "results": [ { "id": "mekanism:substrate", "count": 8 } ],
 *   "chemical_output": { "id": "mekanism:oxygen", "amount": 10 },
 *   "processing_time": 300
 * }
 * </pre>
 *
 * <p>{@code chemical_output} is optional: a reaction that makes only an item leaves it out, and the
 * chamber's outlet tank simply stays empty.</p>
 */
public class ReactingRecipe extends VatRecipe {

	private final ChemicalStackIngredient chemicalInput;
	private final Optional<ChemicalStack> chemicalOutput;

	public ReactingRecipe(ProcessingRecipeParams params, ChemicalStackIngredient chemicalInput,
		Optional<ChemicalStack> chemicalOutput) {
		super(CKRecipeTypes.REACTING, params);
		this.chemicalInput = chemicalInput;
		this.chemicalOutput = chemicalOutput;
	}

	/** The gas the chamber has to be holding, amount included. */
	public ChemicalStackIngredient getChemicalInput() {
		return chemicalInput;
	}

	/** What the chamber's outlet tank is handed, if the reaction makes a gas at all. */
	public Optional<ChemicalStack> getChemicalOutput() {
		return chemicalOutput;
	}

	public boolean matchesChemical(ChemicalStack available) {
		return chemicalInput.test(available);
	}

	public long getRequiredAmount() {
		return chemicalInput.amount();
	}

	/**
	 * The basin's own limit, not a design one.
	 *
	 * <p>Create's processing ingredients carry no count, so a recipe that wants twenty planks lists
	 * the ingredient twenty times and {@code BasinRecipe} pulls one item per entry. The cap has to
	 * leave room for that; the recipes themselves still only ever name a single kind of item.</p>
	 */
	@Override
	protected int getMaxInputCount() {
		return 64;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

	/** One, and it is the basin's: the gas is not a fluid ingredient any more. */
	@Override
	protected int getMaxFluidInputCount() {
		return 1;
	}

	/** None. What used to be a fluid result is the outlet tank's gas now. */
	@Override
	protected int getMaxFluidOutputCount() {
		return 0;
	}

	/** Create's own recipe codec with the two gas fields added. */
	public static class Serializer implements RecipeSerializer<ReactingRecipe> {

		private static final MapCodec<ReactingRecipe> CODEC =
			RecordCodecBuilder.<ReactingRecipe>mapCodec(instance -> instance.group(
				ProcessingRecipeParams.CODEC.forGetter(ProcessingRecipe::getParams),
				ChemicalStackIngredient.CODEC.fieldOf("chemical_input")
					.forGetter(ReactingRecipe::getChemicalInput),
				ChemicalStack.CODEC.optionalFieldOf("chemical_output")
					.forGetter(ReactingRecipe::getChemicalOutput))
				.apply(instance, ReactingRecipe::new))
				.validate(recipe -> {
					var errors = recipe.validate();
					if (errors.isEmpty())
						return DataResult.success(recipe);
					errors.add(recipe.getClass()
						.getSimpleName() + " failed validation:");
					return DataResult.error(() -> Joiner.on('\n')
						.join(errors), recipe);
				});

		private static final StreamCodec<RegistryFriendlyByteBuf, ReactingRecipe> STREAM_CODEC =
			StreamCodec.composite(ProcessingRecipeParams.STREAM_CODEC, ProcessingRecipe::getParams,
				ChemicalStackIngredient.STREAM_CODEC, ReactingRecipe::getChemicalInput,
				ByteBufCodecs.optional(ChemicalStack.OPTIONAL_STREAM_CODEC), ReactingRecipe::getChemicalOutput,
				ReactingRecipe::new);

		@Override
		public MapCodec<ReactingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, ReactingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
