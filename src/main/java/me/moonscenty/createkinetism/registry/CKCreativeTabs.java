package me.moonscenty.createkinetism.registry;

import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;

import me.moonscenty.createkinetism.CreateKinetism;
import me.moonscenty.createkinetism.content.tool.KineticDisassemblerItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** A single tab holding every machine followed by every intermediate. */
public class CKCreativeTabs {

	private static final DeferredRegister<CreativeModeTab> TABS =
		DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateKinetism.ID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
		() -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup.createkinetism"))
			.icon(() -> new ItemStack(CKBlocks.MECHANICAL_ENRICHER.get()))
			.displayItems((parameters, output) -> {
				// Skipping the ones with no item on purpose: a block registered without .item() - the
				// compressor's cradle, say - reports air as its item, and a tab handed a zero-count
				// stack throws rather than ignoring it.
				for (BlockEntry<?> block : CKBlocks.ALL) {
					Item item = block.get()
						.asItem();
					if (item != Items.AIR)
						output.accept(item);
				}
				for (ItemEntry<? extends Item> item : CKItems.ALL)
					output.accept(item.get());

				// A second, ready-to-use stack beside the empty one - grabbing a Disassembler to test
				// with shouldn't also mean grabbing a Kinetic Accumulator to wind it first.
				ItemStack chargedDisassembler = new ItemStack(CKItems.KINETIC_DISASSEMBLER.get());
				KineticDisassemblerItem.setCharge(chargedDisassembler, KineticDisassemblerItem.CAPACITY);
				output.accept(chargedDisassembler);
			})
			.build());

	public static void register(IEventBus modEventBus) {
		TABS.register(modEventBus);
	}
}
