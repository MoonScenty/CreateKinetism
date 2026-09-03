package me.moonscenty.createkinetism.content.injection;

import java.util.List;

import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;

import me.moonscenty.createkinetism.content.recipe.InjectingRecipe;
import me.moonscenty.createkinetism.content.vat.VatBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A vat that also holds a tank of its own.
 *
 * <p>Mekanism's Chemical Injection Chamber takes an item and a gas. The basin under this block is
 * the item half, matching every other vat; this tank is the gas half, checked and consumed on either
 * side of the basin match the same way the Combiner's held infusion item is - {@link BasinRecipe#match}
 * only knows about the basin, so it has no way to see a requirement that lives somewhere else.</p>
 */
public class InjectionChamberBlockEntity extends VatBlockEntity {

	/** How far past the vat's own 7px rest position the head plunges at the peak of a cycle. */
	private static final float PLUNGE_AMPLITUDE = 10 / 16f;

	public SmartFluidTankBehaviour tank;

	public InjectionChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<InjectionChamberBlockEntity> type) {
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

	/**
	 * The vat's own curve, rescaled: {@code VatBlockEntity} swings the mixer head almost a full block
	 * to press into the basin, but this machine only needs to read as "the plunger moved" - ten
	 * pixels rather than sixteen.
	 */
	@Override
	public float getRenderedHeadOffset(float partialTicks) {
		float hump = super.getRenderedHeadOffset(partialTicks) - 7 / 16f;
		return 7 / 16f + hump * PLUNGE_AMPLITUDE;
	}

	/**
	 * The basin half is Create's; the gas half is ours. Both have to be satisfied, or the head never
	 * comes down - the behaviour you want when someone has filled the basin but not the tank.
	 */
	@Override
	protected <I extends RecipeInput> boolean matchBasinRecipe(Recipe<I> recipe) {
		if (!(recipe instanceof InjectingRecipe injecting))
			return false;
		if (!injecting.matchesFluid(getCurrentFluidInTank()))
			return false;
		return getBasin().map(basin -> BasinRecipe.match(basin, recipe))
			.orElse(false);
	}

	/** The recipe's gas cost, drained from our own tank as the basin half is applied. */
	@Override
	protected void applyBasinRecipe() {
		super.applyBasinRecipe();
		if (!(currentRecipe instanceof InjectingRecipe injecting))
			return;
		FluidStack fluid = getCurrentFluidInTank();
		int cost = injecting.getRequiredFluid()
			.amount();
		tank.getPrimaryHandler()
			.setFluid(FluidHelper.copyStackWithAmount(fluid, fluid.getAmount() - cost));
	}

	@Override
	protected boolean matchStaticFilters(RecipeHolder<? extends Recipe<?>> recipe) {
		return recipe.value() instanceof InjectingRecipe;
	}
}
