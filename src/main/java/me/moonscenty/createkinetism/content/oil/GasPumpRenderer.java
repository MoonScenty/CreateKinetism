package me.moonscenty.createkinetism.content.oil;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/** The cog on the pump's side, turning with the network. Nothing else here moves. */
public class GasPumpRenderer extends KineticBlockEntityRenderer<GasPumpBlockEntity> {

	public GasPumpRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected SuperByteBuffer getRotatedModel(GasPumpBlockEntity be, BlockState state) {
		return CachedBuffers.partialFacing(CKPartialModels.GAS_PUMP_COG, state);
	}
}
