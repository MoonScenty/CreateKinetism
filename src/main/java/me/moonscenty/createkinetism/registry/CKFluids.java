package me.moonscenty.createkinetism.registry;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.List;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.FluidEntry;

import me.moonscenty.createkinetism.CreateKinetism;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * The petrochemical line, expressed as Create-compatible fluids.
 *
 * <p>This class used to hold Mekanism's chemistry too. It no longer does: Mekanism is a required
 * dependency, and it registers real Minecraft fluids for eighteen of its chemicals - hydrogen,
 * oxygen, chlorine, the sulfur pair, sulfuric acid, ethene, sodium, steam and the rest - so the
 * thirteen we had duplicating those were deleted and every recipe repointed at Mekanism's.</p>
 *
 * <p>What Mekanism does <em>not</em> give a fluid to is its slurries, its infuse types and the
 * nuclear chain; those exist only as {@code Chemical}, which a Create pipe cannot carry. None of
 * them are re-registered here. The Metallurgic Infuser reaches for Mekanism's chemicals directly
 * instead - it holds an {@code IChemicalTank}, not one of these - and the slurries and the
 * nuclear chain are still open, so the ore chain above 2x stays dark. See the README.</p>
 *
 * <p>What is left here is Petrochem's liquids, which Mekanism has no counterpart for: crude oil and
 * its cuts. Every one of them is pourable - see {@link #oil} - with a source, a flowing form, a
 * fluid block and a bucket, because these are things you would plausibly pour and a bucket is what
 * lets them cross into other mods and out of a pipe network.</p>
 *
 * <p>There is nothing <em>virtual</em> left here. Create's {@code VirtualFluid} was how this mod
 * used to express a gas - no bucket, no fluid block, pipe-only - but a gas in a fluid pipe reads as
 * a liquid and could not enter a single Mekanism machine or tube. Every one of them is a chemical
 * now; see {@link CKChemicals}.</p>
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

	/** Lazily filled by {@link #tintOf}. */
	private static final Map<Fluid, Integer> TINTS = new IdentityHashMap<>();

	public static final FluidEntry<BaseFlowingFluid.Flowing> PETROLEUM = oil("petroleum", 0xFF1C1A16);
	public static final FluidEntry<BaseFlowingFluid.Flowing> DESALTED_OIL = oil("desalted_oil", 0xFF241F1A);
	public static final FluidEntry<BaseFlowingFluid.Flowing> OIL = oil("oil", 0xFF2A2620);
	public static final FluidEntry<BaseFlowingFluid.Flowing> OIL_RESIDUE = oil("oil_residue", 0xFF33291C);
	public static final FluidEntry<BaseFlowingFluid.Flowing> HEAVY_OIL_RESIDUE = oil("heavy_oil_residue", 0xFF241C12);
	public static final FluidEntry<BaseFlowingFluid.Flowing> OIL_BRINE = oil("oil_brine", 0xFF6E6A50);

	public static final FluidEntry<BaseFlowingFluid.Flowing> FUEL_OIL = oil("fuel_oil", 0xFF4A3A24);
	public static final FluidEntry<BaseFlowingFluid.Flowing> DIESEL = oil("diesel", 0xFFB07A3C);
	public static final FluidEntry<BaseFlowingFluid.Flowing> LIGHT_DIESEL = oil("light_diesel", 0xFFC69254);
	public static final FluidEntry<BaseFlowingFluid.Flowing> HEAVY_DIESEL = oil("heavy_diesel", 0xFF8F6230);
	public static final FluidEntry<BaseFlowingFluid.Flowing> DESULFURIZED_HEAVY_DIESEL =
		oil("desulfurized_heavy_diesel", 0xFF9C7038);

	public static final FluidEntry<BaseFlowingFluid.Flowing> LIGHT_GAS_OIL = oil("light_gas_oil", 0xFFBFA05C);
	public static final FluidEntry<BaseFlowingFluid.Flowing> HEAVY_GAS_OIL = oil("heavy_gas_oil", 0xFF7E6634);
	public static final FluidEntry<BaseFlowingFluid.Flowing> HYDROTREATED_GAS_OIL = oil("hydrotreated_gas_oil", 0xFF9A8348);

	public static final FluidEntry<BaseFlowingFluid.Flowing> KEROSENE = oil("kerosene", 0xFFC8D8E8);
	public static final FluidEntry<BaseFlowingFluid.Flowing> DESULFURIZED_KEROSENE = oil("desulfurized_kerosene", 0xFFD8E4F0);

	public static final FluidEntry<BaseFlowingFluid.Flowing> HEAVY_NAPHTHA = oil("heavy_naphtha", 0xFFC9B87A);
	public static final FluidEntry<BaseFlowingFluid.Flowing> LIGHT_NAPHTHA = oil("light_naphtha", 0xFFDCCB93);
	public static final FluidEntry<BaseFlowingFluid.Flowing> DESULFURIZED_HEAVY_NAPHTHA =
		oil("desulfurized_heavy_naphtha", 0xFFD4C48A);

	public static final FluidEntry<BaseFlowingFluid.Flowing> GASOLINE = oil("gasoline", 0xFFE8D24A);
	public static final FluidEntry<BaseFlowingFluid.Flowing> UNTREATED_GASOLINE = oil("untreated_gasoline", 0xFFC9B23E);
	public static final FluidEntry<BaseFlowingFluid.Flowing> HYDROCRACKED_GASOLINE = oil("hydrocracked_gasoline", 0xFFEFDC5E);

	public static final FluidEntry<BaseFlowingFluid.Flowing> LUBRICANT = oil("lubricant", 0xFFB89A3E);


	/**
	 * A pourable petrochemical: source, flowing, a fluid block and a bucket.
	 *
	 * <p>The block and the bucket come from Registrate's own defaults, and the bucket comes back with a
	 * plain bucket as its crafting remainder. The bucket's sprite is two layers, a plain bucket and
	 * a grey liquid that {@code CreateKinetismClient} tints per fluid, so all 23 share one pair of
	 * textures rather than shipping 23 hand-painted icons.</p>
	 */
	private static FluidEntry<BaseFlowingFluid.Flowing> oil(String name, int tint) {
		// The source, the block and the bucket all come from Registrate's own defaults, which
		// FluidBuilder arms at construction and applies in register(). Reaching for bucket() here
		// would only break it: that call needs a source fluid that does not exist until register().
		FluidEntry<BaseFlowingFluid.Flowing> entry = REGISTRATE
			// The same two textures the virtual ones declare, so nothing that reads a fluid's
			// declared sprite sees a name we never shipped.
			.fluid(name, STILL, FLOW, CKFluids::plainType)
			// Doubly wrapped on purpose: RenderType is client-only, and this keeps it off the
			// classloader on a server.
			.renderType(() -> RenderType::translucent)
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
	public record Chemical(FluidEntry<? extends BaseFlowingFluid> fluid, int tint) {
	}

	/**
	 * The colour registered for a fluid, or {@code -1} if it is not one of ours. Built on first
	 * ask, because the list is only complete once every field above has run.
	 */
	public static int tintOf(Fluid fluid) {
		if (TINTS.isEmpty())
			for (Chemical chemical : CHEMICALS) {
				TINTS.put(chemical.fluid()
					.get(), chemical.tint());
				TINTS.put(chemical.fluid()
					.get()
					.getSource(), chemical.tint());
			}
		return TINTS.getOrDefault(fluid, -1);
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
