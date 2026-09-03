package me.moonscenty.createkinetism.content.injection;

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
 * Mekanism's Chemical Injection Chamber, built the way the Mechanical Infuser is: a spout that has
 * to be turned rather than a basin machine.
 *
 * <p>Chemical Injection is item plus gas to item - no bulk material, no second slot - which is
 * exactly the Infuser's shape and not a vat's. Renamed from {@code injection_vat} once that stopped
 * being true.</p>
 *
 * <p>Drive comes in from above, on the Y axis, matching the millstone-style cog visible through the
 * housing.</p>
 */
public class InjectionChamberBlock extends KineticBlock implements IWrenchable, IBE<InjectionChamberBlockEntity> {

	public InjectionChamberBlock(Properties properties) {
		super(properties);
	}

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
	public Class<InjectionChamberBlockEntity> getBlockEntityClass() {
		return InjectionChamberBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends InjectionChamberBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.INJECTION_CHAMBER.get();
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, level, pos, newState);
	}
}
