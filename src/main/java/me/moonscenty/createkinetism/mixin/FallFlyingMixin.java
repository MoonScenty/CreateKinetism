package me.moonscenty.createkinetism.mixin;

import me.moonscenty.createkinetism.content.curio.KineticElytraItem;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets a Kinetic Elytra worn on the back fly, the way one in the chest slot would.
 *
 * <p>Both of vanilla's fall-flying checks read one slot and no other:</p>
 *
 * <pre>ItemStack itemstack = this.getItemBySlot(EquipmentSlot.CHEST);
 *flag = itemstack.canElytraFly(this) &amp;&amp; itemstack.elytraFlightTick(this, this.fallFlyTicks);</pre>
 *
 * <p>NeoForge's own hook says as much in its javadoc - "The ItemStack in the Chest slot of the
 * entity" - so an accessory cannot reach it from the outside. Redirecting that one lookup is the
 * whole patch: the chest slot still wins when it can fly, and only when it cannot do we offer what
 * is on the back. Everything downstream, including the two hooks above, then runs unchanged on our
 * stack.</p>
 *
 * <p>Chosen over keeping the fall-flying flag alive by hand from a curio tick, which works only as
 * long as our tick lands after vanilla's and breaks silently when it does not.</p>
 */
@Mixin(LivingEntity.class)
public class FallFlyingMixin {

	@Redirect(method = "updateFallFlying",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack createkinetism$wingsOnTheBack(LivingEntity self, EquipmentSlot slot) {
		ItemStack chest = self.getItemBySlot(slot);
		if (chest.canElytraFly(self))
			return chest;
		ItemStack worn = KineticElytraItem.findWorn(self);
		return worn.isEmpty() ? chest : worn;
	}
}
