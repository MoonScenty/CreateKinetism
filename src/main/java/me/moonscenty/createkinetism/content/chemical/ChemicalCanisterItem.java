package me.moonscenty.createkinetism.content.chemical;

import java.util.List;

import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.utility.CreateLang;

import me.moonscenty.createkinetism.foundation.CKLang;
import me.moonscenty.createkinetism.registry.CKDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;

/**
 * What a bucket cannot carry.
 *
 * <p>Every chemical in this mod is a {@code VirtualFluid}, and {@code VirtualFluid.getBucket()}
 * returns {@code Items.AIR} outright - not a choice we made, and not one we can override. That is
 * usually fine, because these gases live in pipes. It stops being fine at Create's filters: a filter
 * names a fluid by holding an item that contains it, so with no bucket there is no way to say
 * "chlorine" to a Smart Fluid Pipe at all.</p>
 *
 * <p>This is that item. It takes exactly the fluids a bucket cannot - {@link #isGas} is the whole
 * rule - so the two never overlap and neither needs to know about the other. Right-clicking a tank,
 * a pipe or a basin fills it from what is there, and right-clicking with a full one empties it back;
 * no Spout in the loop, because pouring a gas out of a spout was never a sensible picture.</p>
 */
public class ChemicalCanisterItem extends Item {

	public static final int CAPACITY = 1000;

	public ChemicalCanisterItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	/**
	 * The rule that divides this item from a bucket: a fluid belongs in a canister exactly when no
	 * bucket for it exists. Water and the pourable oils have one, so they are refused here.
	 */
	public static boolean isGas(FluidStack stack) {
		return !stack.isEmpty() && stack.getFluid()
			.getBucket() == Items.AIR;
	}

	public static FluidStack getContents(ItemStack stack) {
		return stack.getOrDefault(CKDataComponents.CANISTER_FLUID.get(), SimpleFluidContent.EMPTY)
			.copy();
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event, Item item) {
		event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new Handler(stack), item);
	}

	/** Fill from whatever was clicked, or pour back into it. */
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, context.getClickedFace());
		if (target == null)
			return InteractionResult.PASS;

		ItemStack held = context.getItemInHand();
		FluidStack held_ = getContents(held);
		boolean client = level.isClientSide;

		if (!held_.isEmpty()) {
			int poured = target.fill(held_, client ? FluidAction.SIMULATE : FluidAction.EXECUTE);
			if (poured <= 0)
				return InteractionResult.PASS;
			if (!client)
				setContents(held, held_.getAmount() - poured <= 0 ? FluidStack.EMPTY
					: FluidHelper.copyStackWithAmount(held_, held_.getAmount() - poured));
			return InteractionResult.sidedSuccess(client);
		}

		// Nothing held: take the first thing in there that a bucket could not have taken.
		for (int tank = 0; tank < target.getTanks(); tank++) {
			FluidStack available = target.getFluidInTank(tank);
			if (!isGas(available))
				continue;
			FluidStack drawn = target.drain(FluidHelper.copyStackWithAmount(available, CAPACITY),
				client ? FluidAction.SIMULATE : FluidAction.EXECUTE);
			if (drawn.isEmpty())
				continue;
			if (!client)
				setContents(held, drawn);
			return InteractionResult.sidedSuccess(client);
		}
		return InteractionResult.PASS;
	}

	private static void setContents(ItemStack stack, FluidStack fluid) {
		if (fluid.isEmpty())
			stack.remove(CKDataComponents.CANISTER_FLUID.get());
		else
			stack.set(CKDataComponents.CANISTER_FLUID.get(), SimpleFluidContent.copyOf(fluid));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		FluidStack held = getContents(stack);
		if (held.isEmpty()) {
			CKLang.translate("tooltip.canister.empty")
				.style(ChatFormatting.GRAY)
				.addTo(tooltip);
			return;
		}
		tooltip.add(Component.translatable(held.getFluid()
			.getFluidType()
			.getDescriptionId())
			.withStyle(ChatFormatting.GRAY));
		CreateLang.number(held.getAmount())
			.text("mB")
			.style(ChatFormatting.DARK_GRAY)
			.addTo(tooltip);
	}

	/** {@link FluidHandlerItemStack} with the gas-only rule bolted on. */
	private static class Handler extends FluidHandlerItemStack {

		Handler(ItemStack container) {
			super(CKDataComponents.CANISTER_FLUID, container, CAPACITY);
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return isGas(stack);
		}
	}

	/** Cast helper for the capability, kept so the class above stays a nested private. */
	public static IFluidHandlerItem handlerFor(ItemStack stack) {
		return new Handler(stack);
	}
}
