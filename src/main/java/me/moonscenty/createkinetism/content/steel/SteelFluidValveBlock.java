package me.moonscenty.createkinetism.content.steel;

import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlockEntity;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.world.level.block.entity.BlockEntityType;

/** Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md. */
public class SteelFluidValveBlock extends FluidValveBlock {

	public SteelFluidValveBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntityType<? extends FluidValveBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.STEEL_VALVE.get();
	}
}
