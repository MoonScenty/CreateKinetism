package me.moonscenty.createkinetism.content.chiller;

import me.moonscenty.createkinetism.CreateKinetism;

import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SpriteShifter;

/**
 * The scrolling flame over a working chiller, the same pair of sprite shifts Create uses for a Blaze
 * Burner's flame.
 *
 * <p>Kept apart from {@code CKSpriteShifts} and touched only from the client entrypoint and the
 * renderer. The scroll targets are not named by any model; they reach the block atlas because it
 * stitches everything under {@code textures/block/}, and the shift has to be registered before that
 * stitch happens - hence the client-constructor {@link #init} call.</p>
 */
public class StrayChillerSpriteShifts {

	public static final SpriteShiftEntry
		FLAME = get("stray_chiller/burner_flame", "stray_chiller/burner_flame_scroll"),
		SUPER_FLAME = get("stray_chiller/burner_flame", "stray_chiller/burner_flame_superheated_scroll");

	private static SpriteShiftEntry get(String original, String target) {
		return SpriteShifter.get(CreateKinetism.asResource("block/" + original),
			CreateKinetism.asResource("block/" + target));
	}

	/** Loads the class, which registers both shifts. */
	public static void init() {}
}
