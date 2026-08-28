package me.moonscenty.createkinetism.content.oil;

import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKShapes;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Burns off whatever you pipe into its base. Refining always leaves you with a cut nobody wants,
 * and the alternative to a flare is a backed-up column.</p>
 */
public class FlarestackBlock extends Block implements IBE<FlarestackBlockEntity> {

	public FlarestackBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Class<FlarestackBlockEntity> getBlockEntityClass() {
		return FlarestackBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends FlarestackBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.FLARESTACK.get();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return CKShapes.FLARESTACK.get(Direction.UP);
	}
}
