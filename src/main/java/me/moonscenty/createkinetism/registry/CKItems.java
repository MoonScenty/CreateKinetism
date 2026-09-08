package me.moonscenty.createkinetism.registry;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;

import me.moonscenty.createkinetism.CreateKinetism;

import me.moonscenty.createkinetism.content.boiler.BoilerControllerItem;
import me.moonscenty.createkinetism.content.chemical.ChemicalCanisterItem;
import me.moonscenty.createkinetism.content.tool.KineticDisassemblerItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * The short list of items this mod still registers itself.
 *
 * <p>It used to be long. Mekanism is a required dependency now, so every stand-in written for one
 * of its items - the five-form ore intermediates, the alloys, the mechanisms, the pellets, steel -
 * was deleted and the recipes repointed at the real thing. What is left is what Mekanism does not
 * have: Kinetite, and the three items that belong to machines of ours.</p>
 */
public class CKItems {

	private static final CreateRegistrate REGISTRATE = CreateKinetism.registrate();

	/** Every item we register, in creative-tab order. */
	public static final List<ItemEntry<? extends Item>> ALL = new ArrayList<>();

	// Mekanism's Osmium, reintroduced as Kinetite - see CKBlocks for the ore/raw/storage blocks.
	public static final ItemEntry<Item> RAW_KINETITE = simple("raw_kinetite");
	public static final ItemEntry<Item> CRUSHED_RAW_KINETITE = simple("crushed_raw_kinetite");
	public static final ItemEntry<Item> KINETITE_INGOT = simple("kinetite_ingot");
	public static final ItemEntry<Item> KINETITE_NUGGET = simple("kinetite_nugget");

	/**
	 * Mekanism: Atomic Disassembler. The mod's one tool, and the only thing outside a shaft network
	 * that spends rotation - it is wound at a Kinetic Accumulator rather than charged.
	 */
	public static final ItemEntry<KineticDisassemblerItem> KINETIC_DISASSEMBLER = register(
		REGISTRATE.item("kinetic_disassembler", KineticDisassemblerItem::new)
			.properties(p -> p.rarity(Rarity.RARE))
			.register());

	private static <T extends Item> ItemEntry<T> register(ItemEntry<T> entry) {
		ALL.add(entry);
		return entry;
	}

	/**
	 * The only way to name a gas to a Create filter - see {@link ChemicalCanisterItem} for why a
	 * bucket cannot do it.
	 */
	public static final ItemEntry<ChemicalCanisterItem> CHEMICAL_CANISTER = register(
		REGISTRATE.item("chemical_canister", ChemicalCanisterItem::new)
			.register());

	/**
	 * What the Nutrition Bar Mixer turns food into. Not a food itself, and since the Curios item
	 * that ate it is gone, nothing spends it yet - see the README.
	 */
	public static final ItemEntry<Item> NUTRITION_BAR = simple("nutrition_bar");

	/** Folds a tall Thermal Boiler Tank stack into a boiler, and back - see {@code content.boiler}. */
	public static final ItemEntry<BoilerControllerItem> BOILER_CONTROLLER = register(
		REGISTRATE.item("boiler_controller", BoilerControllerItem::new)
			.register());

	private static ItemEntry<Item> simple(String name) {
		ItemEntry<Item> entry = REGISTRATE.item(name, Item::new)
			.register();
		ALL.add(entry);
		return entry;
	}

	/** Class-loading hook, called from the mod constructor. */
	public static void register() {
	}
}
