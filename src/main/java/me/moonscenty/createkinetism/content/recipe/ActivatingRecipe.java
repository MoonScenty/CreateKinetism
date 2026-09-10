package me.moonscenty.createkinetism.content.recipe;

import com.mojang.serialization.Codec;
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

/**
 * Solar Neutron Activator: one gas in, one gas out, and nothing else.
 *
 * <p>Mekanism's machine has a single tank on each side and no item slots at all - what it does is
 * stand in the sun and let neutrons do the work. It used to be written here as a basin recipe with
 * the gas carried as a fluid; a basin cannot hold a Mekanism chemical, so both halves are the
 * machine's own tanks now and there is nothing item-shaped left to match on.</p>
 *
 * <pre>
 * {
 *   "type": "createkinetism:activating",
 *   "input":  { "chemical": "mekanism:nuclear_waste", "amount": 10 },
 *   "output": { "id": "mekanism:polonium", "amount": 1 },
 *   "processing_time": 100
 * }
 * </pre>
 */
public record ActivatingRecipe(ChemicalStackIngredient input, ChemicalStack output, int processingTime)
	implements Recipe<RecipeInput> {

	public static final int DEFAULT_PROCESSING_TIME = 100;

	/**
	 * Whether the inlet holds what this asks for.
	 *
	 * <p>{@code ChemicalStackIngredient.test} checks the amount as well as the type, so a tank
	 * holding half the cost does not count.</p>
	 */
	public boolean matches(ChemicalStack available) {
		return input.test(available);
	}

	public long getRequiredAmount() {
		return input.amount();
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
		return CKRecipeTypes.ACTIVATING.getSerializer();
	}

	@Override
	public RecipeType<?> getType() {
		return CKRecipeTypes.ACTIVATING.getType();
	}

	public static class Serializer implements RecipeSerializer<ActivatingRecipe> {

		private static final MapCodec<ActivatingRecipe> CODEC =
			RecordCodecBuilder.mapCodec(instance -> instance.group(
				ChemicalStackIngredient.CODEC.fieldOf("input")
					.forGetter(ActivatingRecipe::input),
				ChemicalStack.CODEC.fieldOf("output")
					.forGetter(ActivatingRecipe::output),
				Codec.INT.optionalFieldOf("processing_time", DEFAULT_PROCESSING_TIME)
					.forGetter(ActivatingRecipe::processingTime))
				.apply(instance, ActivatingRecipe::new));

		private static final StreamCodec<RegistryFriendlyByteBuf, ActivatingRecipe> STREAM_CODEC =
			StreamCodec.composite(ChemicalStackIngredient.STREAM_CODEC, ActivatingRecipe::input,
				ChemicalStack.STREAM_CODEC, ActivatingRecipe::output,
				ByteBufCodecs.VAR_INT, ActivatingRecipe::processingTime,
				ActivatingRecipe::new);

		@Override
		public MapCodec<ActivatingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, ActivatingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
