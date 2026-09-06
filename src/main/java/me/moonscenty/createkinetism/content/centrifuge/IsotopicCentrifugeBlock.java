package me.moonscenty.createkinetism.content.centrifuge;

import me.moonscenty.createkinetism.content.machine.BasinCarryingBlock;
import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Mekanism's Isotopic Centrifuge as a turntable.
 *
 * <p>Same chassis as the Dissolution Chamber - shaft through the middle, a basin fitted into the
 * machine rather than placed below it - and the same model. What differs is what the table does with
 * it; see {@link IsotopicCentrifugeBlockEntity#getSwingAngle}.</p>
 */
public class IsotopicCentrifugeBlock extends BasinCarryingBlock<IsotopicCentrifugeBlockEntity> {

	public IsotopicCentrifugeBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Class<IsotopicCentrifugeBlockEntity> getBlockEntityClass() {
		return IsotopicCentrifugeBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends IsotopicCentrifugeBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.ISOTOPIC_CENTRIFUGE.get();
	}
}
