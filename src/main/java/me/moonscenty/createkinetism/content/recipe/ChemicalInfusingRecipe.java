package me.moonscenty.createkinetism.content.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import com.mojang.serialization.Codec;

/**
 * Mekanism: Chemical Infuser. Two chemicals into a third, and nothing else.
 *
 * <p>The one machine in this mod whose recipe has no item and no fluid anywhere in it, so it is not
 * a Create {@code ProcessingRecipe} at all - there would be nothing for the params to hold. The
 * shape and the field names are Mekanism's own.</p>
 *
 * <pre>{@code
 * {
 *   "type": "createkinetism:chemical_infusing",
 *   "left_input": { "chemical": "mekanism:oxygen", "amount": 100 },
 *   "right_input": { "chemical": "mekanism:sulfur_dioxide", "amount": 200 },
 *   "output": { "id": "mekanism:sulfur_trioxide", "amount": 200 },
 *   "processing_time": 100
 * }
 * }</pre>
 *
 * <p>Left and right are a naming convenience, not a requirement: {@link #matches} tries the tanks
 * both ways round, the way Mekanism's own {@code EitherSideChemical} lookup does. Plumbing the two
 * gases into the wrong sides of the machine should not be a puzzle.</p>
 */
public record ChemicalInfusingRecipe(ChemicalStackIngredient leftInput,
	ChemicalStackIngredient rightInput, ChemicalStack output,
	int processingTime) implements Recipe<RecipeInput> {

	public static final int DEFAULT_PROCESSING_TIME = 100;

	/**
	 * Whether the two tanks between them satisfy this recipe, in either order.
	 *
	 * <p>{@code ChemicalStackIngredient.test} checks the amount as well as the type, so a tank holding
	 * half the cost does not count.</p>
	 */
	public boolean matches(ChemicalStack left, ChemicalStack right) {
		return (leftInput.test(left) && rightInput.test(right))
			|| (leftInput.test(right) && rightInput.test(left));
	}

	/** How much comes out of the tank on the given side, for the pairing {@link #matches} accepted. */
	public long costFor(ChemicalStack left, ChemicalStack right, boolean wantLeftTank) {
		boolean straight = leftInput.test(left) && rightInput.test(right);
		if (wantLeftTank)
			return straight ? leftInput.amount() : rightInput.amount();
		return straight ? rightInput.amount() : leftInput.amount();
	}

	/** A fresh copy each time - the caller inserts it into a tank, which would otherwise alias. */
	public ChemicalStack getChemicalOutput() {
		return output.copy();
	}

	@Override
	public boolean matches(RecipeInput inv, Level level) {
		// Nothing item-shaped is ever offered; the machine matches on its tanks - see above.
		return false;
	}

	@Override
	public ItemStack assemble(RecipeInput inv, HolderLookup.Provider registries) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return ItemStack.EMPTY;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return CKRecipeTypes.CHEMICAL_INFUSING.getSerializer();
	}

	@Override
	public RecipeType<?> getType() {
		return CKRecipeTypes.CHEMICAL_INFUSING.getType();
	}

	public static class Serializer implements RecipeSerializer<ChemicalInfusingRecipe> {

		private static final MapCodec<ChemicalInfusingRecipe> CODEC =
			RecordCodecBuilder.mapCodec(instance -> instance.group(
				ChemicalStackIngredient.CODEC.fieldOf("left_input")
					.forGetter(ChemicalInfusingRecipe::leftInput),
				ChemicalStackIngredient.CODEC.fieldOf("right_input")
					.forGetter(ChemicalInfusingRecipe::rightInput),
				ChemicalStack.CODEC.fieldOf("output")
					.forGetter(ChemicalInfusingRecipe::output),
				Codec.INT.optionalFieldOf("processing_time", DEFAULT_PROCESSING_TIME)
					.forGetter(ChemicalInfusingRecipe::processingTime))
				.apply(instance, ChemicalInfusingRecipe::new));

		private static final StreamCodec<RegistryFriendlyByteBuf, ChemicalInfusingRecipe> STREAM_CODEC =
			StreamCodec.composite(ChemicalStackIngredient.STREAM_CODEC, ChemicalInfusingRecipe::leftInput,
				ChemicalStackIngredient.STREAM_CODEC, ChemicalInfusingRecipe::rightInput,
				ChemicalStack.STREAM_CODEC, ChemicalInfusingRecipe::output,
				ByteBufCodecs.VAR_INT, ChemicalInfusingRecipe::processingTime,
				ChemicalInfusingRecipe::new);

		@Override
		public MapCodec<ChemicalInfusingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, ChemicalInfusingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
