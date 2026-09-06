package me.moonscenty.createkinetism.config;

import net.createmod.catnip.config.ConfigBase;

/** Tuning for machines whose behaviour is not expressed as stress. */
public class CKMachines extends ConfigBase {

	public final ConfigInt evaporationPlantMaxHeight = i(10, 2, 64, "evaporationPlantMaxHeight",
		Comments.evaporationPlantMaxHeight);

	@Override
	public String getName() {
		return "machines";
	}

	private static class Comments {
		static String evaporationPlantMaxHeight =
			"[in blocks] Maximum height of a Thermal Evaporation Plant. Create's own Fluid Tank is capped separately, in Create's config.";

		private Comments() {
		}
	}
}
