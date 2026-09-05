package me.moonscenty.createkinetism.registry;



import com.simibubi.create.AllPartialModels;

import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;

import com.simibubi.create.content.fluids.pipes.GlassPipeVisual;

import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlockEntity;

import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;

import com.simibubi.create.content.fluids.pipes.TransparentStraightPipeRenderer;

import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlockEntity;

import com.simibubi.create.content.fluids.pipes.valve.FluidValveRenderer;

import com.simibubi.create.content.fluids.pipes.valve.FluidValveVisual;

import com.simibubi.create.content.fluids.pump.PumpBlockEntity;

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;

import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;

import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import com.simibubi.create.foundation.data.CreateRegistrate;

import com.tterrag.registrate.util.entry.BlockEntityEntry;



import me.moonscenty.createkinetism.CreateKinetism;

import me.moonscenty.createkinetism.content.accumulator.KineticAccumulatorBlockEntity;
import me.moonscenty.createkinetism.content.chemical.ChemicalTankBlockEntity;
import me.moonscenty.createkinetism.content.chemical.ChemicalTankRenderer;

import me.moonscenty.createkinetism.content.vibrator.PurificationVibratorBlockEntity;

import me.moonscenty.createkinetism.content.vibrator.PurificationVibratorRenderer;

import me.moonscenty.createkinetism.content.accumulator.KineticAccumulatorRenderer;

import me.moonscenty.createkinetism.content.washer.MechanicalWasherBlockEntity;

import me.moonscenty.createkinetism.content.washer.MechanicalWasherRenderer;

import me.moonscenty.createkinetism.content.machine.ProcessingMachineBlockEntity;

import me.moonscenty.createkinetism.content.dissolution.DissolutionChamberBlockEntity;

import me.moonscenty.createkinetism.content.dissolution.DissolutionChamberRenderer;

import me.moonscenty.createkinetism.content.evaporation.EvaporationPlantBlockEntity;

import me.moonscenty.createkinetism.content.evaporation.EvaporationPlantRenderer;

import me.moonscenty.createkinetism.content.chamber.MechanicalEnricherBlockEntity;

import me.moonscenty.createkinetism.content.chamber.MechanicalEnricherRenderer;

import me.moonscenty.createkinetism.content.chamber.MechanicalEnricherVisual;

import me.moonscenty.createkinetism.content.oil.DieselEngineVisual;

import me.moonscenty.createkinetism.content.oil.GasTurbineVisual;

import me.moonscenty.createkinetism.content.oil.FuelEngineVisual;

import me.moonscenty.createkinetism.content.oil.DieselEngineBlockEntity;

import me.moonscenty.createkinetism.content.oil.DieselEngineRenderer;

import me.moonscenty.createkinetism.content.oil.DistillationControllerBlockEntity;

import me.moonscenty.createkinetism.content.oil.DistillationControllerRenderer;

import me.moonscenty.createkinetism.content.oil.DistillationOutputBlockEntity;

import me.moonscenty.createkinetism.content.oil.DistillationOutputRenderer;

import me.moonscenty.createkinetism.content.oil.FlarestackBlockEntity;

import me.moonscenty.createkinetism.content.oil.FuelEngineBlockEntity;

import me.moonscenty.createkinetism.content.oil.FuelEngineRenderer;

import me.moonscenty.createkinetism.content.oil.GasTurbineRenderer;

import me.moonscenty.createkinetism.content.oil.PumpjackArmBlockEntity;

import me.moonscenty.createkinetism.content.oil.PumpjackArmRenderer;

import me.moonscenty.createkinetism.content.oil.PumpjackCrankRenderer;

import me.moonscenty.createkinetism.content.oil.PumpjackWellBlock;

import me.moonscenty.createkinetism.content.oil.PumpjackCrankBlockEntity;

import me.moonscenty.createkinetism.content.oil.PumpjackWellBlockEntity;

import me.moonscenty.createkinetism.content.steel.SteelPumpRenderer;

import me.moonscenty.createkinetism.content.steel.SteelTankBlockEntity;

import me.moonscenty.createkinetism.content.steel.SteelTankRenderer;

import me.moonscenty.createkinetism.content.infuser.MechanicalMetallurgicInfuserBlockEntity;

import me.moonscenty.createkinetism.content.infuser.MechanicalMetallurgicInfuserRenderer;

import me.moonscenty.createkinetism.content.injection.InjectionChamberBlockEntity;

import me.moonscenty.createkinetism.content.injection.InjectionChamberRenderer;
import me.moonscenty.createkinetism.content.compressor.KinetiteCompressorBlockEntity;
import me.moonscenty.createkinetism.content.compressor.KinetiteCompressorCradleBlockEntity;
import me.moonscenty.createkinetism.content.compressor.KinetiteCompressorRenderer;
import me.moonscenty.createkinetism.content.multimeter.MultimeterBlockEntity;
import me.moonscenty.createkinetism.content.multimeter.MultimeterRenderer;
import me.moonscenty.createkinetism.content.crystallization.CrystallizationChamberBlockEntity;
import me.moonscenty.createkinetism.content.crystallization.CrystallizationChamberRenderer;
import me.moonscenty.createkinetism.content.oxidation.OxidationChamberBlockEntity;
import me.moonscenty.createkinetism.content.oxidation.OxidationChamberRenderer;

import me.moonscenty.createkinetism.content.vat.CombinerBlockEntity;
import me.moonscenty.createkinetism.content.vat.ElectrolyticSeparatorBlockEntity;
import me.moonscenty.createkinetism.content.chemistry.MechanicalChemistryInfuserBlockEntity;
import me.moonscenty.createkinetism.content.chemistry.MechanicalChemistryInfuserRenderer;

import me.moonscenty.createkinetism.content.vat.CombinerRenderer;

import me.moonscenty.createkinetism.content.vat.VatBlockEntity;

import me.moonscenty.createkinetism.content.vat.VatRenderer;



import net.minecraft.core.Direction;



import net.neoforged.neoforge.capabilities.Capabilities;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;



/**

 * Two block entity types cover all fourteen machines, because the recipe type is carried by the

 * block rather than the block entity.

 */

public class CKBlockEntityTypes {



	private static final CreateRegistrate REGISTRATE = CreateKinetism.registrate();



	/**

	 * Same block entity class as the other chambers, but its own type so it can carry the press-style

	 * visual. Flywheel picks the visual off the type, and the enricher is the only chamber with a

	 * shaft and a head to show.

	 */

	public static final BlockEntityEntry<MechanicalEnricherBlockEntity> MECHANICAL_ENRICHER = REGISTRATE

		.blockEntity("mechanical_enricher", MechanicalEnricherBlockEntity::new)

		.visual(() -> MechanicalEnricherVisual::new)

		.validBlocks(CKBlocks.MECHANICAL_ENRICHER)

		.renderer(() -> MechanicalEnricherRenderer::new)

		.register();



	public static final BlockEntityEntry<VatBlockEntity> VAT = REGISTRATE

		.blockEntity("vat", VatBlockEntity::new)

		.validBlocksDeferred(CKBlocks::vatBlocks)

		.renderer(() -> VatRenderer::new)

		.register();



	/**

	 * The same mixer body as the other vats, but its own type: the Combiner carries an infusion

	 * inventory, which lives on the block entity, and a renderer that draws what is in it.

	 */

	/** A spout that has to be turned. Its own type, and its own renderer for the nozzle. */

	public static final BlockEntityEntry<MechanicalMetallurgicInfuserBlockEntity> MECHANICAL_METALLURGIC_INFUSER = REGISTRATE

		.blockEntity("mechanical_metallurgic_infuser", MechanicalMetallurgicInfuserBlockEntity::new)

		.validBlocks(CKBlocks.MECHANICAL_METALLURGIC_INFUSER)

		.renderer(() -> MechanicalMetallurgicInfuserRenderer::new)

		.register();



	/** The same spout shape as the infuser, built as its own type for its own cog-and-housing model. */

	public static final BlockEntityEntry<InjectionChamberBlockEntity> INJECTION_CHAMBER = REGISTRATE

		.blockEntity("injection_chamber", InjectionChamberBlockEntity::new)

		.validBlocks(CKBlocks.INJECTION_CHAMBER)

		.renderer(() -> InjectionChamberRenderer::new)

		.register();



	/** The Injection Chamber's housing without its tank - the basin holds both sides here. */
	public static final BlockEntityEntry<KinetiteCompressorBlockEntity> KINETITE_COMPRESSOR = REGISTRATE
		.blockEntity("kinetite_compressor", KinetiteCompressorBlockEntity::new)
		.validBlocks(CKBlocks.KINETITE_COMPRESSOR)
		.renderer(() -> KinetiteCompressorRenderer::new)
		.register();

	/** Carries no logic; it exists so a shaft at the back has something kinetic to attach to. */
	public static final BlockEntityEntry<KinetiteCompressorCradleBlockEntity> KINETITE_COMPRESSOR_CRADLE =
		REGISTRATE.blockEntity("kinetite_compressor_cradle", KinetiteCompressorCradleBlockEntity::new)
			.validBlocks(CKBlocks.KINETITE_COMPRESSOR_CRADLE)
			.register();

	/** Two needles on one gauge - see MultimeterBlockEntity. */
	public static final BlockEntityEntry<MultimeterBlockEntity> MULTIMETER = REGISTRATE
		.blockEntity("multimeter", MultimeterBlockEntity::new)
		.validBlocks(CKBlocks.MULTIMETER)
		.renderer(() -> MultimeterRenderer::new)
		.register();

	/** Same borrowed housing as the Oxidation Chamber, and equally tankless. */
	public static final BlockEntityEntry<CrystallizationChamberBlockEntity> CRYSTALLIZATION_CHAMBER =
		REGISTRATE.blockEntity("crystallization_chamber", CrystallizationChamberBlockEntity::new)
			.validBlocks(CKBlocks.CRYSTALLIZATION_CHAMBER)
			.renderer(() -> CrystallizationChamberRenderer::new)
			.register();

	public static final BlockEntityEntry<OxidationChamberBlockEntity> OXIDATION_CHAMBER = REGISTRATE
		.blockEntity("oxidation_chamber", OxidationChamberBlockEntity::new)
		.validBlocks(CKBlocks.OXIDATION_CHAMBER)
		.renderer(() -> OxidationChamberRenderer::new)
		.register();

	public static final BlockEntityEntry<CombinerBlockEntity> COMBINER = REGISTRATE

		.blockEntity("combiner", CombinerBlockEntity::new)

		.validBlocks(CKBlocks.COMBINER)

		.renderer(() -> CombinerRenderer::new)

		.register();


	/** Its own tank in place of the mixer whisk - see MechanicalChemistryInfuserBlockEntity. */
	public static final BlockEntityEntry<MechanicalChemistryInfuserBlockEntity> MECHANICAL_CHEMISTRY_INFUSER = REGISTRATE
		.blockEntity("mechanical_chemistry_infuser", MechanicalChemistryInfuserBlockEntity::new)
		.validBlocks(CKBlocks.MECHANICAL_CHEMISTRY_INFUSER)
		.renderer(() -> MechanicalChemistryInfuserRenderer::new)
		.register();


	public static final BlockEntityEntry<PumpjackWellBlockEntity> PUMPJACK_WELL = REGISTRATE

		.blockEntity("pumpjack_well", PumpjackWellBlockEntity::new)

		.validBlocks(CKBlocks.PUMPJACK_WELL)

		.register();



	public static final BlockEntityEntry<PumpjackCrankBlockEntity> PUMPJACK_CRANK = REGISTRATE

		.blockEntity("pumpjack_crank", PumpjackCrankBlockEntity::new)

		.validBlocks(CKBlocks.PUMPJACK_CRANK)

		.renderer(() -> PumpjackCrankRenderer::new)

		.register();



	public static final BlockEntityEntry<PumpjackArmBlockEntity> PUMPJACK_ARM = REGISTRATE

		.blockEntity("pumpjack_arm", PumpjackArmBlockEntity::new)

		.validBlocks(CKBlocks.PUMPJACK_ARM)

		.renderer(() -> PumpjackArmRenderer::new)

		.register();



	public static final BlockEntityEntry<SteelTankBlockEntity> STEEL_TANK = REGISTRATE

		.blockEntity("steel_tank", SteelTankBlockEntity::new)

		.validBlocks(CKBlocks.STEEL_TANK)

		.renderer(() -> SteelTankRenderer::new)

		.register();



	/**

	 * Create's tank multiblock again, this time boiling its own contents into the next stage of the

	 * evaporation chain instead of just holding fluid.

	 */

	public static final BlockEntityEntry<EvaporationPlantBlockEntity> EVAPORATION_PLANT = REGISTRATE

		.blockEntity("evaporation_plant", EvaporationPlantBlockEntity::new)

		.validBlocks(CKBlocks.EVAPORATION_PLANT)

		.renderer(() -> EvaporationPlantRenderer::new)

		.register();



	// The steel plumbing reuses Create's own block entities wholesale - only the type is ours, so

	// that our blocks can point at it. Pipes need no fluid capability; Create's fluid network walks

	// the blocks directly.



	public static final BlockEntityEntry<FluidPipeBlockEntity> STEEL_PIPE = REGISTRATE

		.blockEntity("steel_pipe", FluidPipeBlockEntity::new)

		.validBlocks(CKBlocks.STEEL_PIPE)

		.register();



	public static final BlockEntityEntry<StraightPipeBlockEntity> STRAIGHT_STEEL_PIPE = REGISTRATE

		.blockEntity("straight_steel_pipe", StraightPipeBlockEntity::new)

		.validBlocks(CKBlocks.STRAIGHT_STEEL_PIPE)

		.register();



	public static final BlockEntityEntry<StraightPipeBlockEntity> STEEL_WINDOW_PIPE = REGISTRATE

		.blockEntity("steel_window_pipe", StraightPipeBlockEntity::new)

		.visual(() -> GlassPipeVisual::new, false)

		.validBlocks(CKBlocks.STEEL_WINDOW_PIPE)

		.renderer(() -> TransparentStraightPipeRenderer::new)

		.register();



	public static final BlockEntityEntry<SmartFluidPipeBlockEntity> STEEL_SMART_PIPE = REGISTRATE

		.blockEntity("steel_smart_pipe", SmartFluidPipeBlockEntity::new)

		.validBlocks(CKBlocks.STEEL_SMART_PIPE)

		.renderer(() -> SmartBlockEntityRenderer::new)

		.register();



	public static final BlockEntityEntry<FluidValveBlockEntity> STEEL_VALVE = REGISTRATE

		.blockEntity("steel_valve", FluidValveBlockEntity::new)

		.visual(() -> FluidValveVisual::new)

		.validBlocks(CKBlocks.STEEL_VALVE)

		.renderer(() -> FluidValveRenderer::new)

		.register();



	public static final BlockEntityEntry<PumpBlockEntity> STEEL_PUMP = REGISTRATE

		.blockEntity("steel_pump", PumpBlockEntity::new)

		.visual(() -> SingleAxisRotatingVisual.ofZ(AllPartialModels.MECHANICAL_PUMP_COG))

		.validBlocks(CKBlocks.STEEL_PUMP)

		.renderer(() -> SteelPumpRenderer::new)

		.register();



	public static final BlockEntityEntry<DistillationControllerBlockEntity> DISTILLATION_CONTROLLER = REGISTRATE

		.blockEntity("distillation_controller", DistillationControllerBlockEntity::new)

		.validBlocks(CKBlocks.DISTILLATION_CONTROLLER)

		.renderer(() -> DistillationControllerRenderer::new)

		.register();



	public static final BlockEntityEntry<DistillationOutputBlockEntity> DISTILLATION_OUTPUT = REGISTRATE

		.blockEntity("distillation_output", DistillationOutputBlockEntity::new)

		.validBlocks(CKBlocks.DISTILLATION_OUTPUT)

		.renderer(() -> DistillationOutputRenderer::new)

		.register();



	public static final BlockEntityEntry<FlarestackBlockEntity> FLARESTACK = REGISTRATE

		.blockEntity("flarestack", FlarestackBlockEntity::new)

		.validBlocks(CKBlocks.FLARESTACK)

		.register();



	/** One type for all three engines; the block decides which fuels it burns. */

	public static final BlockEntityEntry<FuelEngineBlockEntity> FUEL_ENGINE = REGISTRATE

		.blockEntity("fuel_engine", FuelEngineBlockEntity::new)

		.visual(() -> FuelEngineVisual::new)

		.validBlocks(CKBlocks.GASOLINE_ENGINE)

		.renderer(() -> FuelEngineRenderer::new)

		.register();



	/**

	 * Same block entity class as the gasoline engine - the mechanics are identical - but its own type

	 * so it can carry the fan renderer instead of the piston one.

	 */

	public static final BlockEntityEntry<FuelEngineBlockEntity> GAS_TURBINE = REGISTRATE

		.blockEntity("gas_turbine", FuelEngineBlockEntity::new)

		.visual(() -> GasTurbineVisual::new)

		.validBlocks(CKBlocks.GAS_TURBINE)

		.renderer(() -> GasTurbineRenderer::new)

		.register();



	/** Drives a powered shaft instead of turning itself, so it gets its own type. */

	public static final BlockEntityEntry<DieselEngineBlockEntity> DIESEL_ENGINE = REGISTRATE

		.blockEntity("diesel_engine", DieselEngineBlockEntity::new)

		.visual(() -> DieselEngineVisual::new)

		.validBlocks(CKBlocks.DIESEL_ENGINE)

		.renderer(() -> DieselEngineRenderer::new)

		.register();



	public static final BlockEntityEntry<PurificationVibratorBlockEntity> PURIFICATION_VIBRATOR = REGISTRATE

		.blockEntity("purification_vibrator", PurificationVibratorBlockEntity::new)

		.validBlocks(CKBlocks.PURIFICATION_VIBRATOR)

		.renderer(() -> PurificationVibratorRenderer::new)

		.register();



	public static final BlockEntityEntry<DissolutionChamberBlockEntity> DISSOLUTION_CHAMBER = REGISTRATE

		.blockEntity("dissolution_chamber", DissolutionChamberBlockEntity::new)

		.validBlocks(CKBlocks.DISSOLUTION_CHAMBER)

		.renderer(() -> DissolutionChamberRenderer::new)

		.register();



	/** Its own vessel rather than a basin, so it renders its own fluid - see MechanicalWasherRenderer. */

	public static final BlockEntityEntry<MechanicalWasherBlockEntity> MECHANICAL_WASHER = REGISTRATE

		.blockEntity("mechanical_washer", MechanicalWasherBlockEntity::new)

		.validBlocks(CKBlocks.MECHANICAL_WASHER)

		.renderer(() -> MechanicalWasherRenderer::new)

		.register();



	/** Its own type, not the shared vat one: it hands its two products out sideways. */
	public static final BlockEntityEntry<ElectrolyticSeparatorBlockEntity> ELECTROLYTIC_SEPARATOR = REGISTRATE
		.blockEntity("electrolytic_separator", ElectrolyticSeparatorBlockEntity::new)
		.validBlocks(CKBlocks.ELECTROLYTIC_SEPARATOR)
		.renderer(() -> VatRenderer::new)
		.register();

	public static final BlockEntityEntry<ChemicalTankBlockEntity> CHEMICAL_TANK = REGISTRATE
		.blockEntity("chemical_tank", ChemicalTankBlockEntity::new)
		.validBlocks(CKBlocks.CHEMICAL_TANK)
		.renderer(() -> ChemicalTankRenderer::new)
		.register();

	public static final BlockEntityEntry<KineticAccumulatorBlockEntity> ACCUMULATOR = REGISTRATE

		.blockEntity("kinetic_accumulator", KineticAccumulatorBlockEntity::new)

		.validBlocks(CKBlocks.KINETIC_ACCUMULATOR)

		.renderer(() -> KineticAccumulatorRenderer::new)

		.register();



	/**

	 * Chambers expose their inventory to hoppers, belts, funnels and the Mechanical Arm. The handler

	 * depends on which face is asking, which is how a two-input chamber keeps its slots apart

	 * without a GUI. Vats expose nothing - their inventory is the basin below them, which already

	 * exposes its own.

	 */

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {

		// The Combiner exposes only its infusion slot; the bulk material belongs to the basin.

		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, COMBINER.get(),

			(be, context) -> be.getItemHandler(context));



		// The wellhead only hands oil out through the face it points at.

		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PUMPJACK_WELL.get(),

			(be, context) -> context == null || context == PumpjackWellBlock.getFacing(be.getBlockState())

				? be.tank.getCapability()

				: null);



		SteelTankBlockEntity.registerCapabilities(event, STEEL_TANK.get());

		EvaporationPlantBlockEntity.registerCapabilities(event, EVAPORATION_PLANT.get());

		DistillationControllerBlockEntity.registerCapabilities(event, DISTILLATION_CONTROLLER.get());

		DistillationOutputBlockEntity.registerCapabilities(event, DISTILLATION_OUTPUT.get());

		FlarestackBlockEntity.registerCapabilities(event, FLARESTACK.get());

		MechanicalMetallurgicInfuserBlockEntity.registerCapabilities(event, MECHANICAL_METALLURGIC_INFUSER.get());
		MechanicalChemistryInfuserBlockEntity.registerCapabilities(event, MECHANICAL_CHEMISTRY_INFUSER.get());

		InjectionChamberBlockEntity.registerCapabilities(event, INJECTION_CHAMBER.get());

		KineticAccumulatorBlockEntity.registerCapabilities(event, ACCUMULATOR.get());
		ChemicalTankBlockEntity.registerCapabilities(event, CHEMICAL_TANK.get());
		KinetiteCompressorBlockEntity.registerCapabilities(event, KINETITE_COMPRESSOR.get());
		KinetiteCompressorCradleBlockEntity.registerCapabilities(event, KINETITE_COMPRESSOR_CRADLE.get());

		ProcessingMachineBlockEntity.registerCapabilities(event, PURIFICATION_VIBRATOR.get());

		ProcessingMachineBlockEntity.registerCapabilities(event, DISSOLUTION_CHAMBER.get());

		ProcessingMachineBlockEntity.registerCapabilities(event, MECHANICAL_WASHER.get());



		// Engines are fuelled from below.

		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FUEL_ENGINE.get(),

			(be, context) -> context == null || context == Direction.DOWN ? be.tank.getCapability() : null);

		// The turbine takes fuel on any face that is not the intake or the shaft, so it can be fed

		// from the sides while both ends of its axis stay clear.

		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GAS_TURBINE.get(), (be, context) -> {

			if (context == null)

				return be.tank.getCapability();

			Direction facing = be.getBlockState()

				.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);

			return context.getAxis() == facing.getAxis() ? null : be.tank.getCapability();

		});

		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, DIESEL_ENGINE.get(),

			(be, context) -> be.tank.getCapability());

	}



	/** Class-loading hook, called from the mod constructor. */

	public static void register() {

	}

}

