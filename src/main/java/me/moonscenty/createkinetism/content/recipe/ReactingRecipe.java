package me.moonscenty.createkinetism.content.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.NonNullList;

import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * Mekanism: Pressurized Reaction Chamber. An item, a fluid and a gas into an item and a gas.
 *
 * <p>Mekanism keeps all three inputs inside the machine. We split them the way the block is built:
 * the chamber holds the fluid in a tank of its own, and the basin underneath holds the item and the
 * gas, and takes both products back. Every chemical in this mod is the same kind of fluid, so the
 * distinction is not mechanical - it is where each one goes.</p>
 *
 * <p>That split is spelled in the ingredient order rather than in an extra field: <b>the first fluid
 * ingredient is the chamber's own</b>, and everything after it belongs to the basin.
 * {@link #getFluidIngredients()} therefore reports only the basin's share, because that is the
 * question Create's basin code is asking when it calls it - what must the basin provide. The
 * chamber's own reactant is read through {@link #getReactant()} instead.</p>
 */
public class ReactingRecipe extends VatRecipe {

	public ReactingRecipe(ProcessingRecipeParams params) {
		super(CKRecipeTypes.REACTING, params);
	}

	/** The fluid the chamber holds in its own tank - always the first one listed. */
	public SizedFluidIngredient getReactant() {
		if (fluidIngredients.isEmpty())
			throw new IllegalStateException("Reacting recipe has no fluid for the chamber's own tank");
		return fluidIngredients.get(0);
	}

	/**
	 * What the basin has to provide: everything after the chamber's own.
	 *
	 * <p>Create's {@code BasinRecipe.apply} takes this list as "drain these from the basin", so the
	 * chamber's reactant must not be in it - the basin does not have it and never will.</p>
	 */
	@Override
	public NonNullList<SizedFluidIngredient> getFluidIngredients() {
		NonNullList<SizedFluidIngredient> fromBasin = NonNullList.create();
		for (int i = 1; i < fluidIngredients.size(); i++)
			fromBasin.add(fluidIngredients.get(i));
		return fromBasin;
	}

	/**
	 * The basin's own limit, not a design one.
	 *
	 * <p>Create's processing ingredients carry no count, so a recipe that wants twenty planks lists
	 * the ingredient twenty times and {@code BasinRecipe} pulls one item per entry. The cap has to
	 * leave room for that; the recipes themselves still only ever name a single kind of item.</p>
	 */
	@Override
	protected int getMaxInputCount() {
		return 64;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

	/** Two: the chamber's own, then the basin's gas. */
	@Override
	protected int getMaxFluidInputCount() {
		return 2;
	}

	@Override
	protected int getMaxFluidOutputCount() {
		return 1;
	}
}
