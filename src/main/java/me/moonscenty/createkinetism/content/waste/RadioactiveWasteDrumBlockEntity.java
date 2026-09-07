package me.moonscenty.createkinetism.content.waste;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import me.moonscenty.createkinetism.content.recipe.DecayingRecipe;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * A drum of waste that quietly gets smaller.
 *
 * <p>Two things it does, and both are about the same problem - waste is the one thing in this mod
 * that every recipe makes and nothing consumes.</p>
 *
 * <p><b>It decays.</b> How fast, and what into, is a {@code decaying} recipe rather than a constant
 * here - two millibuckets a second as shipped. Slow enough that a drum is not a disposal chute (a
 * reactor line will outrun one), but it makes waste a storage problem rather than a dead end.</p>
 *
 * <p><b>It falls.</b> A drum with another drum beneath it hands its contents down, so a column of
 * them fills from the bottom and reads as one deep tank. Only into another drum: pushing into
 * whatever happened to be under it would make the drum a pipe, and the point of the block is that
 * waste stops here.</p>
 */
public class RadioactiveWasteDrumBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

	public static final int CAPACITY = 1000;

	/** Ticks between decay steps - the block's lazy tick rate, kept here so the maths can name it. */
	private static final int LAZY_TICK_RATE = 20;

	/** Per tick, into the drum below. Only ever moves what that one still has room for. */
	private static final int SETTLE_RATE = 20;

	public SmartFluidTankBehaviour tank;

	public RadioactiveWasteDrumBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		setLazyTickRate(LAZY_TICK_RATE);
	}

	/**
	 * Whatever a {@code decaying} recipe names, and nothing else.
	 *
	 * <p>The drum has no drain of its own, so a fluid it cannot rot is a fluid a player cannot get
	 * back out by any means the block offers. Refusing at the inlet is the only place to say so.</p>
	 */
	public boolean isWaste(FluidStack stack) {
		if (stack.isEmpty())
			return true;
		return findRecipe(stack) != null;
	}

	@Nullable
	private DecayingRecipe findRecipe(FluidStack held) {
		if (level == null || held.isEmpty())
			return null;
		FluidStack still = FluidHelper.copyStackWithAmount(
			new FluidStack(FluidHelper.convertToStill(held.getFluid()), 1), held.getAmount());
		return level.getRecipeManager()
			.getAllRecipesFor(CKRecipeTypes.DECAYING.<RecipeInput, DecayingRecipe>getType())
			.stream()
			.map(RecipeHolder::value)
			// Two things this must not do. Not SizedFluidIngredient.test, which also demands the
			// amount - here the amount is the decay rate, not a condition, and a pipe offers whatever it
			// happens to be carrying. And not the stack as handed over: a Create pipe delivers the
			// *flowing* fluid, while a recipe names the still one, so they never match untranslated.
			.filter(recipe -> recipe.getFluidIngredients()
				.getFirst()
				.ingredient()
				.test(still))
			.findFirst()
			.orElse(null);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		// The behaviour builds its own tank in the constructor with no hook to replace it, so the
		// restriction goes on afterwards - FluidTank.fill consults the validator either way.
		tank = SmartFluidTankBehaviour.single(this, CAPACITY);
		tank.getPrimaryHandler()
			.setValidator(this::isWaste);
		behaviours.add(tank);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<RadioactiveWasteDrumBlockEntity> type) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type,
			(be, context) -> be.tank == null ? null : be.tank.getCapability());
	}

	public FluidStack getContents() {
		return tank.getPrimaryHandler()
			.getFluid();
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		settle();
	}

	/**
	 * Decay is the only thing on a timer, and one second is a fine granularity for the couple of
	 * millibuckets a recipe usually names. The rate is read the way the boiler reads its own - the
	 * ingredient's amount over the recipe's duration - and rounded up, so a recipe slower than the
	 * lazy tick still rots by at least a millibucket rather than by nothing at all.
	 */
	@Override
	public void lazyTick() {
		super.lazyTick();
		if (level == null || level.isClientSide)
			return;
		FluidStack held = getContents();
		if (held.isEmpty())
			return;

		DecayingRecipe recipe = findRecipe(held);
		if (recipe == null)
			return;

		SizedFluidIngredient ingredient = recipe.getFluidIngredients()
			.getFirst();
		int duration = Math.max(1, recipe.getProcessingDuration());
		int lost = Math.max(1, Math.round(ingredient.amount() * (LAZY_TICK_RATE / (float) duration)));
		lost = Math.min(lost, held.getAmount());

		FluidStack result = recipe.getFluidResults()
			.isEmpty() ? FluidStack.EMPTY
				: recipe.getFluidResults()
					.getFirst();

		tank.getPrimaryHandler()
			.drain(lost, FluidAction.EXECUTE);
		// A recipe with no result is the usual case: the waste simply goes away.
		if (!result.isEmpty())
			tank.getPrimaryHandler()
				.fill(result.copyWithAmount(
					Math.round(result.getAmount() * (lost / (float) ingredient.amount()))), FluidAction.EXECUTE);
		notifyUpdate();
	}

	/** Hand what fits down to the drum below. */
	private void settle() {
		FluidStack held = getContents();
		if (held.isEmpty())
			return;

		BlockEntity beneath = level.getBlockEntity(worldPosition.below());
		if (!(beneath instanceof RadioactiveWasteDrumBlockEntity drum))
			return;

		IFluidHandler below = level.getCapability(Capabilities.FluidHandler.BLOCK, drum.getBlockPos(),
			Direction.UP);
		if (below == null)
			return;

		FluidStack offer = held.copyWithAmount(Math.min(SETTLE_RATE, held.getAmount()));
		int moved = below.fill(offer, FluidAction.EXECUTE);
		if (moved <= 0)
			return;

		tank.getPrimaryHandler()
			.drain(moved, FluidAction.EXECUTE);
		notifyUpdate();
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		return containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability());
	}

}
