package me.moonscenty.createkinetism.foundation;

import java.util.function.Predicate;

import mekanism.api.IContentsListener;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
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
		return radioactiveInput(capacity, ConstantPredicates.alwaysTrue(), listener);
	}

	/**
	 * The same, but choosy about what it will hold.
	 *
	 * <p>The validator is asked on the way in and again whenever the tank is asked whether a stack is
	 * valid, so it is the place to say "this machine only handles these" - and unlike an insert
	 * predicate it also refuses the machine's own hands, which is right when the rule is about the
	 * contents rather than about who is asking.</p>
	 */
	public static IChemicalTank radioactiveInput(long capacity, Predicate<ChemicalStack> validator,
		@Nullable IContentsListener listener) {
		return BasicChemicalTank.createModern(capacity, ConstantPredicates.notExternal(),
			ConstantPredicates.alwaysTrueBi(), validator, ChemicalAttributeValidator.ALWAYS_ALLOW,
			listener);
	}
}
