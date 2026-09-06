package me.moonscenty.createkinetism.config;

import net.createmod.catnip.config.ConfigBase;

/** Groups the kinetic tuning knobs under one heading, the way Create does. */
public class CKKinetics extends ConfigBase {

	public final CKStress stressValues = this.nested(1, CKStress::new, Comments.stress);
	public final CKMachines machines = this.nested(1, CKMachines::new, Comments.machines);

	@Override
	public String getName() {
		return "kinetics";
	}

	private static class Comments {
		static String stress = "Fine tune the kinetic stats of individual components";
		static String machines = "Limits and rates that are not stress";

		private Comments() {
		}
	}
}
