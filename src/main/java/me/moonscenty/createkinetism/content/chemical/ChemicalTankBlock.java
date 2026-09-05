package me.moonscenty.createkinetism.content.chemical;

import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mekanism: the Metallurgic Infuser's infusion slot, pulled out into a block.
 *
 * <p>Stack it on top of a Mechanical Metallurgic Infuser and it keeps the spout supplied. It is not
 * a Create Fluid Tank despite the shape - it does not combine into a multiblock, because its whole
 * job is to sit in one specific place: the block directly above the machine it feeds.</p>
 */
public class ChemicalTankBlock extends Block implements IBE<ChemicalTankBlockEntity> {

	public ChemicalTankBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Class<ChemicalTankBlockEntity> getBlockEntityClass() {
		return ChemicalTankBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ChemicalTankBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.CHEMICAL_TANK.get();
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, level, pos, newState);
		super.onRemove(state, level, pos, newState, isMoving);
	}
}
