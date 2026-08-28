package me.moonscenty.createkinetism.content.steel;

import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Identical to Create's pump renderer except for the cog it spins. Create's draws a brass one,
 * which looks wrong bolted to a steel body.</p>
 */
public class SteelPumpRenderer extends KineticBlockEntityRenderer<PumpBlockEntity> {

	public SteelPumpRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected SuperByteBuffer getRotatedModel(PumpBlockEntity be, BlockState state) {
		return CachedBuffers.partialFacing(CKPartialModels.STEEL_PUMP_COG, state);
	}
}
