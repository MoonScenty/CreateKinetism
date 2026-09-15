package me.moonscenty.createkinetism.content.boiler;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Right-click a Thermal Boiler Tank stack that is at least three floors tall to fold it into a
 * boiler: the bottom floor becomes the feed tank, everything above it becomes the product tank - see
 * {@link ThermalBoilerTankBlockEntity#activateBoiler()}. Taking the boiler apart again is a Wrench's
 * job, not this item's - see {@link ThermalBoilerTankBlock#onWrenched}.
 *
 * <p>Both click, like a lever: the controller latching on, a lower note for it coming off.</p>
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

		if (!level.isClientSide) {
			controller.activateBoiler();
			click(level, context.getClickedPos(), ON_PITCH);
		}
		return InteractionResult.SUCCESS;
	}

	/** A lever's two notes: switching on, and the lower one for switching off. */
	static final float ON_PITCH = 0.6f, OFF_PITCH = 0.5f;

	/** A lever's click, a little louder than a lever's - played from the server so everyone nearby hears. */
	static void click(Level level, BlockPos pos, float pitch) {
		level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6f, pitch);
	}
}
