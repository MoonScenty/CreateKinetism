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
 * column of separate blocks. Each entry pairs a base texture with its {@code _connected} variant.</p>
 */
public class CKSpriteShifts {

	public static final CTSpriteShiftEntry
		STEEL_TANK = getCT(AllCTTypes.RECTANGLE, "steel_tank/steel_fluid_tank"),
		STEEL_TANK_TOP = getCT(AllCTTypes.RECTANGLE, "steel_tank/steel_fluid_tank_top"),
		STEEL_TANK_INNER = getCT(AllCTTypes.RECTANGLE, "steel_tank/steel_fluid_tank_inner");

	private static CTSpriteShiftEntry getCT(CTType type, String blockTextureName) {
		return CTSpriteShifter.getCT(type, CreateKinetism.asResource("block/" + blockTextureName),
			CreateKinetism.asResource("block/" + blockTextureName + "_connected"));
	}
}
