package me.moonscenty.createkinetism.content.heat;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.recipe.HeatCondition;

/**
 * The two heat levels this mod adds to Create's own enums, and how they rank against Create's.
 *
 * <p>The constants themselves are appended to {@link HeatLevel} and {@link HeatCondition} by
 * {@code HeatLevelMixin} and {@code HeatConditionMixin}, so a recipe JSON can say
 * {@code "heat_requirement": "sodium_heated"} or {@code "chilled"} and a burner's {@code blaze}
 * block state can hold them. Appending is the only option - an enum's existing ordinals are fixed -
 * which puts both after {@code SEETHING}. Ordinal order is therefore no longer heat order, and every
 * comparison Create makes goes through {@link #rank} instead (see the mixins).</p>
 *
 * <ul>
 *   <li>{@link #SODIUM_HEATED} - hotter than Seething. A lit Sodium Burner.</li>
 *   <li>{@link #CHILLED} - colder than no heat at all. A working Stray Chiller.</li>
 * </ul>
 *
 * <p>Looked up by name rather than held by the mixins, because the mixins run inside the enums' own
 * static initialisers, before {@code valueOf} can work.</p>
 */
public final class CKHeatLevels {

	public static final HeatLevel SODIUM_HEATED = HeatLevel.valueOf("SODIUM_HEATED");
	public static final HeatLevel CHILLED = HeatLevel.valueOf("CHILLED");

	public static final HeatCondition SODIUM_HEATED_CONDITION = HeatCondition.valueOf("SODIUM_HEATED");
	public static final HeatCondition CHILLED_CONDITION = HeatCondition.valueOf("CHILLED");

	/** Text colour of the two new conditions in JEI - the cyan painted on the lit Sodium Burner, and ice. */
	public static final int SODIUM_HEATED_COLOR = 0x40E0FF;
	public static final int CHILLED_COLOR = 0xD8F6FF;

	private CKHeatLevels() {}

	/**
	 * Where a heat level sits on the thermometer: Create's five keep their ordinals (0 to 4), Sodium
	 * Heated is one above Seething and Chilled one below None.
	 */
	public static int rank(HeatLevel level) {
		if (level == SODIUM_HEATED)
			return HeatLevel.SEETHING.ordinal() + 1;
		if (level == CHILLED)
			return -1;
		return level.ordinal();
	}

	/**
	 * The tier a heat condition asks a heater for - the number this mod's own machines compare
	 * against a {@code BoilerHeater}'s heat: None 0, Heated 1, Super-Heated 2, Sodium Heated 3.
	 * Chilled is -1; no heater reports a negative heat, so a heat-counting machine never satisfies it.
	 */
	public static int tierOf(HeatCondition condition) {
		if (condition == SODIUM_HEATED_CONDITION)
			return 3;
		if (condition == CHILLED_CONDITION)
			return -1;
		return condition == HeatCondition.SUPERHEATED ? 2 : condition == HeatCondition.HEATED ? 1 : 0;
	}
}
