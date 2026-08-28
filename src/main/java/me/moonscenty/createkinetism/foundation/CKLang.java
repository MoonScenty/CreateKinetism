package me.moonscenty.createkinetism.foundation;

import me.moonscenty.createkinetism.CreateKinetism;

import net.createmod.catnip.lang.LangBuilder;

/** Our namespace's equivalent of Create's {@code CreateLang}. */
public class CKLang {

	public static LangBuilder builder() {
		return new LangBuilder(CreateKinetism.ID);
	}

	public static LangBuilder translate(String langKey, Object... args) {
		return builder().translate(langKey, args);
	}
}
