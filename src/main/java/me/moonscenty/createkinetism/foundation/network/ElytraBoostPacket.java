package me.moonscenty.createkinetism.foundation.network;

import me.moonscenty.createkinetism.CreateKinetism;
import me.moonscenty.createkinetism.content.curio.KineticElytraItem;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * "I pressed the boost key." Nothing else - the packet carries no numbers on purpose.
 *
 * <p>A client that sends this is asking, not telling: the server looks up what the player is
 * actually wearing, checks the charge itself, and applies the impulse. Anything sent in the payload
 * would be a figure a modified client could choose.</p>
 */
public record ElytraBoostPacket() implements CustomPacketPayload {

	public static final Type<ElytraBoostPacket> TYPE =
		new Type<>(CreateKinetism.asResource("elytra_boost"));

	public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, ElytraBoostPacket> STREAM_CODEC =
		StreamCodec.unit(new ElytraBoostPacket());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(ElytraBoostPacket packet, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer player)
				KineticElytraItem.boost(player);
		});
	}
}
