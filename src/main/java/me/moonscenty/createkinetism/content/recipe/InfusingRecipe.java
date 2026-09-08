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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Mekanism: Metallurgic Infuser. One item, one infusion chemical, one item out.
 *
 * <p>The infusion is a Mekanism {@link ChemicalStack} held in the machine's own chemical tank, which
 * is where Mekanism holds it too. The spout shape is still ours - the nozzle drips onto whatever is
 * on the depot or belt below - but what it drips is a chemical, so this mod no longer registers a
 * fluid standing in for one.</p>
 *
 * <p>Not a Create {@code ProcessingRecipe}: that shape knows items and fluids and nothing else. The
 * JSON is Mekanism's field naming instead, so it reads like a {@code mekanism:metallurgic_infusing}
 * with our processing time added:</p>
 *
 * <pre>{@code
 * {
 *   "type": "createkinetism:infusing",
 *   "item_input": { "tag": "c:ingots/iron" },
 *   "chemical_input": { "chemical": "mekanism:redstone", "amount": 80 },
 *   "output": { "id": "mekanism:enriched_iron", "count": 1 },
 *   "processing_time": 100
 * }
 * }</pre>
 */
public record InfusingRecipe(Ingredient itemInput, ChemicalStackIngredient chemicalInput,
	ItemStack output, int processingTime) implements Recipe<SingleRecipeInput> {

	public static final int DEFAULT_PROCESSING_TIME = 100;

	/**
	 * Only the item is checked here. The chemical is not part of the inventory the belt hands us, so
	 * the machine tests it separately against its own tank - see
	 * {@code MechanicalMetallurgicInfuserBlockEntity}.
	 */
	@Override
	public boolean matches(SingleRecipeInput inv, Level level) {
		return itemInput.test(inv.item());
	}

	/**
	 * Whether this recipe can be paid for out of the given tank.
	 *
	 * <p>{@code ChemicalStackIngredient.test} checks the amount as well as the type, which is exactly
	 * what is wanted here: a tank holding 40mB of redstone does not satisfy a recipe asking 80.</p>
	 */
	public boolean matchesChemical(ChemicalStack available) {
		return chemicalInput.test(available);
	}

	public long getRequiredAmount() {
		return chemicalInput.amount();
	}

	public ItemStack getResultItem() {
		return output.copy();
	}

	@Override
	public ItemStack assemble(SingleRecipeInput inv, HolderLookup.Provider registries) {
		return output.copy();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return output;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return CKRecipeTypes.INFUSING.getSerializer();
	}

	@Override
	public RecipeType<?> getType() {
		return CKRecipeTypes.INFUSING.getType();
	}

	public static class Serializer implements RecipeSerializer<InfusingRecipe> {

		private static final MapCodec<InfusingRecipe> CODEC =
			RecordCodecBuilder.mapCodec(instance -> instance.group(
				Ingredient.CODEC_NONEMPTY.fieldOf("item_input")
					.forGetter(InfusingRecipe::itemInput),
				ChemicalStackIngredient.CODEC.fieldOf("chemical_input")
					.forGetter(InfusingRecipe::chemicalInput),
				ItemStack.CODEC.fieldOf("output")
					.forGetter(InfusingRecipe::output),
				com.mojang.serialization.Codec.INT.optionalFieldOf("processing_time", DEFAULT_PROCESSING_TIME)
					.forGetter(InfusingRecipe::processingTime))
				.apply(instance, InfusingRecipe::new));

		private static final StreamCodec<RegistryFriendlyByteBuf, InfusingRecipe> STREAM_CODEC =
			StreamCodec.composite(Ingredient.CONTENTS_STREAM_CODEC, InfusingRecipe::itemInput,
				ChemicalStackIngredient.STREAM_CODEC, InfusingRecipe::chemicalInput,
				ItemStack.STREAM_CODEC, InfusingRecipe::output,
				ByteBufCodecs.VAR_INT, InfusingRecipe::processingTime, InfusingRecipe::new);

		@Override
		public MapCodec<InfusingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, InfusingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
