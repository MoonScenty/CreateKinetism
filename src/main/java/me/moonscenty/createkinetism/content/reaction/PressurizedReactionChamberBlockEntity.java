package me.moonscenty.createkinetism.content.reaction;

import java.util.List;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;

import me.moonscenty.createkinetism.content.recipe.ReactingRecipe;
import me.moonscenty.createkinetism.content.vat.VatBlockEntity;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

/**
 * A vat that stands on its basin and holds one of the three inputs itself.
 *
 * <p>Two things differ from the vats above a basin. It looks one block down rather than two, because
 * it is sitting on the thing it works; and it keeps a tank of its own for the reaction's fluid, the
 * way the Injection Chamber keeps one for its gas. The basin still holds the item and the gas and
 * still takes both products - see {@link ReactingRecipe} for why the split is written into the
 * ingredient order.</p>
 */
public class PressurizedReactionChamberBlockEntity extends VatBlockEntity {

	/** One bucket, matching the other machines here that carry their own tank. */
	public static final int CAPACITY = 1000;

	public SmartFluidTankBehaviour tank;

	public PressurizedReactionChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		tank = SmartFluidTankBehaviour.single(this, CAPACITY);
		behaviours.add(tank);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<PressurizedReactionChamberBlockEntity> type) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type,
			(be, context) -> be.tank == null ? null : be.tank.getCapability());
	}

	public FluidStack getCurrentFluidInTank() {
		return tank.getPrimaryHandler()
			.getFluid();
	}

	/** The block it stands on, not the one two below - this machine has no gap under it. */
	@Override
	protected Optional<BasinBlockEntity> getBasin() {
		if (level == null)
			return Optional.empty();
		BlockEntity basin = level.getBlockEntity(worldPosition.below());
		return basin instanceof BasinBlockEntity found ? Optional.of(found) : Optional.empty();
	}

	/** The block does not carry the recipe type, so say it here. */
	@Override
	public CKRecipeTypes getRecipeType() {
		return CKRecipeTypes.REACTING;
	}

	/**
	 * Both halves have to be satisfied: the reaction fluid out of our own tank, and the item and gas
	 * out of the basin. Either missing and the head never comes down, which is the behaviour you want
	 * when someone has filled one side and not the other.
	 */
	@Override
	protected <I extends RecipeInput> boolean matchBasinRecipe(Recipe<I> recipe) {
		if (!(recipe instanceof ReactingRecipe reacting))
			return false;
		if (!reacting.getReactant()
			.test(getCurrentFluidInTank()))
			return false;
		return getBasin().map(basin -> BasinRecipe.match(basin, recipe))
			.orElse(false);
	}

	/** The basin half is Create's; the tank half is ours, drained alongside it. */
	@Override
	protected void applyBasinRecipe() {
		super.applyBasinRecipe();
		if (!(currentRecipe instanceof ReactingRecipe reacting))
			return;
		FluidStack held = getCurrentFluidInTank();
		int cost = reacting.getReactant()
			.amount();
		tank.getPrimaryHandler()
			.setFluid(FluidHelper.copyStackWithAmount(held, held.getAmount() - cost));
	}

	@Override
	protected boolean matchStaticFilters(RecipeHolder<? extends Recipe<?>> recipe) {
		return recipe.value() instanceof ReactingRecipe;
	}
}
