package me.moonscenty.createkinetism.content.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * A combining recipe, which is split across two inventories rather than one.
 *
 * <p>Mekanism's Combiner has an input slot and a separate infusion slot, and this keeps that shape:
 * {@code ingredients} is the bulk material the basin below holds, and {@code infusion} is the single
 * item the machine itself carries and presses into it.</p>
 *
 * <p>The split has to be in the recipe rather than inferred from the ingredient list, because the
 * two come from different places at match time - the basin is checked by Create's own
 * {@code BasinRecipe.match}, which knows nothing about an inventory on the machine.</p>
 */
public class CombinerRecipeParams extends ProcessingRecipeParams {

	public static final MapCodec<CombinerRecipeParams> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
		.group(
			Ingredient.CODEC.listOf()
				.fieldOf("ingredients")
				.forGetter(CombinerRecipeParams::itemIngredients),
			Ingredient.CODEC.fieldOf("infusion")
				.forGetter(CombinerRecipeParams::infusion),
			ProcessingOutput.CODEC_NEW.listOf()
				.fieldOf("results")
				.forGetter(CombinerRecipeParams::itemResults),
			Codec.INT.optionalFieldOf("processing_time", 100)
				.forGetter(CombinerRecipeParams::processingDuration))
		.apply(instance, (ingredients, infusion, results, duration) -> {
			CombinerRecipeParams params = new CombinerRecipeParams();
			params.ingredients.addAll(ingredients);
			params.infusion = infusion;
			params.results.addAll(results);
			params.processingDuration = duration;
			return params;
		}));

	public static final StreamCodec<RegistryFriendlyByteBuf, CombinerRecipeParams> STREAM_CODEC =
		streamCodec(CombinerRecipeParams::new);

	protected Ingredient infusion = Ingredient.EMPTY;

	protected CombinerRecipeParams() {
		super();
	}

	protected final Ingredient infusion() {
		return infusion;
	}

	private java.util.List<Ingredient> itemIngredients() {
		return java.util.List.copyOf(ingredients);
	}

	private java.util.List<ProcessingOutput> itemResults() {
		return java.util.List.copyOf(results);
	}

	@Override
	protected void encode(RegistryFriendlyByteBuf buffer) {
		super.encode(buffer);
		Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, infusion);
	}

	@Override
	protected void decode(RegistryFriendlyByteBuf buffer) {
		super.decode(buffer);
		infusion = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
	}
}
