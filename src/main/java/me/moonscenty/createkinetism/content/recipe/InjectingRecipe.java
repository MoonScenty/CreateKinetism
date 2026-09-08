package me.moonscenty.createkinetism.content.recipe;

import com.google.common.base.Joiner;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Mekanism: Chemical Injection Chamber. One item, one chemical, one item out - the 4x ore step.
 *
 * <p>Half Create and half Mekanism, because the machine is. The item side goes through a basin, so
 * that half stays a Create {@link ProcessingRecipe} and Create's own basin matching keeps working
 * unchanged. The chemical is a Mekanism {@link ChemicalStack} held in the chamber's own tank, so it
 * is bolted on beside the params rather than squeezed into them - {@code ProcessingRecipeParams}
 * knows items and fluids and nothing else.</p>
 *
 * <pre>{@code
 * {
 *   "type": "createkinetism:injecting",
 *   "ingredients": [ { "tag": "c:ores/copper" } ],
 *   "chemical_input": { "chemical": "mekanism:hydrogen_chloride", "amount": 1 },
 *   "results": [ { "id": "mekanism:shard_copper", "count": 4 } ],
 *   "processing_time": 100
 * }
 * }</pre>
 *
 * <p>Hydrogen chloride does have a fluid form, so this used to be a plain fluid ingredient and a
 * Create pipe could feed the chamber. Water vapour, which most of Mekanism's injection recipes want,
 * does not - and a machine that took its gas two different ways depending on which recipe was running
 * would be worse than one that always wants a pressurized tube.</p>
 */
public class InjectingRecipe extends StandardProcessingRecipe<RecipeInput> {

	private final ChemicalStackIngredient chemicalInput;

	public InjectingRecipe(ProcessingRecipeParams params, ChemicalStackIngredient chemicalInput) {
		super(CKRecipeTypes.INJECTING, params);
		this.chemicalInput = chemicalInput;
	}

	/**
	 * Only the item is checked here. The chemical is not part of the basin the machine hands us, so it
	 * is matched separately against our own tank - see {@code InjectionChamberBlockEntity}.
	 */
	@Override
	public boolean matches(RecipeInput inv, Level level) {
		return !inv.isEmpty() && ingredients.get(0)
			.test(inv.getItem(0));
	}

	public ChemicalStackIngredient getRequiredChemical() {
		return chemicalInput;
	}

	/**
	 * Whether this recipe can be paid for out of the given tank.
	 *
	 * <p>{@code ChemicalStackIngredient.test} checks the amount as well as the type, which is what is
	 * wanted: a tank holding half the cost does not satisfy the recipe.</p>
	 */
	public boolean matchesChemical(ChemicalStack available) {
		return chemicalInput.test(available);
	}

	public long getRequiredAmount() {
		return chemicalInput.amount();
	}

	public ItemStack getResultItem() {
		return getRollableResults().get(0)
			.getStack();
	}

	/**
	 * Create spells "three raw copper" as the same ingredient listed three times, and the ore chain's
	 * raw-ore step needs exactly that, so this is not one.
	 */
	@Override
	protected int getMaxInputCount() {
		return 9;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

	/** The chemical is not one of these; it lives beside the params, not in them. */
	@Override
	protected int getMaxFluidInputCount() {
		return 0;
	}

	@Override
	protected boolean canSpecifyDuration() {
		return true;
	}

	/**
	 * Create's own recipe codec with one field added.
	 *
	 * <p>{@link ProcessingRecipe#codec} cannot be reused as-is because it maps a params codec straight
	 * onto a one-argument factory. The body here is the same thing widened to two: Create's params
	 * codec unchanged, our chemical beside it, and Create's validation on the end so a recipe with too
	 * many ingredients still fails to load rather than half-working.</p>
	 */
	public static class Serializer implements RecipeSerializer<InjectingRecipe> {

		private static final MapCodec<InjectingRecipe> CODEC =
			RecordCodecBuilder.<InjectingRecipe>mapCodec(instance -> instance.group(
				ProcessingRecipeParams.CODEC.forGetter(ProcessingRecipe::getParams),
				ChemicalStackIngredient.CODEC.fieldOf("chemical_input")
					.forGetter(InjectingRecipe::getRequiredChemical))
				.apply(instance, InjectingRecipe::new))
				.validate(recipe -> {
					var errors = recipe.validate();
					if (errors.isEmpty())
						return DataResult.success(recipe);
					errors.add(recipe.getClass()
						.getSimpleName() + " failed validation:");
					return DataResult.error(() -> Joiner.on('\n')
						.join(errors), recipe);
				});

		private static final StreamCodec<RegistryFriendlyByteBuf, InjectingRecipe> STREAM_CODEC =
			StreamCodec.composite(ProcessingRecipeParams.STREAM_CODEC, ProcessingRecipe::getParams,
				ChemicalStackIngredient.STREAM_CODEC, InjectingRecipe::getRequiredChemical,
				InjectingRecipe::new);

		@Override
		public MapCodec<InjectingRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, InjectingRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
