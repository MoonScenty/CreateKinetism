package me.moonscenty.createkinetism.registry;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;

import me.moonscenty.createkinetism.CreateKinetism;

import me.moonscenty.createkinetism.content.tool.KineticDisassemblerItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/**
 * The intermediates of Mekanism's ore chain.
 *
 * <p>Five forms per metal, in the order they appear as you climb from 2x to 5x:
 * dust (2x), clump and dirty dust (3x), shard (4x), crystal (5x). The chain always funnels back
 * down into dust, which is what actually gets smelted.</p>
 *
 * <p>Only iron, gold and copper ship with the mod. Everything is keyed off {@code c:} tags in the
 * recipes, so an addon or a pack can bolt on osmium, tin, lead or uranium without touching this
 * class.</p>
 */
public class CKItems {

	private static final CreateRegistrate REGISTRATE = CreateKinetism.registrate();

	/** Every item we register, in creative-tab order. */
	public static final List<ItemEntry<? extends Item>> ALL = new ArrayList<>();

	public static final ItemEntry<Item> IRON_DUST = simple("iron_dust");
	public static final ItemEntry<Item> IRON_CLUMP = simple("iron_clump");
	public static final ItemEntry<Item> DIRTY_IRON_DUST = simple("dirty_iron_dust");
	public static final ItemEntry<Item> IRON_SHARD = simple("iron_shard");
	public static final ItemEntry<Item> IRON_CRYSTAL = simple("iron_crystal");

	public static final ItemEntry<Item> GOLD_DUST = simple("gold_dust");
	public static final ItemEntry<Item> GOLD_CLUMP = simple("gold_clump");
	public static final ItemEntry<Item> DIRTY_GOLD_DUST = simple("dirty_gold_dust");
	public static final ItemEntry<Item> GOLD_SHARD = simple("gold_shard");
	public static final ItemEntry<Item> GOLD_CRYSTAL = simple("gold_crystal");

	public static final ItemEntry<Item> COPPER_DUST = simple("copper_dust");
	public static final ItemEntry<Item> COPPER_CLUMP = simple("copper_clump");
	public static final ItemEntry<Item> DIRTY_COPPER_DUST = simple("dirty_copper_dust");
	public static final ItemEntry<Item> COPPER_SHARD = simple("copper_shard");
	public static final ItemEntry<Item> COPPER_CRYSTAL = simple("copper_crystal");

	/** Feedstock for the sulfuric acid line. */
	public static final ItemEntry<Item> SULFUR_DUST = simple("sulfur_dust");

	// Mekanism's steel line, and the only reason the Metallurgic Infuser exists in this mod:
	// iron + redstone infusion -> enriched iron, enriched iron + carbon infusion -> steel dust.
	public static final ItemEntry<Item> ENRICHED_IRON = simple("enriched_iron");
	public static final ItemEntry<Item> STEEL_DUST = simple("steel_dust");
	public static final ItemEntry<Item> STEEL_INGOT = simple("steel_ingot");

	// Mekanism's Osmium, reintroduced as Kinetite - see CKBlocks for the ore/raw/storage blocks.
	public static final ItemEntry<Item> RAW_KINETITE = simple("raw_kinetite");
	public static final ItemEntry<Item> CRUSHED_RAW_KINETITE = simple("crushed_raw_kinetite");
	public static final ItemEntry<Item> KINETITE_INGOT = simple("kinetite_ingot");
	public static final ItemEntry<Item> KINETITE_NUGGET = simple("kinetite_nugget");

	// Kinetite Compressor output. Mekanism melts osmium into a fluid to make these; we press a solid
	// Kinetite ingot into the dust instead, so the whole line stays one item and one machine.
	public static final ItemEntry<Item> REFINED_OBSIDIAN_INGOT = simple("refined_obsidian_ingot");
	public static final ItemEntry<Item> REFINED_GLOWSTONE_INGOT = simple("refined_glowstone_ingot");

	// Mekanism's Control Circuit ladder - Basic, Advanced, Elite, Ultimate - and the item every
	// tiered thing in Mekanism is gated behind. Named for Create's Precision Mechanism rather than
	// for a circuit, because that is what they are here: the same gadget in four grades, with the
	// tier read off the colour.
	public static final ItemEntry<Item> BASIC_MECHANISM = simple("basic_mechanism");
	public static final ItemEntry<Item> ADVANCED_MECHANISM = simple("advanced_mechanism");
	public static final ItemEntry<Item> ELITE_MECHANISM = simple("elite_mechanism");
	public static final ItemEntry<Item> ULTIMATE_MECHANISM = simple("ultimate_mechanism");

	// The alloys that carry each tier. Mekanism has no alloy for the Basic tier, so there are
	// three of these against four mechanisms - the colours say which mechanism each one feeds.
	public static final ItemEntry<Item> INFUSED_ALLOY = simple("infused_alloy");
	public static final ItemEntry<Item> REINFORCED_ALLOY = simple("reinforced_alloy");
	public static final ItemEntry<Item> ATOMIC_ALLOY = simple("atomic_alloy");

	// Mekanism's Enrichment Chamber output, and what an infusion is actually made of. Mekanism
	// feeds these to the Metallurgic Infuser as a chemical; here a Mixer dissolves one into a base
	// potion to get the infusion fluid our own infuser drips. Note ENRICHED_IRON above is not one
	// of these - that is the steel intermediate, not an infusion source.
	public static final ItemEntry<Item> ENRICHED_REDSTONE = simple("enriched_redstone");
	public static final ItemEntry<Item> ENRICHED_CARBON = simple("enriched_carbon");
	public static final ItemEntry<Item> ENRICHED_DIAMOND = simple("enriched_diamond");
	public static final ItemEntry<Item> ENRICHED_OBSIDIAN = simple("enriched_obsidian");

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
