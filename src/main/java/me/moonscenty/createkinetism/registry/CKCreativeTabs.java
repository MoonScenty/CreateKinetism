package me.moonscenty.createkinetism.registry;

import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;

import me.moonscenty.createkinetism.CreateKinetism;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
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
				for (BlockEntry<?> block : CKBlocks.ALL)
					output.accept(block.get());
				for (ItemEntry<Item> item : CKItems.ALL)
					output.accept(item.get());
			})
			.build());

	public static void register(IEventBus modEventBus) {
		TABS.register(modEventBus);
	}
}
