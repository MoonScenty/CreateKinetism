package me.moonscenty.createkinetism.content.tool;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * How hard the Disassembler is winding down.
 *
 * <p>Mekanism's tool trades energy for speed on a dial, and that is the part worth keeping: the same
 * tool is a careful chisel or a bulldozer depending on how much you are willing to spend. The costs
 * are in stress units times ticks, matched against what the Kinetic Accumulator can actually store,
 * so a full winding is a few thousand blocks at the middle setting.</p>
 */
public enum DisassemblerMode implements StringRepresentable {

	/** Winding disengaged: the tool costs nothing and mines like a bare hand. */
	OFF("off", 1.0f, 0),
	SLOW("slow", 4.0f, 8),
	NORMAL("normal", 12.0f, 32),
	FAST("fast", 40.0f, 128);

	public static final Codec<DisassemblerMode> CODEC = StringRepresentable.fromEnum(DisassemblerMode::values);
	public static final StreamCodec<ByteBuf, DisassemblerMode> STREAM_CODEC =
		ByteBufCodecs.idMapper(i -> values()[i], DisassemblerMode::ordinal);

	private final String name;
	private final float speed;
	private final int cost;

	DisassemblerMode(String name, float speed, int cost) {
		this.name = name;
		this.speed = speed;
		this.cost = cost;
	}

	/** Flat mining speed, applied to every block alike - hardness is what the winding overrides. */
	public float speed() {
		return speed;
	}

	/** Winding spent per block broken. */
	public int cost() {
		return cost;
	}

	public DisassemblerMode next() {
		return values()[(ordinal() + 1) % values().length];
	}

	public String translationKey() {
		return "createkinetism.tooltip.disassembler.mode." + name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
