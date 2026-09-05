package me.moonscenty.createkinetism.content.crystallization;

import me.moonscenty.createkinetism.content.vat.CogVatBlock;
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
 * Mekanism's Chemical Crystallizer: clean slurry into crystals.
 *
 * <p>The Injection Chamber's housing and plunging head, for the same reason the Oxidation Chamber
 * borrows them - the machine presses down on what the basin holds and something comes out. Placement
 * and drive are the mixer's, like every other vat, and the shape is a plain full cube because this
 * housing is its own model rather than Create's bored-out mixer body.</p>
 *
 * <p>Like the Oxidation Chamber and unlike the Injection Chamber, it holds nothing of its own: the
 * slurry going in and the crystal coming out both live in the basin, so there is no second tank to
 * check - see {@link CrystallizationChamberBlockEntity}.</p>
 */
public class CrystallizationChamberBlock extends CogVatBlock {

	public CrystallizationChamberBlock(Properties properties) {
		super(properties, CKRecipeTypes.CRYSTALLIZING);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	@Override
	public BlockEntityType<? extends VatBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.CRYSTALLIZATION_CHAMBER.get();
	}
}
