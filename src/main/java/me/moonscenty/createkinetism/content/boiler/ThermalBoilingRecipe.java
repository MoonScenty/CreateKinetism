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
 * <p>{@code heat_requirement} is still Create's own {@link HeatCondition}, untouched - NONE, HEATED or
 * SUPERHEATED, same as every other Create recipe, and everything that only knows about those three
 * (goggle text, JEI) keeps reading it exactly as before.</p>
 *
 * <p>A Sodium Burner spinning its shaft reports a heat past SUPERHEATED that Create has no name for
 * (see {@link SodiumBurnerBlockEntity#heatLevel()}). Rather than inventing a fourth
 * {@code HeatCondition}, a recipe that wants specifically that heat says so with the optional
 * {@code minimum_tier} field - a plain number only {@link ThermalBoilerTankBlockEntity} ever looks
 * at, layered on top of the named condition rather than replacing it. See {@link #getMinimumTier()}.
 * </p>
 *
 * <p>{@code rate} is the actual mB/t a single qualifying heater drives this recipe at - a number of
 * its own, not something read back out of {@code ingredient.amount() / processing_time}. That let
 * every tier keep the same honest {@code 1 mB -> 1 mB} ratio in JEI instead of the ratio itself being
 * inflated to 2000 or 4000 just so the numbers alone would hint at which recipe was faster.</p>
 */
public class ThermalBoilingRecipe extends VatRecipe {

	private final int minimumTierOverride;
	private final ChemicalStack chemicalResult;
	private final int rate;

	public ThermalBoilingRecipe(ProcessingRecipeParams params, int minimumTierOverride, ChemicalStack chemicalResult,
		int rate) {
		super(CKRecipeTypes.THERMAL_BOILING, params);
		this.minimumTierOverride = minimumTierOverride;
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

	private int minimumTierOverride() {
		return minimumTierOverride;
	}

	/**
	 * The tier a Thermal Boiler Tank actually compares its heaters against: {@code heat_requirement}'s
	 * own tier (NONE/HEATED/SUPERHEATED = 0/1/2), or {@code minimum_tier} if that asks for more.
	 */
	public int getMinimumTier() {
		return Math.max(minimumTierOverride, tierOf(getRequiredHeat()));
	}

	static int tierOf(HeatCondition condition) {
		return condition == HeatCondition.SUPERHEATED ? 2 : condition == HeatCondition.HEATED ? 1 : 0;
	}

	/** mB a single qualifying heater converts per tick - see the class doc. */
	public int getRate() {
		return rate;
	}

	/** Create's own recipe codec with three fields added - see {@code SeparatingRecipe.Serializer}. */
	public static class Serializer implements RecipeSerializer<ThermalBoilingRecipe> {

		private static final MapCodec<ThermalBoilingRecipe> CODEC =
			RecordCodecBuilder.<ThermalBoilingRecipe>mapCodec(instance -> instance.group(
				ProcessingRecipeParams.CODEC.forGetter(ProcessingRecipe::getParams),
				Codec.INT.optionalFieldOf("minimum_tier", 0)
					.forGetter(ThermalBoilingRecipe::minimumTierOverride),
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
					if (errors.isEmpty())
						return DataResult.success(recipe);
					errors.add(recipe.getClass()
						.getSimpleName() + " failed validation:");
					return DataResult.error(() -> Joiner.on('\n')
						.join(errors), recipe);
				});

		private static final StreamCodec<RegistryFriendlyByteBuf, ThermalBoilingRecipe> STREAM_CODEC =
			StreamCodec.composite(ProcessingRecipeParams.STREAM_CODEC, ProcessingRecipe::getParams,
				ByteBufCodecs.VAR_INT, ThermalBoilingRecipe::minimumTierOverride,
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
