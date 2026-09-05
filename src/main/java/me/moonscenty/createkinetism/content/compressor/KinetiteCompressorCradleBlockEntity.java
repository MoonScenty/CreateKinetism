package me.moonscenty.createkinetism.content.compressor;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;

import org.jetbrains.annotations.Nullable;

/**
 * A place for a shaft to land, and the door the Kinetite goes in.
 *
 * <p>The cradle keeps no inventory of its own. What it does is hand out the master's Kinetite holder
 * as its own item capability, so that feeding the machine reads the way it looks: the target goes in
 * at the front, under the spinning head, and the Kinetite goes in at the back, behind the ram that
 * drives it. Two hoppers, no filters.</p>
 */
public class KinetiteCompressorCradleBlockEntity extends KineticBlockEntity {

	public KinetiteCompressorCradleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<KinetiteCompressorCradleBlockEntity> type) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> be.kinetiteHandler());
	}

	/** Null while the master is missing - there is nowhere to put anything until it is back. */
	@Nullable
	private IItemHandler kinetiteHandler() {
		if (level == null)
			return null;
		BlockPos masterPos = KinetiteCompressorCradleBlock.masterPos(worldPosition, getBlockState());
		return level.getBlockEntity(masterPos) instanceof KinetiteCompressorBlockEntity master
			? master.kinetiteHandler()
			: null;
	}
}
