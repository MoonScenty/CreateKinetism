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
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * An elytra worn in a back slot, running on stored rotation instead of durability.
 *
 * <p>Vanilla will not fly this on its own. {@code LivingEntity.updateFallFlying} and
 * {@code Player.tryToStartFallFlying} both read the chest slot and nothing else - see
 * {@link me.moonscenty.createkinetism.mixin.FallFlyingMixin}, which widens exactly those two lookups
 * to notice a pair of these on the back. Everything after that is vanilla's: the same glide, the
 * same firework boost, the same physics.</p>
 *
 * <p>What is not vanilla's is the cost. An elytra wears out and is mended; this one drains
 * {@value #COST_PER_TICK} of charge a tick and is wound back up on a Kinetic Accumulator, so flight
 * is paid for in rotation like everything else here.</p>
 */
public class KineticElytraItem extends Item implements ICurioItem, KineticallyCharged {

	/** Charge spent per tick of flight. A full pair is a little over five minutes in the air. */
	public static final int COST_PER_TICK = 10;

	public KineticElytraItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	/**
	 * The pair the entity is wearing, or an empty stack.
	 *
	 * <p>Used by the mixin, so it must stay cheap - this runs every tick of every fall for every
	 * entity in the world.</p>
	 */
	public static ItemStack findWorn(LivingEntity entity) {
		return CuriosApi.getCuriosInventory(entity)
			.flatMap(inventory -> inventory.findFirstCurio(
				stack -> stack.getItem() instanceof KineticElytraItem))
			.map(result -> result.stack())
			.orElse(ItemStack.EMPTY);
	}

	/** Charge is the fuel, so an empty pair simply will not open. */
	@Override
	public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
		return KineticallyCharged.getCharge(stack) >= COST_PER_TICK;
	}

	@Override
	public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
		int charge = KineticallyCharged.getCharge(stack);
		if (charge < COST_PER_TICK)
			return false;
		if (!entity.level().isClientSide)
			KineticallyCharged.setCharge(stack, charge - COST_PER_TICK);
		return true;
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
		return 0xC7A03A;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		CKLang.translate("tooltip.radar.charge")
			.style(ChatFormatting.GRAY)
			.addTo(tooltip);
		tooltip.add(Component.literal(KineticallyCharged.getCharge(stack) + " / " + KineticallyCharged.CAPACITY)
			.withStyle(ChatFormatting.GOLD));
	}
}
