package me.moonscenty.createkinetism.foundation;

import mekanism.api.IContentsListener;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.attribute.ChemicalAttributeValidator;
import mekanism.api.functions.ConstantPredicates;

import org.jetbrains.annotations.Nullable;

/** Tank shapes Mekanism does not hand out ready-made. */
public class CKChemicalTanks {

	/**
	 * An input tank that will hold radioactive chemicals.
	 *
	 * <p>{@link BasicChemicalTank#input} builds its tank with the default attribute validator, and
	 * that validator turns away any chemical carrying an attribute that asks to be checked -
	 * radiation being the one that matters. Nuclear waste, plutonium, polonium and uranium oxide all
	 * carry it, so an ordinary input tank silently refuses every one of them.</p>
	 *
	 * <p>{@link BasicChemicalTank#output} does not have the problem: output tanks are built with
	 * {@link ChemicalAttributeValidator#ALWAYS_ALLOW}, on the reasoning that a machine which makes a
	 * radioactive chemical must be able to hold what it made. A machine that <em>consumes</em> one
	 * needs the same licence on the way in, and this is it - everything else matches
	 * {@code BasicChemicalTank.input}: anything may be put in from outside, nothing may be taken back
	 * out except by the machine itself.</p>
	 */
	public static IChemicalTank radioactiveInput(long capacity, @Nullable IContentsListener listener) {
		return BasicChemicalTank.createModern(capacity, ConstantPredicates.notExternal(),
			ConstantPredicates.alwaysTrueBi(), ConstantPredicates.alwaysTrue(),
			ChemicalAttributeValidator.ALWAYS_ALLOW, listener);
	}
}
