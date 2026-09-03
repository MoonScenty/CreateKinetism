package me.moonscenty.createkinetism.content.injection;

import me.moonscenty.createkinetism.content.vat.VatBlock;
import me.moonscenty.createkinetism.content.vat.VatBlockEntity;
import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Injection Chamber, a vat that also carries a tank of its own.
 *
 * <p>Placement and drive are the mixer's, exactly like the other vats and the Combiner. What differs
 * is the block entity, which holds the gas tank rather than an infusion slot, and the shape: the
 * vats' bored-out {@code MECHANICAL_PROCESSOR_SHAPE} fit Create's own mixer body, but this housing is
 * its own model with its own proportions, so it gets a plain full-cube hitbox instead.</p>
 */
public class InjectionChamberBlock extends VatBlock {

	public InjectionChamberBlock(Properties properties) {
		super(properties, CKRecipeTypes.INJECTING);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	@Override
	public BlockEntityType<? extends VatBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.INJECTION_CHAMBER.get();
	}
}
