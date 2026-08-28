package me.moonscenty.createkinetism.content.oil;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;

import net.createmod.catnip.lang.Lang;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>How the column is being driven. Each mode splits crude into different fractions and has its
 * own requirement, which is what gives the oil chain a progression instead of one big machine:</p>
 *
 * <ul>
 * <li>{@code FLASH} - the cheap one. Inject steam, take the light ends off the top.</li>
 * <li>{@code ATMOSPHERIC} - heat the column from below. Needs a wider tank.</li>
 * <li>{@code VACUUM} - heat it and pump the air out, which is the only way to crack the heaviest
 * fractions without burning them.</li>
 * </ul>
 *
 * <p>The icons are borrowed from Create's sheet rather than shipping our own.</p>
 */
public enum DistilMode implements INamedIconOptions {

	DISTIL_FLASH(AllIcons.I_PATTERN_CHANCE_25),
	DISTIL_ATMOSPHERIC(AllIcons.I_PATTERN_CHANCE_50),
	DISTIL_VACUUM(AllIcons.I_PATTERN_CHANCE_75);

	private final AllIcons icon;
	private final String translationKey;

	DistilMode(AllIcons icon) {
		this.icon = icon;
		this.translationKey = "gui.distil_mode." + Lang.asId(name());
	}

	@Override
	public AllIcons getIcon() {
		return icon;
	}

	@Override
	public String getTranslationKey() {
		return "createkinetism." + translationKey;
	}

	public String getRawTranslationKey() {
		return translationKey;
	}
}
