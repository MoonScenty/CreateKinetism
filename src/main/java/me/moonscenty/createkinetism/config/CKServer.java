package me.moonscenty.createkinetism.config;

import net.createmod.catnip.config.ConfigBase;

/** Server-side config root. Stress belongs here because it has to agree across a multiplayer world. */
public class CKServer extends ConfigBase {

	public final CKKinetics kinetics =
		this.nested(0, CKKinetics::new, "Parameters and abilities of Create: Kinetism's machines");

	@Override
	public String getName() {
		return "server";
	}
}
