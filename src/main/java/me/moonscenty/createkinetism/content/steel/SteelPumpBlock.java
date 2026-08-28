package me.moonscenty.createkinetism.content.steel;

import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.world.level.block.entity.BlockEntityType;

/** Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md. */
public class SteelPumpBlock extends PumpBlock {

	public SteelPumpBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntityType<? extends PumpBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.STEEL_PUMP.get();
	}
}
