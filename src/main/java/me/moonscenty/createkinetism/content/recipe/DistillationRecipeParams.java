package me.moonscenty.createkinetism.content.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import mekanism.api.chemical.ChemicalStack;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Adds two things to Create's processing params: the distillation mode - flash, atmospheric or
 * vacuum, which decides what the column needs to run - and a list of chemical cuts.</p>
 *
 * <p>The chemicals are a second list rather than mixed into {@code results} because that field is
 * built by Create's own params codec and takes fluids and items only. Keeping them apart costs
 * nothing here: a column pulls its lightest cut off the top, gases are the lightest thing it makes,
 * so the chemical cuts always belong at the end of the run anyway. Stage numbering follows -
 * fluids take stages 1..n, chemicals n+1 onward.</p>
 */
public class DistillationRecipeParams extends ProcessingRecipeParams {

	public static final MapCodec<DistillationRecipeParams> CODEC =
		RecordCodecBuilder.mapCodec(instance -> instance.group(
			codec(DistillationRecipeParams::new).forGetter(Function.identity()),
			Codec.STRING.fieldOf("mode")
				.forGetter(DistillationRecipeParams::mode),
			ChemicalStack.CODEC.listOf()
				.optionalFieldOf("chemical_results", List.of())
				.forGetter(DistillationRecipeParams::chemicalResults)
		).apply(instance, (params, mode, chemicals) -> {
			params.mode = mode;
			params.chemicalResults = chemicals;
			return params;
		}));

	public static final StreamCodec<RegistryFriendlyByteBuf, DistillationRecipeParams> STREAM_CODEC =
		streamCodec(DistillationRecipeParams::new);

	protected String mode = "";
	protected List<ChemicalStack> chemicalResults = List.of();

	protected final String mode() {
		return mode;
	}

	protected final List<ChemicalStack> chemicalResults() {
		return chemicalResults;
	}

	@Override
	protected void encode(RegistryFriendlyByteBuf buffer) {
		super.encode(buffer);
		ByteBufCodecs.STRING_UTF8.encode(buffer, mode);
		buffer.writeVarInt(chemicalResults.size());
		for (ChemicalStack stack : chemicalResults)
			ChemicalStack.STREAM_CODEC.encode(buffer, stack);
	}

	@Override
	protected void decode(RegistryFriendlyByteBuf buffer) {
		super.decode(buffer);
		mode = ByteBufCodecs.STRING_UTF8.decode(buffer);
		int count = buffer.readVarInt();
		List<ChemicalStack> decoded = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
			decoded.add(ChemicalStack.STREAM_CODEC.decode(buffer));
		chemicalResults = List.copyOf(decoded);
	}
}
