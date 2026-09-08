package me.moonscenty.createkinetism.registry;

import java.util.Optional;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Serializer;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;

import me.moonscenty.createkinetism.CreateKinetism;
import me.moonscenty.createkinetism.content.boiler.ThermalBoilingRecipe;
import me.moonscenty.createkinetism.content.recipe.ActivatingRecipe;
import me.moonscenty.createkinetism.content.recipe.CentrifugingRecipe;
import me.moonscenty.createkinetism.content.recipe.ChemicalInfusingRecipe;
import me.moonscenty.createkinetism.content.recipe.CombinerRecipe;
import me.moonscenty.createkinetism.content.recipe.KinetiteCompressingRecipe;
import me.moonscenty.createkinetism.content.recipe.ConvertingRecipe;
import me.moonscenty.createkinetism.content.recipe.DistillingRecipe;
import me.moonscenty.createkinetism.content.recipe.EngineFuelRecipe;
import me.moonscenty.createkinetism.content.recipe.DecayingRecipe;
import me.moonscenty.createkinetism.content.recipe.CrystallizingRecipe;
import me.moonscenty.createkinetism.content.recipe.DissolvingRecipe;
import me.moonscenty.createkinetism.content.recipe.EnrichingRecipe;
import me.moonscenty.createkinetism.content.recipe.EvaporatingRecipe;
import me.moonscenty.createkinetism.content.recipe.InfusingRecipe;
import me.moonscenty.createkinetism.content.recipe.InjectingRecipe;
import me.moonscenty.createkinetism.content.recipe.NutritionBarCookingRecipe;
import me.moonscenty.createkinetism.content.recipe.OxidizingRecipe;
import me.moonscenty.createkinetism.content.recipe.ReactingRecipe;
import me.moonscenty.createkinetism.content.recipe.PumpjackRecipe;
import me.moonscenty.createkinetism.content.recipe.PurifyingRecipe;
import me.moonscenty.createkinetism.content.recipe.SeparatingRecipe;
import me.moonscenty.createkinetism.content.recipe.WashingRecipe;

import net.createmod.catnip.lang.Lang;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Recipe types for every Kinetism machine, modelled one-to-one on Create's {@code AllRecipeTypes}.
 *
 * <p>All of them are Create {@code ProcessingRecipe}s, so their JSON shape is identical to
 * {@code create:milling} / {@code create:mixing}: an {@code ingredients} list mixing item and fluid
 * ingredients, a {@code results} list mixing item and fluid results, plus {@code processing_time}
 * and (where supported) {@code heat_requirement}.</p>
 */
public enum CKRecipeTypes implements IRecipeTypeInfo, StringRepresentable {

	// item -> item, standalone kinetic machines.
	// Crushing and sawing are deliberately absent: Create already ships Crushing Wheels and the
	// Mechanical Saw, so those steps of the Mekanism chain run on Create machines via datapack.
	ENRICHING(EnrichingRecipe::new),
	COMBINING(CombinerRecipe::new),
	INFUSING(InfusingRecipe.Serializer::new),
	// Kinetite Compressor: a target item plus Kinetite, pressed together. Mekanism's Osmium
	// Compressor, with the metal that took osmium's place here.
	KINETITE_COMPRESSING(KinetiteCompressingRecipe::new),
	// The Metallurgic Infuser's own infusion slot: a solid into the Mekanism chemical it stands
	// for. Both of these carry a chemical, so neither is a Create ProcessingRecipe.
	CONVERTING(ConvertingRecipe.Serializer::new),

	// basin machines, item and/or fluid in, item and/or fluid out
	PURIFYING(PurifyingRecipe.Serializer::new),
	INJECTING(InjectingRecipe.Serializer::new),
	DISSOLVING(DissolvingRecipe.Serializer::new),
	WASHING(WashingRecipe.Serializer::new),
	CRYSTALLIZING(CrystallizingRecipe::new),
	OXIDIZING(OxidizingRecipe::new),
	CHEMICAL_INFUSING(ChemicalInfusingRecipe::new),
	SEPARATING(SeparatingRecipe::new),
	// Pressurized Reaction Chamber: the chamber holds the fluid, the basin the item and the gas.
	REACTING(ReactingRecipe::new),
	// Solar Neutron Activator: gas to gas, and the only machine here that runs on daylight
	// instead of rotation.
	ACTIVATING(ActivatingRecipe::new),
	// Nutrition Bar Mixer. Ships empty on purpose - the machine reads a food's nutrition instead,
	// and this type is only here so a pack can name an exception.
	NUTRITION_BAR_COOKING(NutritionBarCookingRecipe::new),
	// Isotopic Centrifuge: gas to gas, on the machine that carries its own basin.
	CENTRIFUGING(CentrifugingRecipe::new),
	// Radioactive Waste Drum: what rots, how fast, and into what if anything.
	DECAYING(DecayingRecipe::new),
	EVAPORATING(EvaporatingRecipe::new),
	// Thermal Boiler Tank in boiler mode: floor 1 holds the feed, everything above holds the product.
	THERMAL_BOILING(ThermalBoilingRecipe::new),

	// oil chain, ported from Petrochem - see LICENSE-THIRD-PARTY.md
	PUMPJACK(PumpjackRecipe::new),
	DISTILLING(DistillingRecipe::new),
	GASOLINE_ENGINE_FUEL(EngineFuelRecipe::gasoline),
	DIESEL_ENGINE_FUEL(EngineFuelRecipe::diesel),
	TURBINE_FUEL(EngineFuelRecipe::turbine);

	public final ResourceLocation id;

	private final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> serializerObject;
	private final DeferredHolder<RecipeType<?>, RecipeType<?>> typeObject;

	CKRecipeTypes(Supplier<RecipeSerializer<?>> serializerSupplier) {
		String name = Lang.asId(name());
		id = CreateKinetism.asResource(name);
		serializerObject = Registers.SERIALIZERS.register(name, serializerSupplier);
		typeObject = Registers.TYPES.register(name, () -> RecipeType.simple(id));
	}

	CKRecipeTypes(StandardProcessingRecipe.Factory<?> processingFactory) {
		this(() -> new Serializer<>(processingFactory));
	}

	CKRecipeTypes(PumpjackRecipe.Factory<?> pumpjackFactory) {
		this(() -> new PumpjackRecipe.Serializer<>(pumpjackFactory));
	}

	CKRecipeTypes(EngineFuelRecipe.Factory<?> engineFuelFactory) {
		this(() -> new EngineFuelRecipe.Serializer<>(engineFuelFactory));
	}

	CKRecipeTypes(CombinerRecipe.Factory<?> combinerFactory) {
		this(() -> new CombinerRecipe.Serializer<>(combinerFactory));
	}

	CKRecipeTypes(DistillingRecipe.Factory<?> distillingFactory) {
		this(() -> new DistillingRecipe.Serializer<>(distillingFactory));
	}

	public static void register(IEventBus modEventBus) {
		Registers.SERIALIZERS.register(modEventBus);
		Registers.TYPES.register(modEventBus);
	}

	@Override
	public ResourceLocation getId() {
		return id;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends RecipeSerializer<?>> T getSerializer() {
		return (T) serializerObject.get();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <I extends RecipeInput, R extends Recipe<I>> RecipeType<R> getType() {
		return (RecipeType<R>) typeObject.get();
	}

	public <I extends RecipeInput, R extends Recipe<I>> Optional<RecipeHolder<R>> find(I inv, Level level) {
		return level.getRecipeManager()
			.getRecipeFor(getType(), inv, level);
	}

	@Override
	public @NotNull String getSerializedName() {
		return id.toString();
	}

	private static class Registers {
		private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
			DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, CreateKinetism.ID);
		private static final DeferredRegister<RecipeType<?>> TYPES =
			DeferredRegister.create(Registries.RECIPE_TYPE, CreateKinetism.ID);
	}
}
