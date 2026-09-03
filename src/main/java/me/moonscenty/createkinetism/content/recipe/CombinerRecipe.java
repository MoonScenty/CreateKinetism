package me.moonscenty.createkinetism.content.recipe;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Combiner: bulk material in the basin, one infusion item held by the machine.
 *
 * <p>Mekanism's Combiner has two slots that mean different things - the material, and the thing
 * worked into it - and this keeps them apart rather than pouring both into the basin. Eight
 * cobblestone sit below; the dust is carried by the Combiner and pressed down into them.</p>
 *
 * <p>It deliberately does <em>not</em> extend {@link BasinRecipe}. That class pins its params type,
 * which leaves nowhere to put the infusion ingredient. Nothing is lost by skipping it: Create's
 * {@code BasinRecipe.match} and {@code apply} are static and take any {@code Recipe<?>}, so the
 * basin half still goes through Create's own code. The infusion half is checked by the block entity,
 * the only thing that can see the machine's own inventory.</p>
 */
public class CombinerRecipe extends ProcessingRecipe<RecipeInput, CombinerRecipeParams> {

	private final Ingredient infusion;

	public CombinerRecipe(CombinerRecipeParams params) {
		super(CKRecipeTypes.COMBINING, params);
		this.infusion = params.infusion();
	}

	/** The item the machine has to be holding. Never empty - the codec requires the field. */
	public Ingredient getInfusion() {
		return infusion;
	}

	public boolean matchesInfusion(ItemStack held) {
		return !held.isEmpty() && infusion.test(held);
	}

	/** Eight, not nine: a basin has nine slots but the ninth item is not the basin's to give. */
	@Override
	protected int getMaxInputCount() {
		return 8;
	}

	@Override
	protected int getMaxOutputCount() {
		return 2;
	}

	@Override
	protected int getMaxFluidInputCount() {
		return 0;
	}

	@Override
	protected int getMaxFluidOutputCount() {
		return 0;
	}

	/** Combining is cold; no blaze burner involved. */
	@Override
	protected boolean canRequireHeat() {
		return false;
	}

	/**
	 * Without this the recipe refuses to load at all: {@code ProcessingRecipe.validate} rejects a
	 * {@code processing_time} on any type that has not opted in, and every combining recipe sets one.
	 */
	@Override
	protected boolean canSpecifyDuration() {
		return true;
	}

	/** Basin machines match through {@code BasinRecipe.match}, never through a RecipeInput. */
	@Override
	public boolean matches(RecipeInput recipeInput, Level level) {
		return false;
	}

	@FunctionalInterface
	public interface Factory<R extends CombinerRecipe> extends ProcessingRecipe.Factory<CombinerRecipeParams, R> {
		R create(CombinerRecipeParams params);
	}

	public static class Serializer<R extends CombinerRecipe> implements RecipeSerializer<R> {

		private final MapCodec<R> codec;
		private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

		public Serializer(ProcessingRecipe.Factory<CombinerRecipeParams, R> factory) {
			this.codec = ProcessingRecipe.codec(factory, CombinerRecipeParams.CODEC);
			this.streamCodec = ProcessingRecipe.streamCodec(factory, CombinerRecipeParams.STREAM_CODEC);
		}

		@Override
		public MapCodec<R> codec() {
			return codec;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
			return streamCodec;
		}
	}
}
