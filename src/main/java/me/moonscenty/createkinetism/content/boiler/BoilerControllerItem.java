package me.moonscenty.createkinetism.content.boiler;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Right-click a Thermal Boiler Tank stack that is at least three floors tall to fold it into a
 * boiler: the bottom floor becomes the feed tank, everything above it becomes the product tank.
 * Sneak and right-click a boiler to fold it back into a plain stack - see
 * {@link ThermalBoilerTankBlockEntity#activateBoiler()} / {@code deactivateBoiler()}.
 */
public class BoilerControllerItem extends Item {

	public BoilerControllerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if (!(level.getBlockEntity(context.getClickedPos()) instanceof ThermalBoilerTankBlockEntity be))
			return InteractionResult.PASS;

		ThermalBoilerTankBlockEntity controller = be.getControllerBE();
		if (controller == null)
			return InteractionResult.PASS;

		Player player = context.getPlayer();
		boolean sneaking = player != null && player.isShiftKeyDown();

		if (sneaking) {
			if (!controller.boilerMode)
				return InteractionResult.PASS;
			if (!level.isClientSide)
				controller.deactivateBoiler();
			return InteractionResult.SUCCESS;
		}

		if (controller.boilerMode)
			return InteractionResult.PASS;

		if (!controller.canBecomeBoiler()) {
			if (level.isClientSide && player != null)
				player.displayClientMessage(Component
					.translatable("createkinetism.tooltip.boiler_controller.too_short",
						ThermalBoilerTankBlockEntity.MIN_BOILER_HEIGHT)
					.withStyle(ChatFormatting.RED), true);
			return InteractionResult.FAIL;
		}

		if (!level.isClientSide)
			controller.activateBoiler();
		return InteractionResult.SUCCESS;
	}
}
