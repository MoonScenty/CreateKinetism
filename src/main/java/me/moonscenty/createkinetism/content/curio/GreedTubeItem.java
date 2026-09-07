package me.moonscenty.createkinetism.content.curio;

import java.util.List;

import me.moonscenty.createkinetism.foundation.CKLang;
import me.moonscenty.createkinetism.foundation.KineticallyCharged;
import me.moonscenty.createkinetism.registry.CKItems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Worn in a charm slot: it eats for you, out of your own pack, whether you were going to or not.
 *
 * <p>A Nutrition Bar is deliberately not a food - nothing happens when you right-click one. This is
 * the thing that spends them. The moment the hunger bar is short by even a single point it pulls one
 * bar out of the inventory, burns it for that point, and drinks. That is the whole trade: the bar
 * never empties, and neither does the pack, because the tube does not ask first.</p>
 *
 * <p>It runs on the same stored rotation as the rest of the Curios line - see
 * {@link KineticallyCharged} - so an uncharged tube is an ornament, and a charged one is a standing
 * cost against everything else you are wearing.</p>
 */
public class GreedTubeItem extends Item implements ICurioItem, KineticallyCharged {

	/** Food points restored per bar. One for one, the same rate the Mixer wrote them at. */
	private static final int FOOD_PER_BAR = 1;

	/** Charge spent per bar swallowed. A full tube is worth eight meals from empty. */
	public static final int COST_PER_BAR = 400;

	/**
	 * Ticks between mouthfuls. Refilling twenty points takes twenty seconds, which reads as being
	 * topped up rather than handed a banquet - and it keeps the drinking sound from stacking on
	 * itself twenty times in one tick.
	 */
	private static final int FEED_INTERVAL = 20;

	public GreedTubeItem(Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public void curioTick(SlotContext slotContext, ItemStack stack) {
		if (!(slotContext.entity() instanceof Player player) || player.level()
			.isClientSide)
			return;
		if (player.tickCount % FEED_INTERVAL != 0)
			return;

		FoodData hunger = player.getFoodData();
		if (hunger.getFoodLevel() >= 20)
			return;

		int charge = KineticallyCharged.getCharge(stack);
		if (charge < COST_PER_BAR)
			return;

		ItemStack bar = findBar(player);
		if (bar.isEmpty())
			return;

		bar.shrink(1);
		hunger.setFoodLevel(Math.min(20, hunger.getFoodLevel() + FOOD_PER_BAR));
		KineticallyCharged.setCharge(stack, charge - COST_PER_BAR);

		player.level()
			.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_DRINK,
				SoundSource.PLAYERS, 0.5f, 1.1f);
	}

	/**
	 * The first Nutrition Bar in the pack, offhand and armour included.
	 *
	 * <p>The stack itself is handed back rather than a slot index, so the caller shrinks the real one
	 * and the inventory notices on its own.</p>
	 */
	private static ItemStack findBar(Player player) {
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack found = inventory.getItem(slot);
			if (found.is(CKItems.NUTRITION_BAR.get()))
				return found;
		}
		return ItemStack.EMPTY;
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
		return 0xD768FB;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
		TooltipFlag flag) {
		CKLang.translate("tooltip.radar.charge")
			.style(ChatFormatting.GRAY)
			.addTo(tooltip);
		tooltip.add(Component.literal(KineticallyCharged.getCharge(stack) + " / " + KineticallyCharged.CAPACITY)
			.withStyle(ChatFormatting.WHITE));
	}
}
