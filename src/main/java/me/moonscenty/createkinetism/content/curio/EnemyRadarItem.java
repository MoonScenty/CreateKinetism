package me.moonscenty.createkinetism.content.curio;

import java.util.List;

import me.moonscenty.createkinetism.foundation.KineticallyCharged;
import me.moonscenty.createkinetism.foundation.CKLang;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.AABB;

import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Worn in a charm slot: it marks what is hunting you.
 *
 * <p>Every {@value #SCAN_INTERVAL} ticks it puts Glowing on every hostile within
 * {@value #RADIUS} blocks, so they show through walls. It is not a display and there is no HUD -
 * the world itself is the readout, which is what makes it useful while running.</p>
 *
 * <p>No capability registration: Curios attaches {@code CuriosCapability.ITEM} itself to any Item
 * that implements {@link ICurioItem}.</p>

 * <p>It spends the same charge the Kinetic Disassembler does, out of the same Kinetic Accumulator,
 * and only when it actually marks something: a mob already glowing is skipped, so standing still in
 * a cleared room costs nothing. That also stops the radar from refreshing the same skeleton twenty
 * times a minute and draining itself for no new information.</p>
 */
public class EnemyRadarItem extends Item implements ICurioItem, KineticallyCharged {

	/** How far it reaches, in blocks. */
	public static final int RADIUS = 15;

	/** How long a marked mob stays lit, in ticks. */
	public static final int GLOW_DURATION = 20 * 60;

	/** Ticks between sweeps. A second is well inside the mark's own lifetime. */
	private static final int SCAN_INTERVAL = 20;

	/** Charge spent per mob marked - not per sweep. */
	public static final int COST_PER_MARK = 200;

	public EnemyRadarItem(Properties properties) {
		super(properties.stacksTo(1));
	}



	@Override
	public void curioTick(SlotContext slotContext, ItemStack stack) {
		LivingEntity wearer = slotContext.entity();
		if (wearer == null || wearer.level()
			.isClientSide)
			return;
		if (wearer.tickCount % SCAN_INTERVAL != 0)
			return;

		int charge = KineticallyCharged.getCharge(stack);
		if (charge < COST_PER_MARK)
			return;

		AABB range = wearer.getBoundingBox()
			.inflate(RADIUS);
		for (LivingEntity found : wearer.level()
			.getEntitiesOfClass(LivingEntity.class, range, EnemyRadarItem::isUnmarkedEnemy)) {

			found.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_DURATION, 0, false, false));
			charge -= COST_PER_MARK;
			if (charge < COST_PER_MARK)
				break;
		}

		KineticallyCharged.setCharge(stack, charge);
	}

	/** Hostile, alive, and not already lit - re-marking the same mob buys nothing. */
	private static boolean isUnmarkedEnemy(LivingEntity entity) {
		return entity instanceof Enemy && entity.isAlive() && !entity.hasEffect(MobEffects.GLOWING);
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
		return 0x4AEDD9;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		CKLang.translate("tooltip.radar.charge")
			.style(ChatFormatting.GRAY)
			.addTo(tooltip);
		tooltip.add(Component.literal(KineticallyCharged.getCharge(stack) + " / " + KineticallyCharged.CAPACITY)
			.withStyle(ChatFormatting.AQUA));
	}
}
