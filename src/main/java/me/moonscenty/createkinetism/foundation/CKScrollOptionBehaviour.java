package me.moonscenty.createkinetism.foundation;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * A {@link ScrollOptionBehaviour} that survives reading a value it cannot represent.
 *
 * <p>Create's own does not clamp. {@code ScrollValueBehaviour.read} assigns {@code ScrollValue}
 * straight out of NBT, and {@code ScrollOptionBehaviour.get} then indexes the enum with it - and
 * every scroll behaviour shares one {@code BehaviourType}, so a block saved when its dial was a
 * free-range number hands that number to whatever behaviour occupies the slot afterwards. Our
 * engines did exactly that: their dial used to be an RPM value that defaulted to 64, and reading one
 * of those back threw {@code ArrayIndexOutOfBoundsException: Index 64 out of bounds for length 2}
 * from the value box renderer.</p>
 *
 * <p>Clamping on read costs nothing and makes an engine placed before the change come back as a
 * valid option instead of crashing the client, which matters for anyone updating a world rather than
 * starting one.</p>
 */
public class CKScrollOptionBehaviour<E extends Enum<E> & INamedIconOptions> extends ScrollOptionBehaviour<E> {

	public CKScrollOptionBehaviour(Class<E> options, Component label, SmartBlockEntity be, ValueBoxTransform slot) {
		super(options, label, be, slot);
	}

	@Override
	public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(nbt, registries, clientPacket);
		// max is set to options.length - 1 by the superclass constructor, and options always start at 0.
		value = Mth.clamp(value, 0, max);
	}
}
