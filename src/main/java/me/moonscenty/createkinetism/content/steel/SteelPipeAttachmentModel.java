package me.moonscenty.createkinetism.content.steel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.FluidTransportBehaviour.AttachmentTypes;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.model.BakedModelWrapperWithData;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.data.Iterate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Steel pipes grow rims, drains and connectors where they meet other blocks, and none of that can
 * live in the blockstate - which face gets which component depends on the neighbours. So the baked
 * model is wrapped: Create's {@code FluidTransportBehaviour} works out the attachment type per face,
 * and this pastes the matching partial model's quads on at bake time.</p>
 *
 * <p>Brackets and pipe casing ride along the same way.</p>
 */
public class SteelPipeAttachmentModel extends BakedModelWrapperWithData {

	private static final ModelProperty<PipeModelData> PIPE_PROPERTY = new ModelProperty<>();

	private final boolean ao;

	public SteelPipeAttachmentModel(BakedModel originalModel, boolean ao) {
		super(originalModel);
		this.ao = ao;
	}

	public static SteelPipeAttachmentModel withAO(BakedModel template) {
		return new SteelPipeAttachmentModel(template, true);
	}

	public static SteelPipeAttachmentModel withoutAO(BakedModel template) {
		return new SteelPipeAttachmentModel(template, false);
	}

	@Override
	protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world, BlockPos pos,
		BlockState state, ModelData blockEntityData) {
		PipeModelData data = new PipeModelData();

		FluidTransportBehaviour transport = BlockEntityBehaviour.get(world, pos, FluidTransportBehaviour.TYPE);
		BracketedBlockEntityBehaviour bracket = BlockEntityBehaviour.get(world, pos, BracketedBlockEntityBehaviour.TYPE);

		if (transport != null)
			for (Direction d : Iterate.directions)
				data.putAttachment(d, transport.getRenderedRimAttachment(world, pos, state, d));
		if (bracket != null)
			data.putBracket(bracket.getBracket());
		data.setEncased(FluidPipeBlock.shouldDrawCasing(world, pos, state));

		return builder.with(PIPE_PROPERTY, data);
	}

	@Override
	public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand,
		@NotNull ModelData data) {
		List<ChunkRenderTypeSet> set = new ArrayList<>();
		set.add(super.getRenderTypes(state, rand, data));
		set.add(CKPartialModels.STEEL_PIPE_CASING.get()
			.getRenderTypes(state, rand, data));

		if (data.has(PIPE_PROPERTY)) {
			PipeModelData pipeData = data.get(PIPE_PROPERTY);
			for (Direction d : Iterate.directions)
				for (AttachmentTypes.ComponentPartials partial : pipeData.getAttachment(d).partials)
					set.add(CKPartialModels.STEEL_PIPE_ATTACHMENTS.get(partial)
						.get(d)
						.get()
						.getRenderTypes(state, rand, data));
		}

		return ChunkRenderTypeSet.union(set);
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data,
		RenderType renderType) {
		List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
		if (!data.has(PIPE_PROPERTY))
			return quads;

		quads = new ArrayList<>(quads);
		addQuads(quads, state, side, rand, data, data.get(PIPE_PROPERTY), renderType);
		return quads;
	}

	@Override
	public boolean useAmbientOcclusion() {
		return ao;
	}

	private void addQuads(List<BakedQuad> quads, BlockState state, Direction side, RandomSource rand, ModelData data,
		PipeModelData pipeData, RenderType renderType) {

		BakedModel bracket = pipeData.getBracket();
		if (bracket != null)
			quads.addAll(bracket.getQuads(state, side, rand, data, renderType));

		for (Direction d : Iterate.directions)
			for (AttachmentTypes.ComponentPartials partial : pipeData.getAttachment(d).partials)
				quads.addAll(CKPartialModels.STEEL_PIPE_ATTACHMENTS.get(partial)
					.get(d)
					.get()
					.getQuads(state, side, rand, data, renderType));

		if (pipeData.isEncased())
			quads.addAll(CKPartialModels.STEEL_PIPE_CASING.get()
				.getQuads(state, side, rand, data, renderType));
	}

	private static class PipeModelData {

		private final AttachmentTypes[] attachments;
		private boolean encased;
		private BakedModel bracket;

		PipeModelData() {
			attachments = new AttachmentTypes[6];
			Arrays.fill(attachments, AttachmentTypes.NONE);
		}

		void putBracket(BlockState state) {
			if (state == null)
				return;
			bracket = Minecraft.getInstance()
				.getBlockRenderer()
				.getBlockModel(state);
		}

		BakedModel getBracket() {
			return bracket;
		}

		void putAttachment(Direction face, AttachmentTypes rim) {
			attachments[face.get3DDataValue()] = rim;
		}

		AttachmentTypes getAttachment(Direction face) {
			return attachments[face.get3DDataValue()];
		}

		void setEncased(boolean encased) {
			this.encased = encased;
		}

		boolean isEncased() {
			return encased;
		}
	}
}
