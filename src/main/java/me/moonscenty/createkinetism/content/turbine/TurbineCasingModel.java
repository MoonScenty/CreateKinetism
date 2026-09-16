package me.moonscenty.createkinetism.content.turbine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankCTBehaviour;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;

import me.moonscenty.createkinetism.registry.CKSpriteShifts;

import net.createmod.catnip.data.Iterate;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * The Turbine Casing's connected textures.
 *
 * <p>The shell is the Steel Tank's, connected the way a Fluid Tank connects. The windows are the
 * turbine's own: a window block's sides are glass edge to edge, and the glass joins every window block
 * beside, above or below it in the same turbine into one pane - so a side of the turbine shows one big
 * framed window instead of a row of slits. See {@code TurbineCasingBlockEntity#setWindows} for which
 * blocks are windows.</p>
 *
 * <p>Faces between two connected casings are dropped, as the Steel Tank's model does, so the rotor can
 * be seen through the glass.</p>
 */
public class TurbineCasingModel extends CTModel {

	private static final ModelProperty<boolean[]> CULLED = new ModelProperty<>();

	public TurbineCasingModel(BakedModel originalModel) {
		super(originalModel, new Behaviour());
	}

	@Override
	protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world, BlockPos pos,
		BlockState state, ModelData blockEntityData) {
		super.gatherModelData(builder, world, pos, state, blockEntityData);
		boolean[] culled = new boolean[4];
		for (Direction d : Iterate.horizontalDirections)
			culled[d.get2DDataValue()] = ConnectivityHandler.isConnected(world, pos, pos.relative(d));
		return builder.with(CULLED, culled);
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData,
		RenderType renderType) {
		if (side != null)
			return Collections.emptyList();
		boolean[] culled = extraData.has(CULLED) ? extraData.get(CULLED) : new boolean[4];
		List<BakedQuad> quads = new ArrayList<>();
		for (Direction d : Iterate.directions) {
			if (d.getAxis()
				.isHorizontal() && culled[d.get2DDataValue()])
				continue;
			quads.addAll(super.getQuads(state, d, rand, extraData, renderType));
		}
		quads.addAll(super.getQuads(state, null, rand, extraData, renderType));
		return quads;
	}

	private static boolean isWindow(BlockState state) {
		return TurbineCasingBlock.isCasing(state) && state.getValue(TurbineCasingBlock.SHAPE) != FluidTankBlock.Shape.PLAIN;
	}

	/**
	 * Fluid Tank connections for the shell; for a window block, connections only to other window blocks
	 * of the same turbine, so the pane's frame lands where the glass stops.
	 */
	private static class Behaviour extends FluidTankCTBehaviour {

		Behaviour() {
			super(CKSpriteShifts.STEEL_TANK, CKSpriteShifts.STEEL_TANK_TOP, CKSpriteShifts.STEEL_TANK_INNER);
		}

		@Override
		public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
			if (sprite != null && CKSpriteShifts.TURBINE_CASING_WINDOW.getOriginal() == sprite)
				return CKSpriteShifts.TURBINE_CASING_WINDOW;
			return super.getShift(state, direction, sprite);
		}

		@Override
		public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos,
			BlockPos otherPos, Direction face) {
			if (!super.connectsTo(state, other, reader, pos, otherPos, face))
				return false;
			return !isWindow(state) || isWindow(other);
		}
	}
}
