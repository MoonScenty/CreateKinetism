package me.moonscenty.createkinetism.registry;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.fluids.tank.FluidTankMovementBehavior;
import com.simibubi.create.content.processing.AssemblyOperatorBlockItem;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import me.moonscenty.createkinetism.CreateKinetism;
import me.moonscenty.createkinetism.config.CKStress;
import me.moonscenty.createkinetism.content.accumulator.KineticAccumulatorBlock;
import me.moonscenty.createkinetism.content.chamber.ChamberBlock;
import me.moonscenty.createkinetism.content.vibrator.PurificationVibratorBlock;
import me.moonscenty.createkinetism.content.infuser.MechanicalInfuserBlock;
import me.moonscenty.createkinetism.content.injection.InjectionChamberBlock;
import me.moonscenty.createkinetism.content.chamber.MechanicalEnricherBlock;
import me.moonscenty.createkinetism.content.oil.DistillationControllerBlock;
import me.moonscenty.createkinetism.content.oil.FlarestackBlock;
import me.moonscenty.createkinetism.content.oil.FuelEngineBlock;
import me.moonscenty.createkinetism.content.oil.GasTurbineBlock;
import me.moonscenty.createkinetism.content.oil.DieselEngineBlock;
import me.moonscenty.createkinetism.content.oil.DistillationOutputBlock;
import me.moonscenty.createkinetism.content.oil.PumpjackArmBlock;
import me.moonscenty.createkinetism.content.oil.PumpjackCrankBlock;
import me.moonscenty.createkinetism.content.oil.PumpjackWellBlock;
import me.moonscenty.createkinetism.content.chamber.DualInputChamberBlock;
import me.moonscenty.createkinetism.content.steel.SteelFluidValveBlock;
import me.moonscenty.createkinetism.content.steel.SteelPipeAttachmentModel;
import me.moonscenty.createkinetism.content.steel.SteelPipeBlock;
import me.moonscenty.createkinetism.content.steel.SteelPumpBlock;
import me.moonscenty.createkinetism.content.steel.SteelSmartPipeBlock;
import me.moonscenty.createkinetism.content.steel.SteelTankBlock;
import me.moonscenty.createkinetism.content.steel.SteelTankItem;
import me.moonscenty.createkinetism.content.steel.SteelTankModel;
import me.moonscenty.createkinetism.content.steel.SteelWindowPipeBlock;
import me.moonscenty.createkinetism.content.steel.StraightSteelPipeBlock;
import me.moonscenty.createkinetism.content.vat.CombinerBlock;
import me.moonscenty.createkinetism.content.vat.VatBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Every machine in the mod.
 *
 * <p>The mapping from Mekanism is deliberately mechanical: a machine that only moves items becomes
 * a {@link ChamberBlock}, a machine that touches a chemical becomes a {@link VatBlock} that sits
 * over a Create Basin. Two block classes cover the whole roster because the differences between
 * Mekanism's machines live in their recipes, not in their block behaviour.</p>
 *
 * <p>Stress impacts are quoted in Create's units (SU per RPM) and stand in for Mekanism's
 * energy-per-tick: the more aggressive the chemistry, the more of your kinetic network it eats.</p>
 */
public class CKBlocks {

	private static final CreateRegistrate REGISTRATE = CreateKinetism.registrate();

	// Declared before the entries below so they are initialised when the entries register themselves.
	private static final List<NonNullSupplier<? extends Block>> CHAMBER_BLOCKS = new ArrayList<>();
	private static final List<NonNullSupplier<? extends Block>> VAT_BLOCKS = new ArrayList<>();

	/** All machines, in creative-tab order. */
	public static final List<BlockEntry<?>> ALL = new ArrayList<>();

	// --- item in, item out -------------------------------------------------------------------

	/** Mekanism: Enrichment Chamber. The 2x ore step and the dirty-dust cleanup step. */
	/** Not a chamber: a press. It works on a depot, belt or basin below rather than on slots of its own. */
	public static final BlockEntry<MechanicalEnricherBlock> MECHANICAL_ENRICHER = register(REGISTRATE
		.block("mechanical_enricher", MechanicalEnricherBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.METAL)
			.noOcclusion()
			.sound(SoundType.NETHERITE_BLOCK))
		.transform(CKStress.setImpact(4.0))
		.item(AssemblyOperatorBlockItem::new)
		.build()
		.register());

	// Mekanism's Crusher and Precision Sawmill have no block here on purpose: Create already ships
	// Crushing Wheels, the Millstone and the Mechanical Saw. Those steps of the Mekanism chain are
	// shipped as create:crushing / create:milling recipes instead of as duplicate machines.

	/** Mekanism: Combiner. Two inputs, so it needs both slots filled before it starts. */
	/** A vat that carries its own infusion item, so it gets its own class and block entity. */
	public static final BlockEntry<CombinerBlock> COMBINER = register(REGISTRATE
		.block("combiner", CombinerBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_GRAY)
			.noOcclusion()
			.sound(SoundType.NETHERITE_BLOCK))
		.transform(CKStress.setImpact(8.0))
		.item(AssemblyOperatorBlockItem::new)
		.build()
		.register());

	/**
	 * Not a chamber: a spout that has to be turned. It works on whatever is on the depot or belt
	 * below, and the infusion is dripped into it rather than fed through a slot.
	 */
	public static final BlockEntry<MechanicalInfuserBlock> MECHANICAL_INFUSER = register(REGISTRATE
		.block("mechanical_infuser", MechanicalInfuserBlock::new)
		.initialProperties(SharedProperties::copperMetal)
		.properties(p -> p.mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)
			.noOcclusion()
			.sound(SoundType.COPPER))
		.transform(CKStress.setImpact(8.0))
		.item(AssemblyOperatorBlockItem::new)
		.build()
		.register());

	/**
	 * Mekanism: Chemical Injection Chamber, built the same way as the infuser above rather than as a
	 * vat - see {@link me.moonscenty.createkinetism.content.injection.InjectionChamberBlock}.
	 */
	public static final BlockEntry<InjectionChamberBlock> INJECTION_CHAMBER = register(REGISTRATE
		.block("injection_chamber", InjectionChamberBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_GRAY)
			.noOcclusion()
			.sound(SoundType.NETHERITE_BLOCK))
		.transform(CKStress.setImpact(8.0))
		.item(AssemblyOperatorBlockItem::new)
		.build()
		.register());

	// --- basin machines ----------------------------------------------------------------------

	/**
	 * Mekanism: Purification Chamber, and the one machine that is not a vat. It is a single block
	 * that holds its own basin so the two can shake together - see PurificationVibratorBlockEntity.
	 */
	public static final BlockEntry<PurificationVibratorBlock> PURIFICATION_VIBRATOR = register(REGISTRATE
		.block("purification_vibrator", PurificationVibratorBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_GRAY)
			.noOcclusion()
			.sound(SoundType.NETHERITE_BLOCK))
		.transform(CKStress.setImpact(8.0))
		.item()
		.build()
		.register());

	/** Mekanism: Chemical Dissolution Chamber. Ore plus sulfuric acid, the first 5x step. */
	public static final BlockEntry<VatBlock> DISSOLUTION_VAT =
		vat("dissolution_vat", CKRecipeTypes.DISSOLVING, 16.0);

	/** Mekanism: Chemical Washer. Dirty slurry plus water. */
	public static final BlockEntry<VatBlock> WASHING_VAT = vat("washing_vat", CKRecipeTypes.WASHING, 8.0);

	/** Mekanism: Chemical Crystallizer. Clean slurry back into a solid. */
	public static final BlockEntry<VatBlock> CRYSTALLIZING_VAT =
		vat("crystallizing_vat", CKRecipeTypes.CRYSTALLIZING, 8.0);

	/** Mekanism: Chemical Oxidizer. A solid into a gas. */
	public static final BlockEntry<VatBlock> OXIDATION_VAT = vat("oxidation_vat", CKRecipeTypes.OXIDIZING, 8.0);

	/** Mekanism: Chemical Infuser. Two gases into a third. */
	public static final BlockEntry<VatBlock> CHEMICAL_INFUSION_VAT =
		vat("chemical_infusion_vat", CKRecipeTypes.CHEMICAL_INFUSING, 8.0);

	/** Mekanism: Electrolytic Separator. Splits a fluid into two gases; the hungriest machine here. */
	public static final BlockEntry<VatBlock> ELECTROLYTIC_SEPARATOR =
		vat("electrolytic_separator", CKRecipeTypes.SEPARATING, 16.0);

	/**
	 * Mekanism: Thermal Evaporation Plant, collapsed into a single block. Its recipes carry a heat
	 * requirement, so it needs a Blaze Burner under the basin - which is how Create already models
	 * "this process needs to be hot".
	 */
	public static final BlockEntry<VatBlock> EVAPORATION_VAT =
		vat("evaporation_vat", CKRecipeTypes.EVAPORATING, 8.0);

	// --- oil (ported from Petrochem, see LICENSE-THIRD-PARTY.md) ------------------------------

	/** The wellhead. Needs Create fluid pipe running from underneath it down to bedrock. */
	public static final BlockEntry<PumpjackWellBlock> PUMPJACK_WELL = register(REGISTRATE
		.block("pumpjack_well", PumpjackWellBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_BLACK)
			.noOcclusion()
			.sound(SoundType.NETHERITE_BLOCK))
		.item()
		.build()
		.register());

	/** The driven end. Wants at least 32 RPM on its shaft. */
	public static final BlockEntry<PumpjackCrankBlock> PUMPJACK_CRANK = register(REGISTRATE
		.block("pumpjack_crank", PumpjackCrankBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_GRAY)
			.noOcclusion()
			.sound(SoundType.NETHERITE_BLOCK))
		.transform(CKStress.setImpact(32.0))
		.item()
		.build()
		.register());

	/** The walking beam that ties crank and well together. */
	public static final BlockEntry<PumpjackArmBlock> PUMPJACK_ARM = register(REGISTRATE
		.block("pumpjack_arm", PumpjackArmBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_GRAY)
			.noOcclusion()
			.sound(SoundType.NETHERITE_BLOCK))
		.item()
		.build()
		.register());

	/** Create's tank multiblock in steel, and the column body a distillation controller claims. */
	public static final BlockEntry<SteelTankBlock> STEEL_TANK = register(REGISTRATE
		.block("steel_tank", SteelTankBlock::new)
		.initialProperties(SharedProperties::stone)
		.onRegister(CreateRegistrate.blockModel(() -> SteelTankModel::standard))
		.onRegister(MovementBehaviour.movementBehaviour(new FluidTankMovementBehavior()))
		.properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY)
			.noOcclusion()
			.sound(SoundType.NETHERITE_BLOCK))
		.item(SteelTankItem::new)
		.build()
		.register());

	/** Turns a steel tank stack into a fractionating column. */
	public static final BlockEntry<DistillationControllerBlock> DISTILLATION_CONTROLLER = register(REGISTRATE
		.block("distillation_controller", DistillationControllerBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_GRAY)
			.sound(SoundType.NETHERITE_BLOCK))
		.item()
		.build()
		.register());

	/** Taps one fraction off the column. Redstone makes it discard that cut instead. */
	public static final BlockEntry<DistillationOutputBlock> DISTILLATION_OUTPUT = register(REGISTRATE
		.block("distillation_output", DistillationOutputBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_GRAY)
			.sound(SoundType.NETHERITE_BLOCK))
		.item()
		.build()
		.register());

	/** Burns off the cuts you have no use for. */
	public static final BlockEntry<FlarestackBlock> FLARESTACK = register(REGISTRATE
		.block("flarestack", FlarestackBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_GRAY)
			.sound(SoundType.NETHERITE_BLOCK))
		.item()
		.build()
		.register());

	/** Light and quick. Runs on gasoline. */
	public static final BlockEntry<FuelEngineBlock> GASOLINE_ENGINE =
		engine("gasoline_engine", p -> new FuelEngineBlock(p, CKRecipeTypes.GASOLINE_ENGINE_FUEL, 2000));

	/**
	 * The workhorse. Unlike the other two this one bolts onto a face and drives an adjacent shaft,
	 * so several can share one shaft and stack their output.
	 */
	public static final BlockEntry<DieselEngineBlock> DIESEL_ENGINE = register(REGISTRATE
		.block("diesel_engine", DieselEngineBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_GRAY)
			.sound(SoundType.NETHERITE_BLOCK))
		// Capacity stays configurable here because the Powered Shaft reads it off the block; see
		// DieselEngineBlockEntity for why the fuel cannot supply it. The shaft's speed is quantised to
		// 16/32/48/64, so 64 is what it actually tops out at.
		.transform(CKStress.setCapacity(1536.0))
		.onRegister(BlockStressValues.setGeneratorSpeed(64, true))
		.item()
		.build()
		.register());

	/**
	 * Runs on the gas fractions, which are otherwise flare fodder. Built on Create's encased fan
	 * rather than the engine body - intake at the front, shaft out the back, fuel in the sides.
	 */
	public static final BlockEntry<GasTurbineBlock> GAS_TURBINE =
		engine("gas_turbine", p -> new GasTurbineBlock(p, CKRecipeTypes.TURBINE_FUEL, 4000));


	// --- steel plumbing --------------------------------------------------------------------------
	// Create's own pipe family in steel. Same throughput as copper; the difference is that steel
	// cannot be encased, so refinery runs stay visually distinct from ordinary Create plumbing.

	public static final BlockEntry<SteelPipeBlock> STEEL_PIPE = register(REGISTRATE
		.block("steel_pipe", SteelPipeBlock::new)
		.onRegister(CreateRegistrate.blockModel(() -> SteelPipeAttachmentModel::withAO))
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY)
			.sound(SoundType.NETHERITE_BLOCK))
		.item()
		.build()
		.register());

	// The two wrenched forms have no item of their own - you reach them by wrenching a steel pipe,
	// and breaking one drops a plain pipe. They are deliberately kept out of the creative tab.

	public static final BlockEntry<StraightSteelPipeBlock> STRAIGHT_STEEL_PIPE = REGISTRATE
		.block("straight_steel_pipe", StraightSteelPipeBlock::new)
		.onRegister(CreateRegistrate.blockModel(() -> SteelPipeAttachmentModel::withAO))
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY)
			.sound(SoundType.NETHERITE_BLOCK))
		.register();

	public static final BlockEntry<SteelWindowPipeBlock> STEEL_WINDOW_PIPE = REGISTRATE
		.block("steel_window_pipe", SteelWindowPipeBlock::new)
		.onRegister(CreateRegistrate.blockModel(() -> SteelPipeAttachmentModel::withAO))
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY)
			.sound(SoundType.NETHERITE_BLOCK))
		.register();

	public static final BlockEntry<SteelSmartPipeBlock> STEEL_SMART_PIPE = register(REGISTRATE
		.block("steel_smart_pipe", SteelSmartPipeBlock::new)
		.onRegister(CreateRegistrate.blockModel(() -> SteelPipeAttachmentModel::withAO))
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY)
			.sound(SoundType.NETHERITE_BLOCK))
		.item()
		.build()
		.register());

	public static final BlockEntry<SteelFluidValveBlock> STEEL_VALVE = register(REGISTRATE
		.block("steel_valve", SteelFluidValveBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY)
			.sound(SoundType.NETHERITE_BLOCK))
		.item()
		.build()
		.register());

	public static final BlockEntry<SteelPumpBlock> STEEL_PUMP = register(REGISTRATE
		.block("steel_pump", SteelPumpBlock::new)
		.onRegister(CreateRegistrate.blockModel(() -> SteelPipeAttachmentModel::withAO))
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY)
			.sound(SoundType.NETHERITE_BLOCK))
		.transform(CKStress.setImpact(3.0))
		.item()
		.build()
		.register());
	// --- kinetic infrastructure ----------------------------------------------------------------

	/**
	 * Mekanism: Energy Cube, as close as Create's stress model allows. Sits in a shaft line and
	 * buffers load through time instead of storing power.
	 */
	public static final BlockEntry<KineticAccumulatorBlock> KINETIC_ACCUMULATOR = register(REGISTRATE
		.block("kinetic_accumulator", KineticAccumulatorBlock::new)
		.initialProperties(SharedProperties::stone)
		.properties(p -> p.mapColor(MapColor.COLOR_BLUE)
			.sound(SoundType.NETHERITE_BLOCK))
		.item()
		.build()
		.register());

	// -----------------------------------------------------------------------------------------

	private static <T extends Block> BlockEntry<T> register(BlockEntry<T> entry) {
		ALL.add(entry);
		return entry;
	}

	private static BlockEntry<ChamberBlock> chamber(String name, CKRecipeTypes recipeType, int inputSlots,
		double stressImpact) {
		BlockEntry<ChamberBlock> entry = REGISTRATE
			.block(name, p -> inputSlots > 1 ? new DualInputChamberBlock(p, recipeType, inputSlots)
				: new ChamberBlock(p, recipeType, inputSlots))
			.initialProperties(SharedProperties::stone)
			.properties(p -> p.mapColor(MapColor.METAL)
				.sound(SoundType.NETHERITE_BLOCK))
			.transform(CKStress.setImpact(stressImpact))
			.item()
			.build()
			.register();
		CHAMBER_BLOCKS.add(entry);
		ALL.add(entry);
		return entry;
	}

	// No capacity and no RPM: these two generate rotation directly, so both figures come from
	// whichever fuel is in the tank. Registering a single number for the block would only put a wrong
	// one in the goggle tooltip - LPG and steam do not run a turbine at the same speed or strength.
	private static <B extends FuelEngineBlock> BlockEntry<B> engine(String name,
		NonNullFunction<BlockBehaviour.Properties, B> factory) {
		return register(REGISTRATE.block(name, factory)
			.initialProperties(SharedProperties::stone)
			.properties(p -> p.mapColor(MapColor.COLOR_GRAY)
				.noOcclusion()
				.sound(SoundType.NETHERITE_BLOCK))
			.item()
			.build()
			.register());
	}

	private static BlockEntry<VatBlock> vat(String name, CKRecipeTypes recipeType, double stressImpact) {
		BlockEntry<VatBlock> entry = REGISTRATE.block(name, p -> new VatBlock(p, recipeType))
			.initialProperties(SharedProperties::stone)
			.properties(p -> p.mapColor(MapColor.COLOR_GRAY)
				.noOcclusion()
				.sound(SoundType.NETHERITE_BLOCK))
			.transform(CKStress.setImpact(stressImpact))
		// AssemblyOperatorBlockItem, the same item Create gives its Mixer and Press: shift-clicking the
		// top of a basin, depot, ejector or horizontal belt places the machine two blocks up with the
		// gap already correct. Without it the block simply refuses to go on a basin, because canSurvive
		// forbids sitting directly on one.
			.item(AssemblyOperatorBlockItem::new)
			.build()
			.register();
		VAT_BLOCKS.add(entry);
		ALL.add(entry);
		return entry;
	}

	public static List<NonNullSupplier<? extends Block>> chamberBlocks() {
		return CHAMBER_BLOCKS;
	}

	public static List<NonNullSupplier<? extends Block>> vatBlocks() {
		return VAT_BLOCKS;
	}

	/** Class-loading hook, called from the mod constructor. */
	public static void register() {
	}
}
