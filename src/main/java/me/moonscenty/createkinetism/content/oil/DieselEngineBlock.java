package me.moonscenty.createkinetism.content.oil;

import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKShapes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>The heavy engine, and the one machine here that is <em>not</em> a rotation source in its own
 * right. Like Create's Steam Engine it bolts onto a face and converts the shaft <em>two</em> blocks
 * out into a Powered Shaft, which is the thing that actually reports to the network.</p>
 *
 * <p>A Powered Shaft records a single engine position, so it takes exactly one engine - engines are
 * ganged by giving each its own shaft and tying those shafts together, and the capacities add up on
 * the network rather than on one shaft.</p>
 *
 * <p>Its stress capacity is read off this block through {@code BlockStressValues}, because the
 * powered shaft is what actually reports capacity to the network.</p>
 */
public class DieselEngineBlock extends SteamEngineBlock {

	public DieselEngineBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		BlockPos shaftPos = getShaftPos(state, pos);
		BlockState shaftState = level.getBlockState(shaftPos);
		if (isShaftValid(state, shaftState))
			level.setBlock(shaftPos, PoweredShaftBlock.getEquivalent(shaftState), Block.UPDATE_ALL);
	}

	/** Unlike a steam engine it needs no boiler under it, so it can go anywhere. */
	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return true;
	}

	/**
	 * Deliberately does <em>not</em> take a bucket, unlike the gasoline engine - {@code useItemOn} is
	 * how Create's shaft placement helper puts a Powered Shaft at the right offset when you
	 * right-click the engine holding a shaft, so overriding it here costs you the only convenient way
	 * to hook the engine up. Feed this one by pipe.
	 */
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		AttachFace face = state.getValue(FACE);
		Direction direction = state.getValue(FACING);
		return face == AttachFace.CEILING ? CKShapes.MEDIUM_ENGINE_CEILING.get(direction.getAxis())
			: face == AttachFace.FLOOR ? CKShapes.MEDIUM_ENGINE.get(direction.getAxis())
				: CKShapes.MEDIUM_ENGINE_WALL.get(direction);
	}

	/** Any redstone signal shuts the engine down outright - no throttling, unlike the gasoline one. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
		boolean isMoving) {
		if (level.isClientSide)
			return;
		boolean hasSignal = level.hasNeighborSignal(pos);
		withBlockEntityDo(level, pos, be -> {
			if (be instanceof DieselEngineBlockEntity engine) {
				engine.redstoneDisabled = hasSignal;
				engine.updateRotation();
			}
		});
	}

	@Override
	public BlockEntityType<? extends SteamEngineBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.DIESEL_ENGINE.get();
	}
}
