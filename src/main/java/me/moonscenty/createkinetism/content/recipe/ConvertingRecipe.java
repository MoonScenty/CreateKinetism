package me.moonscenty.createkinetism.content.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import mekanism.api.chemical.ChemicalStack;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * One solid infusion source dissolved into the Mekanism chemical it stands for.
 *
 * <p>This is the Metallurgic Infuser's own infusion slot, which Mekanism puts on the machine and we
 * once could not - see {@code MechanicalMetallurgicInfuserBlockEntity}. The output is a
 * {@link ChemicalStack}, not a fluid: Mekanism has no fluid for its infuse types, and now that the
 * infuser holds a chemical tank there is no reason to invent one.</p>
 *
 * <p>Not a Create {@code ProcessingRecipe}, because that shape only knows items and fluids. The JSON
 * is Mekanism's instead, so it reads the same as a {@code mekanism:chemical_conversion}:</p>
 *
 * <pre>{@code
 * {
 *   "type": "createkinetism:converting",
 *   "input": { "item": "mekanism:enriched_redstone" },
 *   "output": { "id": "mekanism:redstone", "amount": 80 }
 * }
 * }</pre>
 *
 * <p>The yield carries Mekanism's enrichment ratio: a plain item is worth an eighth of its Enriched
 * form, so enriching is an efficiency upgrade rather than a gate.</p>
 */
public record ConvertingRecipe(Ingredient input, ChemicalStack output) implements Recipe<SingleRecipeInput> {

	@Override
	public boolean matches(SingleRecipeInput inv, Level level) {
		return input.test(inv.item());
	}

	/** Nothing item-shaped comes out of this one; the whole result is the chemical. */
	@Override
	public ItemStack assemble(SingleRecipeInput inv, HolderLookup.Provider registries) {
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
		return CKRecipeTypes.CONVERTING.getSerializer();
	}

	@Override
	public RecipeType<?> getType() {
		return CKRecipeTypes.CONVERTING.getType();
	}

	public static class Serializer implements RecipeSerializer<ConvertingRecipe> {

		private static final MapCodec<ConvertingRecipe> CODEC =
			RecordCodecBuilder.mapCodec(instance -> instance.group(
				Ingredient.CODEC_NONEMPTY.fieldOf("input")
					.forGetter(ConvertingRecipe::input),
				ChemicalStack.CODEC.fieldOf("output")
					.forGetter(ConvertingRecipe::output))
				.apply(instance, ConvertingRecipe::new));

		private static final StreamCodec<RegistryFriendlyByteBuf, ConvertingRecipe> STREAM_CODEC =
			StreamCodec.composite(Ingredient.CONTENTS_STREAM_CODEC, ConvertingRecipe::input,
				ChemicalStack.STREAM_CODEC, ConvertingRecipe::output, ConvertingRecipe::new);

		@Override
		public MapCodec<ConvertingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, ConvertingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
