package me.moonscenty.createkinetism.content.boiler;

import com.google.common.base.Joiner;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import mekanism.api.chemical.ChemicalStack;

import me.moonscenty.createkinetism.content.heat.CKHeatLevels;
import me.moonscenty.createkinetism.content.recipe.VatRecipe;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Thermal Boiler Tank, in boiler mode: water into steam, sodium into superheated sodium.
 *
 * <p>Mekanism's real Steam is a gas, not a liquid - the Thermoelectric Boiler holds it in a chemical
 * tank, same as Hydrogen or Oxygen. So the product here is a {@link ChemicalStack}
 * ({@code chemical_result} in the JSON) rather than a fluid result: {@link ThermalBoilerTankBlockEntity}
 * hands it out through a Mekanism chemical capability the same way
 * {@link me.moonscenty.createkinetism.content.vat.MechanicalElectrolyzerBlockEntity} does, not by
 * filling its own tank with a liquid-flavoured stand-in.</p>
 *
 * <p>{@code heat_requirement} is Create's own {@link HeatCondition}, with the two levels this mod adds
 * to it (see {@link CKHeatLevels}): {@code "sodium_heated"} is the heat of a Sodium Burner spinning its
 * shaft (see {@link SodiumBurnerBlockEntity#heatLevel()}), past anything a Blaze Burner reaches.
 * {@code "chilled"} makes no sense for a boiler and is refused when the recipe loads.</p>
 *
 * <p>{@code rate} is the actual mB/t a single qualifying heater drives this recipe at - a number of
 * its own, not something read back out of {@code ingredient.amount() / processing_time}. That let
 * every tier keep the same honest {@code 1 mB -> 1 mB} ratio in JEI instead of the ratio itself being
 * inflated to 2000 or 4000 just so the numbers alone would hint at which recipe was faster.</p>
 */
public class ThermalBoilingRecipe extends VatRecipe {

	private final ChemicalStack chemicalResult;
	private final int rate;

	public ThermalBoilingRecipe(ProcessingRecipeParams params, ChemicalStack chemicalResult, int rate) {
		super(CKRecipeTypes.THERMAL_BOILING, params);
		this.chemicalResult = chemicalResult;
		this.rate = rate;
	}

	@Override
	protected boolean canRequireHeat() {
		return true;
	}

	/** No liquid output at all - the product is a gas, see {@link #getChemicalResult()}. */
	@Override
	protected int getMaxFluidOutputCount() {
		return 0;
	}

	/** A fresh copy each time - the caller inserts it into a chemical tank, which would otherwise alias. */
	public ChemicalStack getChemicalResult() {
		return chemicalResult.copy();
	}

	/** The stored stack itself, for the codec and anything that only wants to look at it. */
	public ChemicalStack chemicalResult() {
		return chemicalResult;
	}

	/**
	 * The tier a Thermal Boiler Tank compares its heaters against - {@code heat_requirement} as a
	 * number, see {@link CKHeatLevels#tierOf}.
	 */
	public int getMinimumTier() {
		return CKHeatLevels.tierOf(getRequiredHeat());
	}

	/** mB a single qualifying heater converts per tick - see the class doc. */
	public int getRate() {
		return rate;
	}

	/** Create's own recipe codec with two fields added - see {@code SeparatingRecipe.Serializer}. */
	public static class Serializer implements RecipeSerializer<ThermalBoilingRecipe> {

		private static final MapCodec<ThermalBoilingRecipe> CODEC =
			RecordCodecBuilder.<ThermalBoilingRecipe>mapCodec(instance -> instance.group(
				ProcessingRecipeParams.CODEC.forGetter(ProcessingRecipe::getParams),
				ChemicalStack.CODEC.fieldOf("chemical_result")
					.forGetter(ThermalBoilingRecipe::chemicalResult),
				Codec.INT.fieldOf("rate")
					.forGetter(ThermalBoilingRecipe::getRate))
				.apply(instance, ThermalBoilingRecipe::new))
				.validate(recipe -> {
					var errors = recipe.validate();
					if (recipe.chemicalResult()
						.isEmpty())
						errors.add("chemical_result must not be empty");
					if (recipe.getRate() <= 0)
						errors.add("rate must be positive, was " + recipe.getRate());
					if (recipe.getRequiredHeat() == CKHeatLevels.CHILLED_CONDITION)
						errors.add("a boiler cannot require chilled heat");
					if (errors.isEmpty())
						return DataResult.success(recipe);
					errors.add(recipe.getClass()
						.getSimpleName() + " failed validation:");
					return DataResult.error(() -> Joiner.on('\n')
						.join(errors), recipe);
				});

		private static final StreamCodec<RegistryFriendlyByteBuf, ThermalBoilingRecipe> STREAM_CODEC =
			StreamCodec.composite(ProcessingRecipeParams.STREAM_CODEC, ProcessingRecipe::getParams,
				ChemicalStack.STREAM_CODEC, ThermalBoilingRecipe::chemicalResult,
				ByteBufCodecs.VAR_INT, ThermalBoilingRecipe::getRate,
				ThermalBoilingRecipe::new);

		@Override
		public MapCodec<ThermalBoilingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, ThermalBoilingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
