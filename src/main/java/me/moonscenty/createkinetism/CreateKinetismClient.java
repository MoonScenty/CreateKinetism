package me.moonscenty.createkinetism;

import me.moonscenty.createkinetism.registry.CKFluids;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Client entrypoint.
 *
 * <p>Block entity renderers are attached through Registrate in {@code CKBlockEntityTypes}, so the
 * only thing left here is telling the client how to draw the chemicals: they all share Create's
 * neutral potion texture and are told apart purely by tint.</p>
 */
@Mod(value = CreateKinetism.ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CreateKinetism.ID, value = Dist.CLIENT)
public class CreateKinetismClient {

	public CreateKinetismClient(ModContainer container) {
		// Touch the holder so its PartialModel fields register themselves for baking.
		CKPartialModels.init();
	}

	@SubscribeEvent
	static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		for (CKFluids.Chemical chemical : CKFluids.chemicals())
			event.registerFluidType(tinted(chemical.tint()), chemical.fluid()
				.get()
				.getFluidType());
	}

	private static IClientFluidTypeExtensions tinted(int tint) {
		return new IClientFluidTypeExtensions() {
			@Override
			public ResourceLocation getStillTexture() {
				return CKFluids.stillTexture();
			}

			@Override
			public ResourceLocation getFlowingTexture() {
				return CKFluids.flowingTexture();
			}

			@Override
			public int getTintColor() {
				return tint;
			}
		};
	}
}
