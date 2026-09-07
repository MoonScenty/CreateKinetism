package me.moonscenty.createkinetism.foundation;

import me.moonscenty.createkinetism.registry.CKDataComponents;

import net.minecraft.world.item.ItemStack;

/**
 * An item the Kinetic Accumulator can wind.
 *
 * <p>The unit is the one the accumulator banks in - stress units times ticks - so a full item is a
 * real record of rotation paid for rather than a second energy currency. Every charged item shares
 * one capacity and one data component on purpose: the accumulator then has nothing to know about
 * them beyond this interface, and a new one costs no change there.</p>
 */
public interface KineticallyCharged {

	/** What a charged item holds when full, in stress units times ticks. */
	int CAPACITY = 64_000;

	static int getCharge(ItemStack stack) {
		return stack.getOrDefault(CKDataComponents.CHARGE.get(), 0);
	}

	static void setCharge(ItemStack stack, int charge) {
		stack.set(CKDataComponents.CHARGE.get(), Math.max(0, Math.min(CAPACITY, charge)));
	}

	static boolean is(ItemStack stack) {
		return stack.getItem() instanceof KineticallyCharged;
	}
}
