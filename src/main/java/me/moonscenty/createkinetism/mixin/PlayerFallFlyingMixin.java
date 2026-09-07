package me.moonscenty.createkinetism.mixin;

import me.moonscenty.createkinetism.content.curio.KineticElytraItem;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The take-off half of {@link FallFlyingMixin}.
 *
 * <p>{@code updateFallFlying} keeps a glide going; this is what starts one. They are the same
 * one-slot lookup in two different classes, so they need the same redirect twice - without this the
 * wings would sustain a flight the player can never begin.</p>
 */
@Mixin(Player.class)
public class PlayerFallFlyingMixin {

	@Redirect(method = "tryToStartFallFlying",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/player/Player;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack createkinetism$wingsOnTheBack(Player self, EquipmentSlot slot) {
		ItemStack chest = self.getItemBySlot(slot);
		if (chest.canElytraFly(self))
			return chest;
		ItemStack worn = KineticElytraItem.findWorn((LivingEntity) self);
		return worn.isEmpty() ? chest : worn;
	}
}
