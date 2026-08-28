package me.moonscenty.createkinetism.registry;

import static net.minecraft.core.Direction.DOWN;
import static net.minecraft.core.Direction.NORTH;
import static net.minecraft.core.Direction.SOUTH;
import static net.minecraft.core.Direction.UP;

import java.util.function.BiFunction;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Hitboxes for the blocks whose models do not fill a cube. Without these a block is selected and
 * collided with as a full cube, which reads wrong on anything slim and leaves the parts that stick
 * out of the block entirely unclickable.</p>
 *
 * <p>The builder mirrors Create's own {@code AllShapes}, so a shape is written once facing one way
 * and the {@link VoxelShaper} rotates it for the rest.</p>
 */
public class CKShapes {

	public static final VoxelShaper

		PUMPJACK_CRANK = shape(2, 0, 2, 14, 16, 14).forDirectional(NORTH),

		PUMPJACK_WELL = shape(2, 0, 0, 14, 16, 14).forDirectional(NORTH),

		PUMPJACK_PIVOT = shape(1, 0, 1, 15, 14, 15).forDirectional(NORTH),

		DIPPER = shape(0, -16, 0, 16, 16, 16).forDirectional(DOWN),

		DISTILLATION_OUTPUT = shape(3, 3, 3, 13, 13, 16).forDirectional(SOUTH),

		FLARESTACK = shape(1, 0, 1, 15, 10, 15)
			.add(2, 10, 2, 14, 20, 14)
			.forDirectional(),

		SMALL_ENGINE = shape(1, 0, 0, 15, 3, 16)
			.add(3, 3, 1, 13, 13, 15)
			.forDirectional(NORTH),

		// Thick base plus the main body, so the engine reads as bolted down.
		MEDIUM_ENGINE = shape(1, 0, 1, 15, 3, 15)
			.add(2, 0, 2, 14, 15, 14)
			.forHorizontalAxis(),

		MEDIUM_ENGINE_CEILING = shape(1, 13, 1, 15, 16, 15)
			.add(2, 1, 2, 14, 16, 14)
			.forHorizontalAxis(),

		MEDIUM_ENGINE_WALL = shape(1, 1, 0, 15, 15, 3)
			.add(2, 2, 0, 14, 14, 15)
			.forHorizontal(SOUTH);

	private static Builder shape(VoxelShape shape) {
		return new Builder(shape);
	}

	private static Builder shape(double x1, double y1, double z1, double x2, double y2, double z2) {
		return shape(cuboid(x1, y1, z1, x2, y2, z2));
	}

	private static VoxelShape cuboid(double x1, double y1, double z1, double x2, double y2, double z2) {
		return Block.box(x1, y1, z1, x2, y2, z2);
	}

	public static class Builder {

		private VoxelShape shape;

		public Builder(VoxelShape shape) {
			this.shape = shape;
		}

		public Builder add(VoxelShape shape) {
			this.shape = Shapes.or(this.shape, shape);
			return this;
		}

		public Builder add(double x1, double y1, double z1, double x2, double y2, double z2) {
			return add(cuboid(x1, y1, z1, x2, y2, z2));
		}

		public Builder erase(double x1, double y1, double z1, double x2, double y2, double z2) {
			this.shape = Shapes.join(shape, cuboid(x1, y1, z1, x2, y2, z2), BooleanOp.ONLY_FIRST);
			return this;
		}

		public VoxelShape build() {
			return shape;
		}

		public VoxelShaper build(BiFunction<VoxelShape, Direction, VoxelShaper> factory, Direction direction) {
			return factory.apply(shape, direction);
		}

		public VoxelShaper build(BiFunction<VoxelShape, Direction.Axis, VoxelShaper> factory, Direction.Axis axis) {
			return factory.apply(shape, axis);
		}

		public VoxelShaper forDirectional(Direction direction) {
			return build(VoxelShaper::forDirectional, direction);
		}

		public VoxelShaper forAxis() {
			return build(VoxelShaper::forAxis, Direction.Axis.Y);
		}

		public VoxelShaper forHorizontalAxis() {
			return build(VoxelShaper::forHorizontalAxis, Direction.Axis.Z);
		}

		public VoxelShaper forHorizontal(Direction direction) {
			return build(VoxelShaper::forHorizontal, direction);
		}

		public VoxelShaper forDirectional() {
			return forDirectional(UP);
		}
	}
}
