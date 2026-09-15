package me.moonscenty.createkinetism.mixin.client;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;

import me.moonscenty.createkinetism.content.chiller.StrayChillerBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps Create's head animation off a Stray Chiller.
 *
 * <p>Create turns a burner's head only while it is Fading or hotter, and a working chiller is
 * Chilled, which ranks below all of those - Create would leave its head idling. The method is
 * package-private, so the chiller cannot override it; it turns its own head instead
 * ({@code StrayChillerBlockEntity#animateHead}), and this stops Create's from chasing a second
 * target on the same tick.</p>
 */
@Mixin(value = BlazeBurnerBlockEntity.class, remap = false)
public abstract class BlazeBurnerBlockEntityMixin {

	@Inject(method = "tickAnimation", at = @At("HEAD"), cancellable = true)
	private void createkinetism$chillerAnimatesItself(CallbackInfo ci) {
		if ((Object) this instanceof StrayChillerBlockEntity)
			ci.cancel();
	}
}
