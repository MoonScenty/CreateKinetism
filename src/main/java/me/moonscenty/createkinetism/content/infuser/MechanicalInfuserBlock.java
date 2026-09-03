package me.moonscenty.createkinetism.content.infuser;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.ComparatorUtil;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mekanism's Metallurgic Infuser, built on Create's Spout.
 *
 * <p>A spout drips a fluid onto whatever sits below it, which is exactly the shape of an infuser:
 * the infusion goes into the item rather than into a slot. The one thing Create's spout does not do
 * is cost anything to run - it is passive - so this one is kinetic, and stops when the shaft
 * does.</p>
 *
 * <p>Drive comes in from above, on the Y axis. The nozzle needs the space underneath and fluid
 * arrives through the sides, which leaves the top as the only face free for a shaft.</p>
 */
public class MechanicalInfuserBlock extends KineticBlock implements IWrenchable, IBE<MechanicalInfuserBlockEntity> {

	public MechanicalInfuserBlock(Properties properties) {
		super(properties);
	}

	/**
	 * The vats' shape, not the spout's. Create's SPOUT is a chunky column, which stopped matching once
	 * this became a mixer-shaped body with a nozzle underneath - MECHANICAL_PROCESSOR_SHAPE is a full
	 * cube with the middle bored out, which is exactly that. The 14px box for players is Create's own
	 * trick to stop you catching on the frame while walking past.
	 */
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (context instanceof EntityCollisionContext entityContext
			&& entityContext.getEntity() instanceof Player)
			return AllShapes.CASING_14PX.get(Direction.DOWN);
		return AllShapes.MECHANICAL_PROCESSOR_SHAPE;
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return Axis.Y;
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face == Direction.UP;
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return ComparatorUtil.levelOfSmartFluidTank(level, pos);
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}

	@Override
	public Class<MechanicalInfuserBlockEntity> getBlockEntityClass() {
		return MechanicalInfuserBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends MechanicalInfuserBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.MECHANICAL_INFUSER.get();
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, level, pos, newState);
	}
}
