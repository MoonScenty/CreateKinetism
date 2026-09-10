package me.moonscenty.createkinetism.foundation;

import java.util.List;

import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;

import net.minecraft.core.Direction;

import org.jetbrains.annotations.Nullable;

/**
 * A machine's chemical handler, bound to the face it was asked for.
 *
 * <p>Mekanism decides what a tank will accept from two things: the tank's own rules and the
 * {@code AutomationType} of whoever is asking. An input tank refuses to be drained from outside; an
 * output tank refuses to be filled from outside. Both allow it internally, because that is how the
 * machine itself moves gas around.</p>
 *
 * <p>Which of the two applies comes from the {@code Direction} carried into the call, and
 * {@link IMekanismChemicalHandler}'s default side is {@code null} - meaning internal. So a machine
 * that hands out <em>itself</em> as the capability, no matter which face was asked for, gives every
 * pipe on every side the machine's own privileges: gas can be pushed into the outlet and pulled back
 * out of the inlet. With one tank that is invisible. With an inlet and an outlet it is not.</p>
 *
 * <p>This wrapper is the missing half. {@code getSideFor} is what the no-side methods on
 * {@code IChemicalHandler} - the ones a pipe actually calls, since the face was already chosen when
 * it looked the capability up - route through.</p>
 */
public record SidedChemicalAccess(IMekanismChemicalHandler machine, @Nullable Direction side)
	implements IMekanismChemicalHandler {

	@Override
	@Nullable
	public Direction getSideFor() {
		return side;
	}

	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		return machine.getChemicalTanks(side);
	}

	/** Straight through: the tanks are the machine's, so a change to them is the machine's news. */
	@Override
	public void onContentsChanged() {
		machine.onContentsChanged();
	}
}
