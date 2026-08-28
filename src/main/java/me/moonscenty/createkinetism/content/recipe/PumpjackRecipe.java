package me.moonscenty.createkinetism.content.recipe;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Which fluid a well brings up, keyed on the biome it was sunk in. A {@code biome} value starting
 * with {@code #} is read as a biome tag.</p>
 */
public class PumpjackRecipe extends ProcessingRecipe<RecipeInput, PumpjackRecipeParams> {

	private final ResourceKey<Biome> biome;
	private final TagKey<Biome> biomeTag;

	public PumpjackRecipe(PumpjackRecipeParams params) {
		super(CKRecipeTypes.PUMPJACK, params);
		String id = params.biome();
		if (id.startsWith("#")) {
			biomeTag = TagKey.create(Registries.BIOME, ResourceLocation.parse(id.substring(1)));
			biome = null;
		} else {
			biomeTag = null;
			biome = ResourceKey.create(Registries.BIOME, ResourceLocation.parse(id));
		}
	}

	public boolean matchesBiome(Holder<Biome> here) {
		return biomeTag != null ? here.is(biomeTag) : here.is(biome);
	}

	public FluidStack getFluidResult() {
		return getFluidResults().get(0);
	}

	@Override
	protected int getMaxInputCount() {
		return 0;
	}

	@Override
	protected int getMaxOutputCount() {
		return 0;
	}

	@Override
	protected int getMaxFluidOutputCount() {
		return 1;
	}

	@Override
	public boolean matches(RecipeInput recipeInput, Level level) {
		return false;
	}

	@FunctionalInterface
	public interface Factory<R extends PumpjackRecipe> extends ProcessingRecipe.Factory<PumpjackRecipeParams, R> {
		R create(PumpjackRecipeParams params);
	}

	public static class Serializer<R extends PumpjackRecipe> implements RecipeSerializer<R> {

		private final MapCodec<R> codec;
		private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

		public Serializer(ProcessingRecipe.Factory<PumpjackRecipeParams, R> factory) {
			this.codec = ProcessingRecipe.codec(factory, PumpjackRecipeParams.CODEC);
			this.streamCodec = ProcessingRecipe.streamCodec(factory, PumpjackRecipeParams.STREAM_CODEC);
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
