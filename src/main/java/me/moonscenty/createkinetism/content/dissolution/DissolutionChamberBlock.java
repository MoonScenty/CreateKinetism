package me.moonscenty.createkinetism.content.dissolution;

import me.moonscenty.createkinetism.content.machine.BasinCarryingBlock;
import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Mekanism's Chemical Dissolution Chamber as a rocking table.
 *
 * <p>The basin handling and the shaft-through-the-middle placement come from
 * {@link BasinCarryingBlock}; all this adds is which block entity to build.</p>
 */
public class DissolutionChamberBlock extends BasinCarryingBlock<DissolutionChamberBlockEntity> {

	public DissolutionChamberBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Class<DissolutionChamberBlockEntity> getBlockEntityClass() {
		return DissolutionChamberBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends DissolutionChamberBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.DISSOLUTION_CHAMBER.get();
	}
}
