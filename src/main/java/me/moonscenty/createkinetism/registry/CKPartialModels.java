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

	private static PartialModel block(String path) {
		return PartialModel.of(CreateKinetism.asResource("block/" + path));
	}

	/** Touching the class is enough; the fields register themselves. */
	public static void init() {
	}
}
