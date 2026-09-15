package me.moonscenty.createkinetism.content.condensentrator;

import me.moonscenty.createkinetism.content.machine.BasinCarryingBlock;
import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Mekanism's Rotary Condensentrator as the Dissolution Chamber turned upside down: the housing on top,
 * the table hanging under it, and the basin carried below the block rather than above it.
 *
 * <p>Basin fitting and the shaft come from {@link BasinCarryingBlock}, as for the chamber. What it does
 * - Mekanism's rotary recipes, one way or the other by the heat under the basin - is in
 * {@link MechanicalCondensentratorBlockEntity}.</p>
 */
public class MechanicalCondensentratorBlock extends BasinCarryingBlock<MechanicalCondensentratorBlockEntity> {

	public MechanicalCondensentratorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Class<MechanicalCondensentratorBlockEntity> getBlockEntityClass() {
		return MechanicalCondensentratorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends MechanicalCondensentratorBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.MECHANICAL_CONDENSENTRATOR.get();
	}
}
