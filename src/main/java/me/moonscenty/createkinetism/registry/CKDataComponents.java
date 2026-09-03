package me.moonscenty.createkinetism.registry;

import com.mojang.serialization.Codec;

import me.moonscenty.createkinetism.CreateKinetism;
import me.moonscenty.createkinetism.content.tool.DisassemblerMode;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data components for the mod's items.
 *
 * <p>1.21 moved item state out of free-form NBT and into typed components, so anything the
 * Disassembler remembers between ticks - how much winding is left in it, which mode it is on - is
 * declared here rather than written into a tag by hand.</p>
 */
public class CKDataComponents {

	private static final DeferredRegister<DataComponentType<?>> TYPES =
		DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CreateKinetism.ID);

	/**
	 * Stored winding, in stress units times ticks - the same unit the Kinetic Accumulator buffers in,
	 * because that is where it comes from. One unit is one SU applied for one tick, so the number is
	 * a real record of rotation actually paid for rather than a separate energy currency.
	 */
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHARGE =
		TYPES.register("charge", () -> DataComponentType.<Integer>builder()
			.persistent(Codec.INT)
			.networkSynchronized(ByteBufCodecs.VAR_INT)
			.build());

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<DisassemblerMode>> MODE =
		TYPES.register("mode", () -> DataComponentType.<DisassemblerMode>builder()
			.persistent(DisassemblerMode.CODEC)
			.networkSynchronized(DisassemblerMode.STREAM_CODEC)
			.build());

	public static void register(IEventBus modEventBus) {
		TYPES.register(modEventBus);
	}
}
