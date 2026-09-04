package me.moonscenty.createkinetism.content.evaporation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankCTBehaviour;
import com.simibubi.create.foundation.block.connected.CTModel;

import net.createmod.catnip.data.Iterate;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * Same job as the Steel Tank's model wrapper ({@code SteelTankModel}), but pointed at Create's own
 * fluid tank textures instead of a local reskin - the models under evaporation_plant/ are Create's
 * own fluid_tank models with the lid taken off, so Create's own connected-texture shifts
 * ({@link AllSpriteShifts#FLUID_TANK}) already line up with them unchanged.
 *
 * <p>Applies the connected-texture shift across a stack so it reads as one vessel, and culls the
 * faces between neighbouring tanks so interior walls do not show through in a clustered stack.</p>
 */
public class EvaporationPlantModel extends CTModel {

	private static final ModelProperty<CullData> CULL_PROPERTY = new ModelProperty<>();

	public EvaporationPlantModel(BakedModel originalModel) {
		super(originalModel, new FluidTankCTBehaviour(AllSpriteShifts.FLUID_TANK, AllSpriteShifts.FLUID_TANK_TOP,
			AllSpriteShifts.FLUID_TANK_INNER));
	}

	public static EvaporationPlantModel standard(BakedModel template) {
		return new EvaporationPlantModel(template);
	}

	@Override
	protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world, BlockPos pos,
		BlockState state, ModelData blockEntityData) {
		super.gatherModelData(builder, world, pos, state, blockEntityData);

		CullData cullData = new CullData();
		for (Direction d : Iterate.horizontalDirections)
			cullData.setCulled(d, ConnectivityHandler.isConnected(world, pos, pos.relative(d)));
		return builder.with(CULL_PROPERTY, cullData);
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData,
		RenderType renderType) {
		// All faces are gathered in one pass so we can drop the ones facing a neighbouring tank.
		if (side != null)
			return Collections.emptyList();

		List<BakedQuad> quads = new ArrayList<>();
		for (Direction d : Iterate.directions) {
			if (extraData.has(CULL_PROPERTY) && extraData.get(CULL_PROPERTY)
				.isCulled(d))
				continue;
			quads.addAll(super.getQuads(state, d, rand, extraData, renderType));
		}
		quads.addAll(super.getQuads(state, null, rand, extraData, renderType));
		return quads;
	}

	private static class CullData {

		private final boolean[] culledFaces = new boolean[4];

		CullData() {
			Arrays.fill(culledFaces, false);
		}

		void setCulled(Direction face, boolean cull) {
			if (face.getAxis()
				.isVertical())
				return;
			culledFaces[face.get2DDataValue()] = cull;
		}

		boolean isCulled(Direction face) {
			if (face.getAxis()
				.isVertical())
				return false;
			return culledFaces[face.get2DDataValue()];
		}
	}
}
