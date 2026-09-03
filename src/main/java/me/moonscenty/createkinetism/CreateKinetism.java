package me.moonscenty.createkinetism;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;

import me.moonscenty.createkinetism.config.CKConfigs;
import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKCreativeTabs;
import me.moonscenty.createkinetism.registry.CKDataComponents;
import me.moonscenty.createkinetism.registry.CKFluids;
import me.moonscenty.createkinetism.registry.CKItems;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;
import me.moonscenty.createkinetism.registry.CKSoundEvents;

import net.createmod.catnip.lang.FontHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.resources.ResourceKey;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Create: Kinetism.
 *
 * <p>Mekanism's machine roster, rebuilt on top of Create's rotational force. Every machine in this
 * mod is powered by stress/RPM only - there is no Forge Energy anywhere in the codebase.</p>
 */
@Mod(CreateKinetism.ID)
public class CreateKinetism {

	public static final String ID = "createkinetism";
	public static final String NAME = "Create: Kinetism";

	public static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * Our own {@link CreateRegistrate}. Create explicitly forbids addons from using its instance, so
	 * we create one here and hand it Create's tooltip machinery, which is what makes our machines
	 * show "Stress Impact" and goggle tooltips exactly like Create's own blocks do.
	 */
	private static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID)
		.defaultCreativeTab((ResourceKey<CreativeModeTab>) null)
		.setTooltipModifierFactory(item ->
			new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
				.andThen(TooltipModifier.mapNull(KineticStats.create(item))));

	public CreateKinetism(IEventBus modEventBus, ModContainer modContainer) {
		LOGGER.info("{} initialising", NAME);

		REGISTRATE.registerEventListeners(modEventBus);

		CKCreativeTabs.register(modEventBus);
		CKFluids.register();
		CKDataComponents.register(modEventBus);
		CKItems.register();
		CKBlocks.register();
		CKBlockEntityTypes.register();
		CKRecipeTypes.register(modEventBus);
		CKSoundEvents.register(modEventBus);

		// After registration, so every setImpact/setCapacity default is in hand before the spec is built.
		CKConfigs.register(modContainer);

		modEventBus.addListener(CreateKinetism::registerCapabilities);
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		CKBlockEntityTypes.registerCapabilities(event);
	}

	public static CreateRegistrate registrate() {
		return REGISTRATE;
	}

	public static ResourceLocation asResource(String path) {
		return ResourceLocation.fromNamespaceAndPath(ID, path);
	}
}
