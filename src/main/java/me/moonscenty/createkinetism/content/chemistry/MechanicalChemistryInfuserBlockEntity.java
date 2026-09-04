package me.moonscenty.createkinetism.content.chemistry;

import java.util.List;
import java.util.Optional;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;

import me.moonscenty.createkinetism.content.recipe.VatRecipe;
import me.moonscenty.createkinetism.content.vat.VatBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * A vat that also holds a tank of its own, poured into the basin below one recipe's worth at a time.
 *
 * <p>Everything about running the recipe is still {@link VatBlockEntity}'s, unchanged: the basin
 * matches and applies {@code ChemicalInfusingRecipe} exactly as it would any other vat recipe,
 * against whatever is sitting in its own two tanks. This class does not touch that at all - it only
 * keeps the basin stocked with exactly as much of our fluid as the matching recipe's
 * {@link SizedFluidIngredient} calls for, the same amount {@code BasinRecipe.apply} drains back out
 * once the recipe fires. It never just dumps everything it is holding in - only ever the one recipe's
 * worth, topped back up as soon as the basin uses it.</p>
 */
public class MechanicalChemistryInfuserBlockEntity extends VatBlockEntity {

	public SmartFluidTankBehaviour tank;

	public MechanicalChemistryInfuserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<MechanicalChemistryInfuserBlockEntity> type) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type,
			(be, context) -> be.tank == null ? null : be.tank.getCapability());
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		tank = SmartFluidTankBehaviour.single(this, 1000);
		behaviours.add(tank);
	}

	public FluidStack getCurrentFluidInTank() {
		return tank.getPrimaryHandler()
			.getFluid();
	}

	@Override
	public void tick() {
		super.tick();
		if (level != null && !level.isClientSide)
			pourIntoBasin();
	}

	/**
	 * Tops the basin up to whatever amount the recipe actually calls for - not to tank capacity.
	 * Pouring is keyed off {@link SizedFluidIngredient#amount()}, the same number
	 * {@link com.simibubi.create.content.processing.basin.BasinRecipe#apply} drains back out once the
	 * recipe fires, so the basin never ends up holding more of our fluid than one cycle can use.
	 */
	private void pourIntoBasin() {
		FluidStack held = getCurrentFluidInTank();
		if (held.isEmpty())
			return;

		Optional<BasinBlockEntity> basin = getBasin();
		if (basin.isEmpty())
			return;

		int amountNeeded = amountRequiredByRecipe(held);
		if (amountNeeded <= 0)
			return;

		IFluidHandler basinFluids = level.getCapability(Capabilities.FluidHandler.BLOCK, basin.get()
			.getBlockPos(), null);
		if (basinFluids == null)
			return;

		int alreadyPresent = 0;
		for (int i = 0; i < basinFluids.getTanks(); i++) {
			FluidStack inTank = basinFluids.getFluidInTank(i);
			if (FluidStack.isSameFluidSameComponents(inTank, held))
				alreadyPresent += inTank.getAmount();
		}

		int toPour = amountNeeded - alreadyPresent;
		if (toPour <= 0)
			return;

		FluidStack request = held.copy();
		request.setAmount(toPour);

		int transferable = basinFluids.fill(request, FluidAction.SIMULATE);
		if (transferable <= 0)
			return;

		FluidStack drained = tank.getPrimaryHandler()
			.drain(transferable, FluidAction.EXECUTE);
		basinFluids.fill(drained, FluidAction.EXECUTE);

		// The basin only re-checks its recipe when something wakes it up.
		basinChecker.scheduleUpdate();
	}

	/**
	 * How much of {@code held} a registered recipe actually asks for, or 0 if no recipe of this
	 * machine's type wants this fluid at all - in which case there is nothing to pour it towards.
	 */
	private int amountRequiredByRecipe(FluidStack held) {
		if (level == null)
			return 0;

		RecipeType<VatRecipe> type = getRecipeType().getType();
		for (RecipeHolder<VatRecipe> holder : level.getRecipeManager()
			.getAllRecipesFor(type)) {
			for (SizedFluidIngredient ingredient : holder.value()
				.getFluidIngredients())
				if (ingredient.test(held))
					return ingredient.amount();
		}
		return 0;
	}
}
