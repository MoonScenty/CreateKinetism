package me.moonscenty.createkinetism.content.recipe;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.foundation.codec.CreateCodecs;

import mekanism.api.recipes.ingredients.ChemicalStackIngredient;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * What a fuel is worth to an engine, on top of Create's processing params.
 *
 * <p>Petrochem could leave this out because its engines pay out Forge Energy, which is the same
 * number whatever you burn - only efficiency varied. Rotation has two dimensions, so a fuel here
 * has to say both how hard it pushes and how fast:</p>
 *
 * <ul>
 * <li>{@code stress} - stress units the engine supplies while running on this fuel.</li>
 * <li>{@code rpm} - the speed it runs at. The engine no longer lets the player dial this; the fuel
 * decides it, and the dial only picks which way round the shaft turns.</li>
 * </ul>
 *
 * <p>Burn rate is still {@code ingredient amount / processing_time} millibuckets per tick, so
 * fractional rates are written as a small amount over a long time: {@code amount 1} over
 * {@code processing_time 10} is 0.1 mB/t.</p>
 *
 * <p>Together these are what let one engine treat two fuels completely differently - thin, cheap
 * steam against dense LPG - which a single config number per block could never express.</p>
 *
 * <p>A fuel is either a fluid or a chemical, never both. The two liquid engines take
 * {@code ingredients}; the Gas Turbine takes {@code chemical_input} instead, because the gases it
 * burns are Mekanism chemicals and travel in tubes rather than pipes. Both fields are optional so
 * neither engine has to write an empty list for the half it does not use.</p>
 */
public class EngineFuelRecipeParams extends ProcessingRecipeParams {

	public static final MapCodec<EngineFuelRecipeParams> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
		.group(
			Codec.either(CreateCodecs.FLAT_SIZED_FLUID_INGREDIENT_WITH_TYPE, Ingredient.CODEC)
				.listOf()
				.optionalFieldOf("ingredients", List.of())
				.forGetter(EngineFuelRecipeParams::ingredients),
			ChemicalStackIngredient.CODEC.optionalFieldOf("chemical_input")
				.forGetter(EngineFuelRecipeParams::chemicalInput),
			Codec.INT.optionalFieldOf("processing_time", 1)
				.forGetter(EngineFuelRecipeParams::processingDuration),
			Codec.DOUBLE.fieldOf("stress")
				.forGetter(EngineFuelRecipeParams::stress),
			Codec.INT.fieldOf("rpm")
				.forGetter(EngineFuelRecipeParams::rpm))
		.apply(instance, (ingredients, chemical, duration, stress, rpm) -> {
			EngineFuelRecipeParams params = new EngineFuelRecipeParams();
			ingredients.forEach(either -> either.ifRight(params.ingredients::add)
				.ifLeft(params.fluidIngredients::add));
			params.chemicalInput = chemical;
			params.processingDuration = Math.max(1, duration);
			params.stress = stress;
			params.rpm = rpm;
			return params;
		}));

	public static final StreamCodec<RegistryFriendlyByteBuf, EngineFuelRecipeParams> STREAM_CODEC =
		streamCodec(EngineFuelRecipeParams::new);

	protected double stress;
	protected int rpm;
	protected Optional<ChemicalStackIngredient> chemicalInput = Optional.empty();

	protected EngineFuelRecipeParams() {
		super();
		stress = 0;
		rpm = 0;
	}

	protected final double stress() {
		return stress;
	}

	protected final Optional<ChemicalStackIngredient> chemicalInput() {
		return chemicalInput;
	}

	protected final int rpm() {
		return rpm;
	}

	@Override
	protected void encode(RegistryFriendlyByteBuf buffer) {
		super.encode(buffer);
		ByteBufCodecs.DOUBLE.encode(buffer, stress);
		ByteBufCodecs.VAR_INT.encode(buffer, rpm);
		buffer.writeBoolean(chemicalInput.isPresent());
		chemicalInput.ifPresent(ingredient -> ChemicalStackIngredient.STREAM_CODEC.encode(buffer, ingredient));
	}

	@Override
	protected void decode(RegistryFriendlyByteBuf buffer) {
		super.decode(buffer);
		stress = ByteBufCodecs.DOUBLE.decode(buffer);
		rpm = ByteBufCodecs.VAR_INT.decode(buffer);
		chemicalInput = buffer.readBoolean()
			? Optional.of(ChemicalStackIngredient.STREAM_CODEC.decode(buffer))
			: Optional.empty();
	}
}
