package me.moonscenty.createkinetism.content.turbine;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItem;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lets a Turbine grow a floor at a time, the way Create's Fluid Tank and this mod's Steel Tank do:
 * clicking the top or bottom face of a turbine wider than one block places the whole width x width floor
 * at once. Sneak to place a single casing.
 *
 * <p>The floor goes down only if every cell of it is free - a Turbine Bearing under the middle of the
 * floor, for one, stops a floor being added underneath.</p>
 */
public class TurbineCasingItem extends BlockItem {

	/** The flag {@link TurbineCasingBlock#getSoundType} checks, so a whole floor does not clang nine times. */
	public static final String SILENCE_SOUND = "SilenceTankSound";

	public TurbineCasingItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult place(BlockPlaceContext ctx) {
		InteractionResult initialResult = super.place(ctx);
		if (!initialResult.consumesAction())
			return initialResult;
		tryMultiPlace(ctx);
		return initialResult;
	}

	/** The structure's bookkeeping belongs to the structure, not to a casing in someone's hand. */
	@Override
	protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, Player player, ItemStack itemStack,
		BlockState state) {
		if (level.getServer() == null)
			return false;
		CustomData blockEntityData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (blockEntityData != null) {
			CompoundTag nbt = blockEntityData.copyTag();
			for (String key : new String[] { "Size", "Height", "Controller", "LastKnownPos", "Water", "Steam", "Flow" })
				nbt.remove(key);
			BlockEntity.addEntityType(nbt, ((IBE<?>) getBlock()).getBlockEntityType());
			itemStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(nbt));
		}
		return super.updateCustomBlockEntityTag(pos, level, player, itemStack, state);
	}

	private void tryMultiPlace(BlockPlaceContext ctx) {
		Player player = ctx.getPlayer();
		if (player == null || player.isShiftKeyDown())
			return;

		Direction face = ctx.getClickedFace();
		if (!face.getAxis()
			.isVertical())
			return;

		ItemStack stack = ctx.getItemInHand();
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		BlockPos placedOnPos = pos.relative(face.getOpposite());
		if (!TurbineCasingBlock.isCasing(level.getBlockState(placedOnPos)))
			return;
		if (SymmetryWandItem.presentInHotbar(player))
			return;

		TurbineCasingBlockEntity casing =
			ConnectivityHandler.partAt(CKBlockEntityTypes.TURBINE_CASING.get(), level, placedOnPos);
		if (casing == null)
			return;
		TurbineCasingBlockEntity controller = casing.getControllerBE();
		if (controller == null)
			return;

		int width = controller.getWidth();
		if (width == 1)
			return;

		// Floors only ever go on the very top or the very bottom of the turbine.
		BlockPos startPos = face == Direction.DOWN ? controller.getBlockPos()
			.below()
			: controller.getBlockPos()
				.above(controller.getHeight());
		if (startPos.getY() != pos.getY())
			return;

		int toPlace = 0;
		for (int xOffset = 0; xOffset < width; xOffset++)
			for (int zOffset = 0; zOffset < width; zOffset++) {
				BlockState blockState = level.getBlockState(startPos.offset(xOffset, 0, zOffset));
				if (TurbineCasingBlock.isCasing(blockState))
					continue;
				// Anything in the way would leave the floor ragged, so place nothing.
				if (!blockState.canBeReplaced())
					return;
				toPlace++;
			}

		if (!player.isCreative() && stack.getCount() < toPlace)
			return;

		for (int xOffset = 0; xOffset < width; xOffset++)
			for (int zOffset = 0; zOffset < width; zOffset++) {
				BlockPos offsetPos = startPos.offset(xOffset, 0, zOffset);
				if (TurbineCasingBlock.isCasing(level.getBlockState(offsetPos)))
					continue;
				player.getPersistentData()
					.putBoolean(SILENCE_SOUND, true);
				super.place(BlockPlaceContext.at(ctx, offsetPos, face));
				player.getPersistentData()
					.remove(SILENCE_SOUND);
			}
	}
}
