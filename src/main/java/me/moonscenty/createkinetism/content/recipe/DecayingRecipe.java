package me.moonscenty.createkinetism.content.recipe;

import java.util.Optional;

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
 * Radioactive Waste Drum: what may go in, and what it turns into on the way out.
 *
 * <p>Not how fast. Mekanism's barrel rots a flat millibucket a second no matter what is in it, and
 * so does this one - the rate is the drum's, not the recipe's, so there is nothing here to write it
 * with. What the amounts express is a <b>ratio</b>: one input's worth becomes one output's worth, and
 * whatever the drum drains in a second is converted by the same proportion.</p>
 *
 * <p>Writing this as a recipe at all decides a second question for free: <b>the drum accepts exactly
 * the chemicals named here</b>. The block has no drain, so anything it would take but could not rot
 * is stuck in there forever - the recipe list is the inlet's guest list, and adding a waste is one
 * file rather than a code change.</p>
 *
 * <p>The output is optional, and both recipes shipped here go without one. Mekanism's barrel simply
 * deletes what it holds; the slot exists so a pack can make waste rot into something instead.</p>
 *
 * <pre>{@code
 * {
 *   "type": "createkinetism:decaying",
 *   "input": { "chemical": "mekanism:spent_nuclear_waste", "amount": 1 }
 * }
 * }</pre>
 */
public record DecayingRecipe(ChemicalStackIngredient input, Optional<ChemicalStack> output)
	implements Recipe<RecipeInput> {

	/** Whether the drum will take this at all. */
	public boolean matches(ChemicalStack available) {
		return input.testType(available);
	}

	/** One unit of input, as the ratio's denominator. */
	public long getInputUnit() {
		return Math.max(1, input.amount());
	}

	/** What {@code drained} millibuckets of input become. Empty when the waste just goes away. */
	public ChemicalStack yieldFor(long drained) {
		return output.map(made -> made.copyWithAmount(
			Math.max(1, Math.round(made.getAmount() * (drained / (double) getInputUnit())))))
			.orElse(ChemicalStack.EMPTY);
	}

	@Override
	public boolean matches(RecipeInput inv, Level level) {
		// Nothing item-shaped is ever offered; the drum matches on its tank - see above.
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
		return CKRecipeTypes.DECAYING.getSerializer();
	}

	@Override
	public RecipeType<?> getType() {
		return CKRecipeTypes.DECAYING.getType();
	}

	public static class Serializer implements RecipeSerializer<DecayingRecipe> {

		private static final MapCodec<DecayingRecipe> CODEC =
			RecordCodecBuilder.mapCodec(instance -> instance.group(
				ChemicalStackIngredient.CODEC.fieldOf("input")
					.forGetter(DecayingRecipe::input),
				ChemicalStack.CODEC.optionalFieldOf("output")
					.forGetter(DecayingRecipe::output))
				.apply(instance, DecayingRecipe::new));

		private static final StreamCodec<RegistryFriendlyByteBuf, DecayingRecipe> STREAM_CODEC =
			StreamCodec.composite(ChemicalStackIngredient.STREAM_CODEC, DecayingRecipe::input,
				ByteBufCodecs.optional(ChemicalStack.STREAM_CODEC), DecayingRecipe::output,
				DecayingRecipe::new);

		@Override
		public MapCodec<DecayingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, DecayingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
