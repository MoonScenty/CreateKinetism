package me.moonscenty.createkinetism.content.curio.client;

import com.mojang.blaze3d.platform.InputConstants;

import me.moonscenty.createkinetism.CreateKinetism;
import me.moonscenty.createkinetism.foundation.network.ElytraBoostPacket;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

import org.lwjgl.glfw.GLFW;

/**
 * The boost key.
 *
 * <p>Bound to the backtick, which vanilla leaves free - none of its thirty-four default mappings
 * claim it. Held rather than tapped: a boost every tick would be the wrong feel and the charge cost
 * is what limits it anyway.</p>
 */
@EventBusSubscriber(modid = CreateKinetism.ID, value = Dist.CLIENT)
public class CKKeys {

	public static final KeyMapping ELYTRA_BOOST = new KeyMapping("key.createkinetism.elytra_boost",
		KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT,
		"key.categories.createkinetism");

	@SubscribeEvent
	static void register(RegisterKeyMappingsEvent event) {
		event.register(ELYTRA_BOOST);
	}

	/**
	 * The client only ever asks. Every check that matters - is anything worn, is there charge - is the
	 * server's, so a client that sends this at will gets nothing it has not paid for.
	 */
	@EventBusSubscriber(modid = CreateKinetism.ID, value = Dist.CLIENT)
	public static class Ticker {

		@SubscribeEvent
		static void onClientTick(ClientTickEvent.Post event) {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player == null || minecraft.screen != null)
				return;
			while (ELYTRA_BOOST.consumeClick())
				net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ElytraBoostPacket());
		}
	}
}
