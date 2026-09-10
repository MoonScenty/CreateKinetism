package me.moonscenty.createkinetism.registry;

import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalBuilder;

import me.moonscenty.createkinetism.CreateKinetism;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Every gas this mod registers.
 *
 * <p>These were all virtual fluids in {@link CKFluids} once, which made them things a Create pipe
 * could carry. That was always a compromise: a gas in a pipe reads as a liquid, and it kept them
 * out of every Mekanism machine and tube in the pack. Registering them here instead puts them in the
 * same registry as hydrogen and oxygen, so a pressurized tube moves them, a chemical tank stores
 * them, and the Gas Turbine can insist on gases and mean it.</p>
 *
 * <p>The cost is that they no longer travel in fluid pipes at all. That is the point: the
 * distillation column taps its gaseous cuts through a chemical tank (see
 * {@code DistillationOutputBlockEntity}), the turbine burns them out of one, and the column's own
 * air leaves the same way.</p>
 *
 * <p>Nothing virtual is left in {@link CKFluids} as a result - everything registered there now is a
 * pourable liquid with a bucket.</p>
 *
 * <p>No textures of our own - Mekanism's default chemical texture is a neutral, tintable liquid, so
 * these are coloured purely by tint, exactly as the fluids were. The tints are carried over
 * unchanged so nothing looks different from before, minus the alpha byte a chemical does not
 * take.</p>
 */
public class CKChemicals {

	private static final DeferredRegister<Chemical> CHEMICALS =
		DeferredRegister.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, CreateKinetism.ID);

	/** Wet natural gas, straight off the column. Sour because of the hydrogen sulfide in it. */
	public static final DeferredHolder<Chemical, Chemical> SOUR_GAS = gas("sour_gas", 0xA8B070);

	/** Sweetened gas. No source yet - the Claus process that made it is still to be rebuilt. */
	public static final DeferredHolder<Chemical, Chemical> NATURAL_GAS = gas("natural_gas", 0xDCE8D0);

	/** The lightest cut a flash column throws off, and the turbine's cheap fuel. */
	public static final DeferredHolder<Chemical, Chemical> LPG = gas("lpg", 0xEFC98A);

	/** LPG's two halves, once there is something to split it with. */
	public static final DeferredHolder<Chemical, Chemical> PROPANE = gas("propane", 0xF0D9A4);
	public static final DeferredHolder<Chemical, Chemical> BUTANE = gas("butane", 0xE9C583);

	/** What the Claus process runs on. Nothing makes it yet - see the README. */
	public static final DeferredHolder<Chemical, Chemical> HYDROGEN_SULFIDE = gas("hydrogen_sulfide", 0xD8E8A0);

	public static final DeferredHolder<Chemical, Chemical> VOLATILE_GAS = gas("volatile_gas", 0xE4EEC4);
	public static final DeferredHolder<Chemical, Chemical> NITROGEN = gas("nitrogen", 0xD2DCE8);

	/**
	 * Not a product in its own right: air is what a vacuum column has to keep pushing out, and what
	 * the Air Pump exists to throw away. The column refills its own supply every tick, so holding a
	 * vacuum means removing this faster than it comes back.
	 */
	public static final DeferredHolder<Chemical, Chemical> AIR = gas("air", 0xCFE3F0);

	/**
	 * Kinetite in chemical form, the way Mekanism keeps an osmium one alongside the metal.
	 *
	 * <p>Not a gas in the way the petrochemicals are - it is out of the {@code mekanism:gaseous}
	 * tag for the same reason osmium is, so it renders as a liquid rather than a haze. Nothing makes
	 * or spends it yet.</p>
	 */
	public static final DeferredHolder<Chemical, Chemical> KINETITE = gas("kinetite", 0xEE9B73);

	private static DeferredHolder<Chemical, Chemical> gas(String name, int tint) {
		return CHEMICALS.register(name, () -> new Chemical(ChemicalBuilder.builder()
			.tint(tint)));
	}

	public static void register(IEventBus modEventBus) {
		CHEMICALS.register(modEventBus);
	}
}
