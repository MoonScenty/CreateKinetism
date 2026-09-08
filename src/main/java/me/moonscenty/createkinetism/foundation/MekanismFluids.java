package me.moonscenty.createkinetism.foundation;

import java.util.function.Supplier;

import com.google.common.base.Suppliers;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

/**
 * The handful of Mekanism fluids our own machines name in code.
 *
 * <p>Mekanism registers real Minecraft fluids for eighteen of its chemicals, still under
 * {@code mekanism:&lt;name&gt;} and flowing under {@code mekanism:flowing_&lt;name&gt;} - the same
 * convention Registrate uses - so a Create pipe carries them exactly like ours. That is what let the
 * thirteen duplicates in {@code CKFluids} be deleted rather than bridged.</p>
 *
 * <p>Looked up by name rather than imported. Mekanism keeps its registry holders in the main jar and
 * publishes only an {@code api} artifact, and reaching for the main jar to read two constants would
 * put every internal of that mod on our compile classpath for no gain. A registry lookup costs one
 * hash and cannot drift out of sync with what is actually loaded.</p>
 */
public class MekanismFluids {

	/** What a Thermal Boiler Tank turns water into, and what a Gas Turbine burns. */
	public static final Supplier<Fluid> STEAM = of("steam");

	/** The reactor's other coolant loop - see the Thermal Boiler Tank. */
	public static final Supplier<Fluid> SODIUM = of("sodium");

	/**
	 * Memoised: the registry is frozen long before any of this is asked, and these sit in per-tick
	 * paths where a fresh lookup each time would be pure waste.
	 */
	private static Supplier<Fluid> of(String name) {
		return Suppliers.memoize(() -> BuiltInRegistries.FLUID
			.get(ResourceLocation.fromNamespaceAndPath("mekanism", name)))::get;
	}
}
