package me.moonscenty.createkinetism.content.curio;

import java.util.List;

import me.moonscenty.createkinetism.foundation.CKLang;
import me.moonscenty.createkinetism.foundation.KineticallyCharged;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * A wound spring worn on a ring slot: it keeps the rest of what you are wearing wound too.
 *
 * <p>Everything chargeable in this mod runs off a Kinetic Accumulator, which means taking the thing
 * off and setting it on a block. That is fine for a tool you use at a base and wrong for gear you
 * wear while away from one. The spring is the answer: wind the spring, and it winds the wings and
 * the radar while you fly.</p>
 *
 * <p>It gives to one item at a time rather than splitting the flow. Draining a spring into three
 * half-empty things at once leaves you with three that still do not work; filling them in turn means
 * the first is usable immediately.</p>
 */
public class KineticSpringItem extends Item implements ICurioItem, KineticallyCharged {

	/** Charge handed over per tick. Slower than the accumulator's 128 - this is a reserve, not a source. */
	public static final int TRANSFER_RATE = 32;

	public KineticSpringItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public void curioTick(SlotContext slotContext, ItemStack stack) {
		LivingEntity wearer = slotContext.entity();
		if (wearer == null || wearer.level()
			.isClientSide)
			return;

		int available = KineticallyCharged.getCharge(stack);
		if (available <= 0)
			return;

		ItemStack target = findNeedy(wearer, stack);
		if (target.isEmpty())
			return;

		int stored = KineticallyCharged.getCharge(target);
		int moved = Math.min(Math.min(TRANSFER_RATE, available), KineticallyCharged.CAPACITY - stored);
		if (moved <= 0)
			return;

		KineticallyCharged.setCharge(target, stored + moved);
		KineticallyCharged.setCharge(stack, available - moved);
	}

	/**
	 * The first worn item that is chargeable and not full.
	 *
	 * <p>Compared by identity so a spring never feeds itself, and so a second spring is a legitimate
	 * target - two of them are just a bigger reserve.</p>
	 */
	private static ItemStack findNeedy(LivingEntity wearer, ItemStack self) {
		return CuriosApi.getCuriosInventory(wearer)
			.map(inventory -> inventory.findCurios(worn -> KineticallyCharged.is(worn)
				&& KineticallyCharged.getCharge(worn) < KineticallyCharged.CAPACITY))
			.orElse(List.of())
			.stream()
			.map(SlotResult::stack)
			.filter(worn -> worn != self)
			.findFirst()
			.orElse(ItemStack.EMPTY);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return KineticallyCharged.getCharge(stack) < KineticallyCharged.CAPACITY;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13f * KineticallyCharged.getCharge(stack) / KineticallyCharged.CAPACITY);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0xB0B0B0;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		CKLang.translate("tooltip.radar.charge")
			.style(ChatFormatting.GRAY)
			.addTo(tooltip);
		tooltip.add(Component.literal(KineticallyCharged.getCharge(stack) + " / " + KineticallyCharged.CAPACITY)
			.withStyle(ChatFormatting.WHITE));
	}
}
