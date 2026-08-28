package me.moonscenty.createkinetism.content.recipe;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md, though what a recipe carries
 * has grown since: Petrochem's engines only needed to know how fast a fuel burned, because the
 * energy they paid out was a flat config number. Ours have to know what the fuel is worth as
 * rotation too - see {@link EngineFuelRecipeParams}.
 *
 * <p>One fluid ingredient, a duration, and the two output figures. Burn rate falls out of the first
 * two: an engine burns {@code amount / processing_time} millibuckets per tick, so a fuel with the
 * same amount over a longer time is simply more efficient.</p>
 *
 * <p>Each engine has its own recipe type, so a datapack can teach one engine a fuel without teaching
 * it to all of them - and the same fluid can be worth different things in different engines.</p>
 */
public class EngineFuelRecipe extends ProcessingRecipe<RecipeInput, EngineFuelRecipeParams> {

	private final double stress;
	private final int rpm;

	public EngineFuelRecipe(IRecipeTypeInfo typeInfo, EngineFuelRecipeParams params) {
		super(typeInfo, params);
		this.stress = params.stress();
		this.rpm = params.rpm();
	}

	public static EngineFuelRecipe gasoline(EngineFuelRecipeParams params) {
		return new EngineFuelRecipe(CKRecipeTypes.GASOLINE_ENGINE_FUEL, params);
	}

	public static EngineFuelRecipe diesel(EngineFuelRecipeParams params) {
		return new EngineFuelRecipe(CKRecipeTypes.DIESEL_ENGINE_FUEL, params);
	}

	public static EngineFuelRecipe turbine(EngineFuelRecipeParams params) {
		return new EngineFuelRecipe(CKRecipeTypes.TURBINE_FUEL, params);
	}

	public boolean match(FluidStack fuel) {
		return getFluidIngredients().getFirst()
			.test(fuel);
	}

	/** Millibuckets burned per tick, at full load. */
	public float getConsumptionRate() {
		return (float) getFluidIngredients().getFirst()
			.amount() / (float) getProcessingDuration();
	}

	/** Stress units the engine supplies while burning this. */
	public float getStress() {
		return (float) stress;
	}

	/** The speed the engine turns at on this fuel. Always positive; the dial decides the sign. */
	public int getRpm() {
		return Math.abs(rpm);
	}

	@Override
	protected boolean canSpecifyDuration() {
		return true;
	}

	@Override
	protected int getMaxFluidInputCount() {
		return 1;
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
	public boolean matches(RecipeInput recipeInput, Level level) {
		return false;
	}

	@FunctionalInterface
	public interface Factory<R extends EngineFuelRecipe>
		extends ProcessingRecipe.Factory<EngineFuelRecipeParams, R> {
		R create(EngineFuelRecipeParams params);
	}

	public static class Serializer<R extends EngineFuelRecipe> implements RecipeSerializer<R> {

		private final MapCodec<R> codec;
		private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

		public Serializer(ProcessingRecipe.Factory<EngineFuelRecipeParams, R> factory) {
			this.codec = ProcessingRecipe.codec(factory, EngineFuelRecipeParams.CODEC);
			this.streamCodec = ProcessingRecipe.streamCodec(factory, EngineFuelRecipeParams.STREAM_CODEC);
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
