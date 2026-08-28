package me.moonscenty.createkinetism.registry;

import me.moonscenty.createkinetism.CreateKinetism;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Engine loops. Sound files ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Petrochem carries a copy of Create's whole {@code AllSoundEvents} builder for these three
 * entries; a plain deferred register plus a hand-written {@code sounds.json} does the same job.</p>
 */
public class CKSoundEvents {

	private static final DeferredRegister<SoundEvent> SOUNDS =
		DeferredRegister.create(Registries.SOUND_EVENT, CreateKinetism.ID);

	public static final DeferredHolder<SoundEvent, SoundEvent> ENGINE = create("engine");
	public static final DeferredHolder<SoundEvent, SoundEvent> DIESEL = create("diesel");
	public static final DeferredHolder<SoundEvent, SoundEvent> TURBINE = create("turbine");

	private static DeferredHolder<SoundEvent, SoundEvent> create(String name) {
		ResourceLocation id = CreateKinetism.asResource(name);
		return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
	}

	public static void register(IEventBus modEventBus) {
		SOUNDS.register(modEventBus);
	}
}
