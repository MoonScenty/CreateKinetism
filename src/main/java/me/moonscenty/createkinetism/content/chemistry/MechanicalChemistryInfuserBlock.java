package me.moonscenty.createkinetism.content.chemistry;

import me.moonscenty.createkinetism.content.vat.CogVatBlock;
import me.moonscenty.createkinetism.content.vat.VatBlockEntity;
import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The Mechanical Chemistry Infuser, a vat that also holds a tank of its own.
 *
 * <p>Placement, shape and drive are the mixer's, exactly like the other vats. What differs is the
 * block entity: instead of stirring the basin with a whisk, it pours its own tank into the basin
 * below, so a nozzle model draws instead of the mixer's pole and head - see
 * {@link MechanicalChemistryInfuserBlockEntity} and {@link MechanicalChemistryInfuserRenderer}. The
 * basin still runs the recipe itself, exactly as any other vat's basin does; this block only ever
 * feeds it a second fluid.</p>
 */
public class MechanicalChemistryInfuserBlock extends CogVatBlock {

	public MechanicalChemistryInfuserBlock(Properties properties) {
		super(properties, CKRecipeTypes.CHEMICAL_INFUSING);
	}

	@Override
	public BlockEntityType<? extends VatBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.MECHANICAL_CHEMISTRY_INFUSER.get();
	}
}
