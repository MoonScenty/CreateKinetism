package me.moonscenty.createkinetism.registry;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.CTType;

import me.moonscenty.createkinetism.CreateKinetism;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Connected textures for the steel tank, so a stack of them reads as one vessel instead of a
 * column of separate blocks. Each entry pairs a plain texture with the connected one that replaces
 * it where two tanks meet.</p>
 */
public class CKSpriteShifts {

	public static final CTSpriteShiftEntry
		STEEL_TANK = getCT(AllCTTypes.RECTANGLE, "steel_tank/base", "steel_tank/connected"),
		STEEL_TANK_TOP = getCT(AllCTTypes.RECTANGLE, "steel_tank/top", "steel_tank/top_connected"),
		STEEL_TANK_INNER =
			getCT(AllCTTypes.RECTANGLE, "steel_tank/inner", "steel_tank/inner_connected");

	// Both names spelled out rather than appending a suffix: the plain one is "base" but its partner
	// is "connected", not "base_connected", so there is no suffix that holds for all three.
	private static CTSpriteShiftEntry getCT(CTType type, String plain, String connected) {
		return CTSpriteShifter.getCT(type, CreateKinetism.asResource("block/" + plain),
			CreateKinetism.asResource("block/" + connected));
	}
}
