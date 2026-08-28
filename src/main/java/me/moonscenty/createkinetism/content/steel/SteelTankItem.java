package me.moonscenty.createkinetism.content.steel;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItem;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.block.IBE;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
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

import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Lets a stack of tanks grow a layer at a time. Clicking the top or bottom face of an existing
 * multiblock places the whole width x width layer in one go instead of making you lay nine blocks by
 * hand, which is what Create's own fluid tank does. Sneak to place a single tank.</p>
 */
public class SteelTankItem extends BlockItem {

	public SteelTankItem(Block block, Properties properties) {
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

	@Override
	protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, Player player, ItemStack itemStack,
		BlockState state) {
		MinecraftServer server = level.getServer();
		if (server == null)
			return false;

		CustomData blockEntityData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
		if (blockEntityData != null) {
			CompoundTag nbt = blockEntityData.copyTag();
			// Multiblock bookkeeping belongs to the structure, not to the item in your hand.
			nbt.remove("Luminosity");
			nbt.remove("Size");
			nbt.remove("Height");
			nbt.remove("Controller");
			nbt.remove("LastKnownPos");

			if (nbt.contains("TankContent")) {
				FluidStack fluid = FluidStack.parseOptional(server.registryAccess(), nbt.getCompound("TankContent"));
				if (!fluid.isEmpty()) {
					// A picked-up tank must not carry more than one block's worth of fluid.
					fluid.setAmount(Math.min(FluidTankBlockEntity.getCapacityMultiplier(), fluid.getAmount()));
					nbt.put("TankContent", fluid.saveOptional(server.registryAccess()));
				}
			}

			BlockEntity.addEntityType(nbt, ((IBE<?>) this.getBlock()).getBlockEntityType());
			itemStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(nbt));
		}

		return super.updateCustomBlockEntityTag(pos, level, player, itemStack, state);
	}

	private void tryMultiPlace(BlockPlaceContext ctx) {
		Player player = ctx.getPlayer();
		if (player == null)
			return;
		if (player.isShiftKeyDown())
			return;

		Direction face = ctx.getClickedFace();
		if (!face.getAxis()
			.isVertical())
			return;

		ItemStack stack = ctx.getItemInHand();
		Level world = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		BlockPos placedOnPos = pos.relative(face.getOpposite());
		BlockState placedOnState = world.getBlockState(placedOnPos);

		if (!SteelTankBlock.isTank(placedOnState))
			return;
		if (SymmetryWandItem.presentInHotbar(player))
			return;

		SteelTankBlockEntity tankAt =
			ConnectivityHandler.partAt(CKBlockEntityTypes.STEEL_TANK.get(), world, placedOnPos);
		if (tankAt == null)
			return;
		SteelTankBlockEntity controllerBE = tankAt.getControllerBE();
		if (controllerBE == null)
			return;

		int width = controllerBE.getWidth();
		if (width == 1)
			return;

		// Layers only ever go on the very top or the very bottom of the existing structure.
		BlockPos startPos = face == Direction.DOWN ? controllerBE.getBlockPos()
			.below()
			: controllerBE.getBlockPos()
				.above(controllerBE.getHeight());

		if (startPos.getY() != pos.getY())
			return;

		int tanksToPlace = 0;
		for (int xOffset = 0; xOffset < width; xOffset++) {
			for (int zOffset = 0; zOffset < width; zOffset++) {
				BlockPos offsetPos = startPos.offset(xOffset, 0, zOffset);
				BlockState blockState = world.getBlockState(offsetPos);
				if (SteelTankBlock.isTank(blockState))
					continue;
				// Anything solid in the way means the layer would come out ragged, so place nothing.
				if (!blockState.canBeReplaced())
					return;
				tanksToPlace++;
			}
		}

		if (!player.isCreative() && stack.getCount() < tanksToPlace)
			return;

		for (int xOffset = 0; xOffset < width; xOffset++) {
			for (int zOffset = 0; zOffset < width; zOffset++) {
				BlockPos offsetPos = startPos.offset(xOffset, 0, zOffset);
				BlockState blockState = world.getBlockState(offsetPos);
				if (SteelTankBlock.isTank(blockState))
					continue;
				BlockPlaceContext context = BlockPlaceContext.at(ctx, offsetPos, face);
				// Nine placement sounds at once is a racket; the block checks this flag.
				player.getPersistentData()
					.putBoolean("SilenceTankSound", true);
				super.place(context);
				player.getPersistentData()
					.remove("SilenceTankSound");
			}
		}
	}
}
