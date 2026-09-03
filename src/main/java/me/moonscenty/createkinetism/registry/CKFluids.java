package me.moonscenty.createkinetism.registry;


import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.fluids.VirtualFluid;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.FluidEntry;

import me.moonscenty.createkinetism.CreateKinetism;

import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.fluids.FluidType;

/**
 * Mekanism's chemicals, expressed as Create-compatible fluids.
 *
 * <p>This is the single biggest design decision in the mod. Mekanism ships four parallel transport
 * networks (gas, infusion, pigment, slurry) with their own pipes, tanks and tank GUIs. Rebuilding
 * those would mean rebuilding half of Create's fluid content as well, and the result would not be
 * pipeable with a Create Mechanical Pump.</p>
 *
 * <p>So every chemical here is a plain fluid. Create's pumps, pipes, valves, spouts, item drains,
 * tanks and basins all work on them unmodified, and Mekanism's chemistry still reads correctly
 * because the recipes - not the transport layer - are what make a Purification Chamber a
 * Purification Chamber.</p>
 *
 * <p>They are <em>virtual</em> fluids (Create's {@code VirtualFluid}): no bucket, no fluid block,
 * not placeable in the world. A bucket of chlorine would be silly, and it keeps the item list
 * short.</p>
 */
public class CKFluids {

	private static final CreateRegistrate REGISTRATE = CreateKinetism.registrate();

	// Create's potion fluid texture is a neutral, tintable liquid. Reusing it means every chemical
	// in this mod is coloured purely by its tint value and we ship no fluid textures at all.
	private static final ResourceLocation STILL =
		ResourceLocation.fromNamespaceAndPath("create", "fluid/potion_still");
	private static final ResourceLocation FLOW =
		ResourceLocation.fromNamespaceAndPath("create", "fluid/potion_flow");

	/** Every chemical registered here, paired with its tint. Populated as the fields below run. */
	private static final List<Chemical> CHEMICALS = new ArrayList<>();

	// --- gases -------------------------------------------------------------------------------

	public static final FluidEntry<VirtualFluid> OXYGEN = chemical("oxygen", 0xFFBBDDFF);
	public static final FluidEntry<VirtualFluid> HYDROGEN = chemical("hydrogen", 0xFFE8E8E8);
	public static final FluidEntry<VirtualFluid> CHLORINE = chemical("chlorine", 0xFFD8E086);
	public static final FluidEntry<VirtualFluid> HYDROGEN_CHLORIDE = chemical("hydrogen_chloride", 0xFFA8C8A0);
	public static final FluidEntry<VirtualFluid> SULFUR_DIOXIDE = chemical("sulfur_dioxide", 0xFFE0C060);
	public static final FluidEntry<VirtualFluid> SULFUR_TRIOXIDE = chemical("sulfur_trioxide", 0xFFE0A040);
	public static final FluidEntry<VirtualFluid> SULFURIC_ACID = chemical("sulfuric_acid", 0xFFE8E060);
	public static final FluidEntry<VirtualFluid> BRINE = chemical("brine", 0xFFDDE7B0);
	public static final FluidEntry<VirtualFluid> LITHIUM = chemical("lithium", 0xFFF2F2F2);

	// --- petrochemicals ------------------------------------------------------------------------
	// Naming follows Petrochem's, so a pack that knows one knows the other: petroleum is what comes
	// up the well (it is what carries the c:crude_oil tag there), and oil is the flash cut. Sour gas
	// is the fraction that carries the sulfur, which is where the Claus process - and therefore the
	// whole sulfuric acid line - starts.
	//
	// The one place we do not copy Petrochem exactly is the spelling: it writes "naphta", and we keep
	// the h. Everything else lines up name for name.

	public static final FluidEntry<VirtualFluid> PETROLEUM = chemical("petroleum", 0xFF1C1A16);
	public static final FluidEntry<VirtualFluid> DESALTED_OIL = chemical("desalted_oil", 0xFF241F1A);
	public static final FluidEntry<VirtualFluid> OIL = chemical("oil", 0xFF2A2620);
	public static final FluidEntry<VirtualFluid> OIL_RESIDUE = chemical("oil_residue", 0xFF33291C);
	public static final FluidEntry<VirtualFluid> HEAVY_OIL_RESIDUE = chemical("heavy_oil_residue", 0xFF241C12);
	public static final FluidEntry<VirtualFluid> OIL_BRINE = chemical("oil_brine", 0xFF6E6A50);

	public static final FluidEntry<VirtualFluid> FUEL_OIL = chemical("fuel_oil", 0xFF4A3A24);
	public static final FluidEntry<VirtualFluid> DIESEL = chemical("diesel", 0xFFB07A3C);
	public static final FluidEntry<VirtualFluid> LIGHT_DIESEL = chemical("light_diesel", 0xFFC69254);
	public static final FluidEntry<VirtualFluid> HEAVY_DIESEL = chemical("heavy_diesel", 0xFF8F6230);
	public static final FluidEntry<VirtualFluid> DESULFURIZED_HEAVY_DIESEL =
		chemical("desulfurized_heavy_diesel", 0xFF9C7038);

	public static final FluidEntry<VirtualFluid> LIGHT_GAS_OIL = chemical("light_gas_oil", 0xFFBFA05C);
	public static final FluidEntry<VirtualFluid> HEAVY_GAS_OIL = chemical("heavy_gas_oil", 0xFF7E6634);
	public static final FluidEntry<VirtualFluid> HYDROTREATED_GAS_OIL = chemical("hydrotreated_gas_oil", 0xFF9A8348);

	public static final FluidEntry<VirtualFluid> KEROSENE = chemical("kerosene", 0xFFC8D8E8);
	public static final FluidEntry<VirtualFluid> DESULFURIZED_KEROSENE = chemical("desulfurized_kerosene", 0xFFD8E4F0);

	public static final FluidEntry<VirtualFluid> HEAVY_NAPHTHA = chemical("heavy_naphtha", 0xFFC9B87A);
	public static final FluidEntry<VirtualFluid> LIGHT_NAPHTHA = chemical("light_naphtha", 0xFFDCCB93);
	public static final FluidEntry<VirtualFluid> DESULFURIZED_HEAVY_NAPHTHA =
		chemical("desulfurized_heavy_naphtha", 0xFFD4C48A);

	public static final FluidEntry<VirtualFluid> GASOLINE = chemical("gasoline", 0xFFE8D24A);
	public static final FluidEntry<VirtualFluid> UNTREATED_GASOLINE = chemical("untreated_gasoline", 0xFFC9B23E);
	public static final FluidEntry<VirtualFluid> HYDROCRACKED_GASOLINE = chemical("hydrocracked_gasoline", 0xFFEFDC5E);

	public static final FluidEntry<VirtualFluid> SOUR_GAS = chemical("sour_gas", 0xFFA8B070);
	public static final FluidEntry<VirtualFluid> NATURAL_GAS = chemical("natural_gas", 0xFFDCE8D0);
	public static final FluidEntry<VirtualFluid> HYDROGEN_SULFIDE = chemical("hydrogen_sulfide", 0xFFD8E8A0);
	public static final FluidEntry<VirtualFluid> VOLATILE_GAS = chemical("volatile_gas", 0xFFE4EEC4);

	public static final FluidEntry<VirtualFluid> LPG = chemical("lpg", 0xFFEFC98A);
	public static final FluidEntry<VirtualFluid> PROPANE = chemical("propane", 0xFFF0D9A4);
	public static final FluidEntry<VirtualFluid> BUTANE = chemical("butane", 0xFFE9C583);
	public static final FluidEntry<VirtualFluid> ETHYLENE = chemical("ethylene", 0xFFDCEBD4);
	public static final FluidEntry<VirtualFluid> NITROGEN = chemical("nitrogen", 0xFFD2DCE8);

	public static final FluidEntry<VirtualFluid> LUBRICANT = chemical("lubricant", 0xFFB89A3E);
	public static final FluidEntry<VirtualFluid> PLASTIC = chemical("plastic", 0xFFDDDDD2);

	// Used by the distillation column rather than as products in their own right: steam drives the
	// flash mode, and air is what a vacuum column has to keep pumping out.
	public static final FluidEntry<VirtualFluid> STEAM = chemical("steam", 0xFFEFEFEF);
	public static final FluidEntry<VirtualFluid> AIR = chemical("air", 0xFFCFE3F0);

	// --- infusions ---------------------------------------------------------------------------
	// Mekanism stores an infusion type inside the Metallurgic Infuser; ours is a spout, so the
	// infusion is a fluid it drips onto the item below. The Oxidation Vat is what turns the solid
	// into it, which is the Chemical Oxidizer's job in Mekanism too.
	public static final FluidEntry<VirtualFluid> REDSTONE_INFUSION = chemical("redstone_infusion", 0xFFD03A3A);
	public static final FluidEntry<VirtualFluid> CARBON_INFUSION = chemical("carbon_infusion", 0xFF3C3C3C);

	// --- slurries ----------------------------------------------------------------------------

	public static final FluidEntry<VirtualFluid> DIRTY_IRON_SLURRY = chemical("dirty_iron_slurry", 0xFF6B5A4E);
	public static final FluidEntry<VirtualFluid> CLEAN_IRON_SLURRY = chemical("clean_iron_slurry", 0xFFC9B49B);
	public static final FluidEntry<VirtualFluid> DIRTY_GOLD_SLURRY = chemical("dirty_gold_slurry", 0xFF7A6329);
	public static final FluidEntry<VirtualFluid> CLEAN_GOLD_SLURRY = chemical("clean_gold_slurry", 0xFFE2C44A);
	public static final FluidEntry<VirtualFluid> DIRTY_COPPER_SLURRY = chemical("dirty_copper_slurry", 0xFF6E4433);
	public static final FluidEntry<VirtualFluid> CLEAN_COPPER_SLURRY = chemical("clean_copper_slurry", 0xFFD07C50);

	private static FluidEntry<VirtualFluid> chemical(String name, int tint) {
		FluidEntry<VirtualFluid> entry = REGISTRATE
			.virtualFluid(name, STILL, FLOW, CKFluids::plainType, VirtualFluid::createSource,
				VirtualFluid::createFlowing)
			.register();
		CHEMICALS.add(new Chemical(entry, tint));
		return entry;
	}

	private static FluidType plainType(FluidType.Properties properties, ResourceLocation still,
		ResourceLocation flow) {
		return new FluidType(properties);
	}

	/**
	 * A registered chemical and the colour its shared texture should be tinted with. The client
	 * extensions that actually apply it are registered in {@code CreateKinetismClient}.
	 */
	public record Chemical(FluidEntry<VirtualFluid> fluid, int tint) {
	}

	public static List<Chemical> chemicals() {
		return CHEMICALS;
	}

	public static ResourceLocation stillTexture() {
		return STILL;
	}

	public static ResourceLocation flowingTexture() {
		return FLOW;
	}

	/** Class-loading hook, called from the mod constructor. */
	public static void register() {
	}
}
