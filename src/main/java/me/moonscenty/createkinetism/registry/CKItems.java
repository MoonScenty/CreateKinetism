package me.moonscenty.createkinetism.registry;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;

import me.moonscenty.createkinetism.CreateKinetism;

import net.minecraft.world.item.Item;

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
	public static final List<ItemEntry<Item>> ALL = new ArrayList<>();

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
