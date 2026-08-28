package me.moonscenty.createkinetism.content.recipe;

import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Adds the distillation mode - flash, atmospheric or vacuum - to Create's processing params.
 * The mode decides what the column needs to run: steam, heat, or heat plus a pumped-out column.</p>
 */
public class DistillationRecipeParams extends ProcessingRecipeParams {

	public static final MapCodec<DistillationRecipeParams> CODEC =
		RecordCodecBuilder.mapCodec(instance -> instance.group(
			codec(DistillationRecipeParams::new).forGetter(Function.identity()),
			Codec.STRING.fieldOf("mode")
				.forGetter(DistillationRecipeParams::mode)
		).apply(instance, (params, mode) -> {
			params.mode = mode;
			return params;
		}));

	public static final StreamCodec<RegistryFriendlyByteBuf, DistillationRecipeParams> STREAM_CODEC =
		streamCodec(DistillationRecipeParams::new);

	protected String mode = "";

	protected final String mode() {
		return mode;
	}

	@Override
	protected void encode(RegistryFriendlyByteBuf buffer) {
		super.encode(buffer);
		ByteBufCodecs.STRING_UTF8.encode(buffer, mode);
	}

	@Override
	protected void decode(RegistryFriendlyByteBuf buffer) {
		super.decode(buffer);
		mode = ByteBufCodecs.STRING_UTF8.decode(buffer);
	}
}
