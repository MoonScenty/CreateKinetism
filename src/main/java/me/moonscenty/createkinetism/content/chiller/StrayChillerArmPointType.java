package me.moonscenty.createkinetism.content.chiller;

import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;

import me.moonscenty.createkinetism.registry.CKBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lets a Mechanical Arm feed a chiller.
 *
 * <p>Create decides whether a block is an arm target by asking its own registry entry -
 * {@code AllBlocks.BLAZE_BURNER.has(state)} - so a subclass is not enough on its own. The point it
 * creates is Create's own Blaze Burner point, which only deposits fuel and does so through the block
 * entity this chiller inherits.</p>
 */
public class StrayChillerArmPointType extends ArmInteractionPointType {

	@Override
	public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
		return CKBlocks.STRAY_CHILLER.has(state);
	}

	@Override
	public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
		return new AllArmInteractionPointTypes.BlazeBurnerPoint(this, level, pos, state);
	}
}
