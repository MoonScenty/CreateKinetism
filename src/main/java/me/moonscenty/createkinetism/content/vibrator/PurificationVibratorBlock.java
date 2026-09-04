package me.moonscenty.createkinetism.content.vibrator;

import me.moonscenty.createkinetism.content.machine.BasinCarryingBlock;
import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Mekanism's Purification Chamber as a vibrating table.
 *
 * <p>The basin handling and the shaft-through-the-middle placement come from
 * {@link BasinCarryingBlock}; all this adds is which block entity to build.</p>
 */
public class PurificationVibratorBlock extends BasinCarryingBlock<PurificationVibratorBlockEntity> {

	public PurificationVibratorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Class<PurificationVibratorBlockEntity> getBlockEntityClass() {
		return PurificationVibratorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends PurificationVibratorBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.PURIFICATION_VIBRATOR.get();
	}
}
