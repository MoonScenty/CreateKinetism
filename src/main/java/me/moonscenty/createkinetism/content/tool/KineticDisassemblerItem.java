package me.moonscenty.createkinetism.content.tool;

import java.util.List;

import me.moonscenty.createkinetism.registry.CKDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mekanism: Atomic Disassembler, wound up instead of charged.
 *
 * <p>Mekanism's tool runs on Forge Energy, which this mod does not have and does not want. What it
 * does have is the Kinetic Accumulator, which already stores a real quantity of rotation - so the
 * Disassembler is a clockwork tool: set it on an accumulator to wind it, and every block it breaks
 * spends some of that winding back.</p>
 *
 * <p>The signature behaviour is kept: one tool for stone, wood and earth, and a flat mining speed
 * that ignores hardness entirely. Obsidian and dirt come apart at the same rate, because what is
 * doing the work is the winding rather than the edge. Run it dry and it is a bare hand again - no
 * speed, and no harvesting the blocks that need a real tool.</p>
 */
public class KineticDisassemblerItem extends Item {

	/** A tenth of a full Kinetic Accumulator. */
	public static final int CAPACITY = 64_000;

	public KineticDisassemblerItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	// ------------------------------------------------------------------ winding

	public static int getCharge(ItemStack stack) {
		return stack.getOrDefault(CKDataComponents.CHARGE.get(), 0);
	}

	public static void setCharge(ItemStack stack, int charge) {
		stack.set(CKDataComponents.CHARGE.get(), Math.max(0, Math.min(CAPACITY, charge)));
	}

	public static DisassemblerMode getMode(ItemStack stack) {
		return stack.getOrDefault(CKDataComponents.MODE.get(), DisassemblerMode.NORMAL);
	}

	/**
	 * The mode the tool can actually deliver: an empty one falls back to bare hands. Package-visible
	 * so {@link KineticDisassemblerItemRenderer} can gate the cog and blades on the same condition
	 * mining already uses, rather than a separate notion of "powered."
	 */
	static DisassemblerMode effectiveMode(ItemStack stack) {
		DisassemblerMode mode = getMode(stack);
		return getCharge(stack) >= mode.cost() ? mode : DisassemblerMode.OFF;
	}

	// ------------------------------------------------------------------ mining

	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		return effectiveMode(stack).speed();
	}

	@Override
	public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
		if (effectiveMode(stack) == DisassemblerMode.OFF)
			return false;
		return state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_AXE)
			|| state.is(BlockTags.MINEABLE_WITH_SHOVEL);
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos,
		LivingEntity miner) {
		// Instant-breaking blocks like tall grass cost nothing, the same rule a vanilla tool uses for
		// its durability.
		if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0)
			setCharge(stack, getCharge(stack) - effectiveMode(stack).cost());
		return true;
	}

	// ------------------------------------------------------------------ the dial

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!player.isShiftKeyDown())
			return InteractionResultHolder.pass(stack);

		DisassemblerMode mode = getMode(stack).next();
		stack.set(CKDataComponents.MODE.get(), mode);

		if (level.isClientSide) {
			player.displayClientMessage(Component.translatable("createkinetism.tooltip.disassembler.switched",
				Component.translatable(mode.translationKey())
					.withStyle(ChatFormatting.WHITE))
				.withStyle(ChatFormatting.GRAY), true);
		} else {
			level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(),
				SoundSource.PLAYERS, 0.4f, mode == DisassemblerMode.OFF ? 0.6f : 0.8f + 0.2f * mode.ordinal());
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	// ------------------------------------------------------------------ readout

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return getCharge(stack) < CAPACITY;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13f * getCharge(stack) / CAPACITY);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0x4C9AD8;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
		TooltipFlag flag) {
		DisassemblerMode mode = getMode(stack);
		tooltip.add(Component.translatable("createkinetism.tooltip.disassembler.mode",
			Component.translatable(mode.translationKey())
				.withStyle(ChatFormatting.WHITE))
			.withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("createkinetism.tooltip.disassembler.charge",
			Component.literal(getCharge(stack) + " / " + CAPACITY)
				.withStyle(ChatFormatting.WHITE))
			.withStyle(ChatFormatting.GRAY));
		if (mode != DisassemblerMode.OFF && getCharge(stack) < mode.cost())
			tooltip.add(Component.translatable("createkinetism.tooltip.disassembler.empty")
				.withStyle(ChatFormatting.RED));
	}
}
