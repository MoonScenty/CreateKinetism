package me.moonscenty.createkinetism.content.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>A pumpjack recipe is just "this biome yields this fluid", so the only thing it adds to Create's
 * processing params is a biome id. A leading {@code #} makes it a biome tag.</p>
 */
public class PumpjackRecipeParams extends ProcessingRecipeParams {

	public static final MapCodec<PumpjackRecipeParams> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Codec.either(FluidStack.CODEC, ProcessingOutput.CODEC)
			.listOf()
			.fieldOf("results")
			.forGetter(PumpjackRecipeParams::results),
		Codec.STRING.optionalFieldOf("biome", "minecraft:void")
			.forGetter(PumpjackRecipeParams::biome)
	).apply(instance, (results, biome) -> {
		PumpjackRecipeParams params = new PumpjackRecipeParams();
		params.biome = biome;
		results.forEach(either -> either.ifRight(params.results::add)
			.ifLeft(params.fluidResults::add));
		return params;
	}));

	public static final StreamCodec<RegistryFriendlyByteBuf, PumpjackRecipeParams> STREAM_CODEC =
		streamCodec(PumpjackRecipeParams::new);

	protected String biome;

	protected PumpjackRecipeParams() {
		super();
		biome = "";
	}

	protected final String biome() {
		return biome;
	}

	@Override
	protected void encode(RegistryFriendlyByteBuf buffer) {
		super.encode(buffer);
		ByteBufCodecs.STRING_UTF8.encode(buffer, biome);
	}

	@Override
	protected void decode(RegistryFriendlyByteBuf buffer) {
		super.decode(buffer);
		biome = ByteBufCodecs.STRING_UTF8.decode(buffer);
	}
}
