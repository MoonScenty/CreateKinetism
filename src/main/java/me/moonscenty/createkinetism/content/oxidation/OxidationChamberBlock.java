package me.moonscenty.createkinetism.content.oxidation;

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
 * Mekanism's Chemical Oxidizer: a solid into a gas.
 *
 * <p>The Injection Chamber's housing and plunging head, because the two machines do the same
 * gesture - press down on what is in the basin and let a gas come off it. Placement and drive are
 * the mixer's, like every other vat, and the shape is a plain full cube for the same reason the
 * Injection Chamber's is: this housing is its own model, not Create's bored-out mixer body.</p>
 *
 * <p>Where it differs from the Injection Chamber is that it needs no input of its own. That machine
 * carries a tank because Mekanism's takes an item <em>and</em> a gas, and only the item half fits in
 * a basin; oxidising takes one side and puts out the other, both of which the basin already holds.
 * So this is an ordinary {@link me.moonscenty.createkinetism.content.vat.VatBlockEntity} underneath -
 * see {@link OxidationChamberBlockEntity}.</p>
 */
public class OxidationChamberBlock extends CogVatBlock {

	public OxidationChamberBlock(Properties properties) {
		super(properties, CKRecipeTypes.OXIDIZING);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	@Override
	public BlockEntityType<? extends VatBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.OXIDATION_CHAMBER.get();
	}
}
