package me.moonscenty.createkinetism.content.steel;

import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlockEntity;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.world.level.block.entity.BlockEntityType;

/** Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md. */
public class SteelSmartPipeBlock extends SmartFluidPipeBlock {

	public SteelSmartPipeBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntityType<? extends SmartFluidPipeBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.STEEL_SMART_PIPE.get();
	}
}
