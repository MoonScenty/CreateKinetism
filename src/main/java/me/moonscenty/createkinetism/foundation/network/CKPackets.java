package me.moonscenty.createkinetism.foundation.network;

import me.moonscenty.createkinetism.CreateKinetism;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** The mod's one packet. Everything else it does happens on blocks the server already ticks. */
public class CKPackets {

	public static void register(RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar(CreateKinetism.ID)
			.versioned("1");
		registrar.playToServer(ElytraBoostPacket.TYPE, ElytraBoostPacket.STREAM_CODEC,
			ElytraBoostPacket::handle);
	}
}
