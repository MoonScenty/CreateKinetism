package me.moonscenty.createkinetism.content.washer;

import me.moonscenty.createkinetism.content.machine.ProcessingMachineBlockEntity;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mekanism: Chemical Washer. Dirty slurry plus a great deal of water.
 *
 * <p>Unlike the vats this holds its own fluid rather than working out of a basin - it is a sealed
 * vessel with an auger down the middle, and the shaft comes in underneath where a basin would
 * otherwise sit. Everything but the auger's angle comes from
 * {@link ProcessingMachineBlockEntity}.</p>
 */
public class MechanicalWasherBlockEntity extends ProcessingMachineBlockEntity {

	public MechanicalWasherBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected CKRecipeTypes getRecipeType() {
		return CKRecipeTypes.WASHING;
	}

	/**
	 * The auger's angle. Driven straight off the shaft rather than off a work timer, so a washer with
	 * nothing in it still turns - the shaft is connected either way, and a stopped auger on a live
	 * network would read as a broken machine.
	 */
	public float getPropellerAngle(float partialTicks) {
		return (AnimationTickHolder.getRenderTime(level) + partialTicks) * getSpeed() * 3 / 10f % 360;
	}
}
