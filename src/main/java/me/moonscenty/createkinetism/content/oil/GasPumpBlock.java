package me.moonscenty.createkinetism.content.oil;

import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Create's Mechanical Pump, moving a Mekanism chemical instead of a fluid.
 *
 * <p>A shell over Create's block, for the same reason {@link SmartGasPipeBlock} is one. The moving
 * is {@link GasPumpBlockEntity}'s.</p>
 */
public class GasPumpBlock extends PumpBlock {

	public GasPumpBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntityType<? extends PumpBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.GAS_PUMP.get();
	}
}
