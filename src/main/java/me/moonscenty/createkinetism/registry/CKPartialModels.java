package me.moonscenty.createkinetism.registry;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.Direction;

import me.moonscenty.createkinetism.CreateKinetism;

/**
 * Model parts that a renderer assembles every frame rather than the blockstate placing once.
 *
 * <p>Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md. This mirrors Create's own
 * {@code AllPartialModels}: the blockstate shows only the static housing, and everything that moves
 * lives here.</p>
 */
public class CKPartialModels {

	// Pumpjack. The holder is in the blockstate; these seven are the moving assembly.
	public static final PartialModel
		PUMPJACK_ARM = block("pumpjack/arm"),
		PUMPJACK_HEAD = block("pumpjack/head"),
		PUMPJACK_CONNECTOR = block("pumpjack/connector"),
		PUMPJACK_PITMAN = block("pumpjack/pitman"),
		PUMPJACK_CRANK = block("pumpjack/crank"),
		PUMPJACK_SMOOTH_ROD = block("pumpjack/smooth_rod");

	// Distillation. The mode dial is drawn on every face that is allowed to show one, and the output
	// swaps its base model on redstone rather than in the blockstate.
	public static final PartialModel
		DISTILLATION_SELECTOR = block("distillation_controller/head"),
		DISTILLATION_GAUGE = block("distillation_controller/gauge"),
		DISTILLATION_GAUGE_DIAL = block("distillation_controller/gauge_dial"),
		DISTILLATION_OUTPUT_BASE_UNPOWERED = block("distillation_output/base_unpowered"),
		DISTILLATION_OUTPUT_BASE_POWERED = block("distillation_output/base_powered");

	/** The steel pump turns its own cog, not the brass one Create's pump renderer reaches for. */
	public static final PartialModel STEEL_PUMP_COG = block("steel_pump/cog");

	/** The reciprocating piston on a fuel engine. */
	public static final PartialModel GASOLINE_ENGINE_PISTON = block("gasoline_engine/piston");

	/** The diesel engine's piston, cam linkage and the collar that grips the powered shaft. */
	public static final PartialModel
		DIESEL_ENGINE_PISTON = block("diesel_engine/piston"),
		DIESEL_ENGINE_LINKAGE = block("diesel_engine/linkage"),
		DIESEL_ENGINE_CONNECTOR = block("diesel_engine/shaft_connector");

	/** The Purification Vibrator's shaking half, everything above the bolted-down base. */
	public static final PartialModel PURIFICATION_VIBRATOR_HEAD = block("purification_vibrator/head");

	/** The Dissolution Chamber's rocking table, everything above the piston it tips on. */
	public static final PartialModel DISSOLUTION_CHAMBER_HEAD = block("dissolution_chamber/head");

	/** The washer's auger: a central axle with six stacked blades, turning about Y. */
	public static final PartialModel MECHANICAL_WASHER_PROPELLER = block("mechanical_washer/propeller");

	/** The infuser's nozzle, in three segments that telescope apart while it works. */
	public static final PartialModel MECHANICAL_METALLURGIC_INFUSER_TOP = block("mechanical_metallurgic_infuser/top");
	public static final PartialModel MECHANICAL_METALLURGIC_INFUSER_MIDDLE = block("mechanical_metallurgic_infuser/middle");
	public static final PartialModel MECHANICAL_METALLURGIC_INFUSER_BOTTOM = block("mechanical_metallurgic_infuser/bottom");
	public static final PartialModel MECHANICAL_CHEMISTRY_INFUSER_TOP = block("mechanical_chemistry_infuser/top");
	public static final PartialModel MECHANICAL_CHEMISTRY_INFUSER_MIDDLE = block("mechanical_chemistry_infuser/middle");
	public static final PartialModel MECHANICAL_CHEMISTRY_INFUSER_BOTTOM = block("mechanical_chemistry_infuser/bottom");

	/** The Injection Chamber's moving cog and its two static housing pieces. */
	public static final PartialModel INJECTION_CHAMBER_COG = block("injection_chamber/cog");
	public static final PartialModel INJECTION_CHAMBER_HEAD = block("injection_chamber/head");
	public static final PartialModel INJECTION_CHAMBER_ARROWS = block("injection_chamber/arrows");

	/** The Crystallization Chamber's parts: the Injection Chamber's again, copied to stay separate. */
	public static final PartialModel CRYSTALLIZATION_CHAMBER_COG = block("crystallization_chamber/cog");
	public static final PartialModel CRYSTALLIZATION_CHAMBER_HEAD = block("crystallization_chamber/head");
	public static final PartialModel CRYSTALLIZATION_CHAMBER_ARROWS =
		block("crystallization_chamber/arrows");

	/** The Oxidation Chamber's parts: the Injection Chamber's, copied so the two can diverge. */
	public static final PartialModel OXIDATION_CHAMBER_COG = block("oxidation_chamber/cog");
	public static final PartialModel OXIDATION_CHAMBER_HEAD = block("oxidation_chamber/head");
	public static final PartialModel OXIDATION_CHAMBER_ARROWS = block("oxidation_chamber/arrows");

	/** The Combiner's whisk and pole - the mixer's, copied so they can be reshaped independently. */
	public static final PartialModel COMBINER_HEAD = block("combiner/head");
	public static final PartialModel COMBINER_POLE = block("combiner/pole");

	/**
	 * The Electrolytic Separator's whisk and pole. Same idea as the Combiner's above, but this one
	 * shares {@link me.moonscenty.createkinetism.content.vat.VatRenderer} with the other vats, so the
	 * renderer picks between these and Create's by block - see the branch there.
	 */
	public static final PartialModel ELECTROLYTIC_SEPARATOR_HEAD = block("electrolytic_separator/head");
	public static final PartialModel ELECTROLYTIC_SEPARATOR_POLE = block("electrolytic_separator/pole");

	/** The accumulator's bracket, drawn only when a large cogwheel is sat on top of it. */
	public static final PartialModel KINETIC_ACCUMULATOR_BRACKET = block("kinetic_accumulator/bracket");

	/** The enricher's head and poles, kept out of the block model so it can be animated later. */
	public static final PartialModel MECHANICAL_ENRICHER_HEAD = block("mechanical_enricher/head");

	/** One fan stage; the turbine renderer draws it three times at staggered angles. */
	public static final PartialModel TURBINE_PROPELLER = block("gas_turbine/propeller");

	/** Casing drawn over an encased steel pipe. */
	public static final PartialModel STEEL_PIPE_CASING = block("steel_pipe/casing");

	/**
	 * Rims, drains and connectors a steel pipe grows where it meets something. Create decides which
	 * component belongs on which face; we just supply the model for every combination.
	 */
	public static final Map<FluidTransportBehaviour.AttachmentTypes.ComponentPartials, Map<Direction, PartialModel>>
		STEEL_PIPE_ATTACHMENTS = new EnumMap<>(FluidTransportBehaviour.AttachmentTypes.ComponentPartials.class);

	static {
		for (FluidTransportBehaviour.AttachmentTypes.ComponentPartials type
			: FluidTransportBehaviour.AttachmentTypes.ComponentPartials.values()) {
			Map<Direction, PartialModel> byDirection = new HashMap<>();
			for (Direction d : Iterate.directions)
				byDirection.put(d,
					block("steel_pipe/" + Lang.asId(type.name()) + "/" + Lang.asId(d.getSerializedName())));
			STEEL_PIPE_ATTACHMENTS.put(type, byDirection);
		}
	}

	// The Kinetic Disassembler's own parts, drawn by a custom item renderer rather than a block
	// entity one - see KineticDisassemblerItemRenderer. Everything but the base moves: the cog spins
	// continuously, and the three small blades reciprocate out of phase with each other for the
	// chainsaw look.
	public static final PartialModel
		KINETIC_DISASSEMBLER_BASE = item("kinetic_disassembler/base"),
		KINETIC_DISASSEMBLER_COG = item("kinetic_disassembler/cog"),
		KINETIC_DISASSEMBLER_BLADE_1 = item("kinetic_disassembler/small_blade1"),
		KINETIC_DISASSEMBLER_BLADE_2 = item("kinetic_disassembler/small_blade2"),
		KINETIC_DISASSEMBLER_BLADE_3 = item("kinetic_disassembler/small_blade3");

	private static PartialModel block(String path) {
		return PartialModel.of(CreateKinetism.asResource("block/" + path));
	}

	private static PartialModel item(String path) {
		return PartialModel.of(CreateKinetism.asResource("item/" + path));
	}

	/** Touching the class is enough; the fields register themselves. */
	public static void init() {
	}
}
