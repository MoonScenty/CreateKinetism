package me.moonscenty.createkinetism.content.oil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;

import me.moonscenty.createkinetism.content.recipe.DistillingRecipe;
import me.moonscenty.createkinetism.content.steel.SteelTankBlockEntity;
import me.moonscenty.createkinetism.foundation.CKLang;
import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKFluids;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Bolted to the side of a Steel Tank stack, this turns the stack into a fractionating column.
 * Crude goes in here, and each pair of tank layers hands one cut to whichever Distillation Output is
 * tapped into it.</p>
 *
 * <p>The mode - set by scrolling on the front face - is what gates progression:</p>
 *
 * <ul>
 * <li><b>Flash</b>: inject steam. Cheapest, splits crude into the fewest cuts.</li>
 * <li><b>Atmospheric</b>: heat the column from below. Needs a 3-wide stack.</li>
 * <li><b>Vacuum</b>: heat it <em>and</em> keep the air pumped out, which is the only way to get at
 * the heaviest fractions. The controller fills its own air tank back up every tick, so you have to
 * keep draining it to hold the vacuum.</li>
 * </ul>
 */
public class DistillationControllerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

	private static final int AIR_CAPACITY = 8000;
	private static final int VACUUM_THRESHOLD = 500;
	private static final int STEAM_PER_CYCLE = 500;
	private static final int STEAM_THRESHOLD = 1000;

	public ScrollOptionBehaviour<DistilMode> distilMode;
	public SmartFluidTankBehaviour inputTank;
	public SmartFluidTankBehaviour outputTank;
	public IFluidHandler fluidCapability;

	public DistillingRecipe currentRecipe;
	public BlockPos tankController;
	public int requiredOutputs = 0;
	public Map<Integer, BlockPos> outputs = new HashMap<>();
	public int timer;

	public LerpedFloat gaugeLevel = LerpedFloat.linear();

	public DistillationControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		setLazyTickRate(10);
		gaugeLevel.chase(0, 1 / 16f, LerpedFloat.Chaser.EXP);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		distilMode = new ScrollOptionBehaviour<>(DistilMode.class, CKLang.translate("gui.distil_mode")
			.component(), this, new DistilModeBoxTransform());
		distilMode.withCallback(i -> {
			// Switching into vacuum starts the column full of air; you have to pump it down yourself.
			if (DistilMode.values()[i] == DistilMode.DISTIL_VACUUM)
				outputTank.getPrimaryHandler()
					.setFluid(new FluidStack(CKFluids.AIR.get()
						.getSource(), AIR_CAPACITY));
			else
				outputTank.getPrimaryHandler()
					.setFluid(FluidStack.EMPTY);
		});
		behaviours.add(distilMode);

		inputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 2, 4000, true)
			.whenFluidUpdates(this::sendData)
			.forbidExtraction();
		outputTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, this, 1, AIR_CAPACITY, true)
			.whenFluidUpdates(this::sendData)
			.forbidInsertion();
		behaviours.add(inputTank);
		behaviours.add(outputTank);

		fluidCapability = new CombinedTankWrapper(inputTank.getCapability(), outputTank.getCapability());
	}

	public Optional<SteelTankBlockEntity> getTankControllerBE() {
		if (tankController == null)
			return Optional.empty();
		BlockEntity be = level.getBlockEntity(tankController);
		if (be instanceof SteelTankBlockEntity tank)
			return Optional.ofNullable(tank.getControllerBE());
		return Optional.empty();
	}

	/** @return true if this stage was already claimed by a different output block */
	public boolean addOutput(int stage, BlockPos pos) {
		if (outputs.containsKey(stage))
			return !outputs.get(stage)
				.equals(pos);

		outputs.put(stage, pos);
		recountRequiredOutputs();
		sendData();
		return false;
	}

	public void removeOutput(int stage) {
		outputs.remove(stage);
		if (level.isClientSide)
			return;
		recountRequiredOutputs();
		sendData();
	}

	private void recountRequiredOutputs() {
		if (currentRecipe != null) {
			requiredOutputs = currentRecipe.getFluidResults()
				.size() - outputs.size();
			return;
		}
		requiredOutputs = getTankControllerBE().map(tank -> tank.getHeight() / 2 + 2 - outputs.size())
			.orElse(0);
	}

	public int getSteam() {
		IFluidHandler fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition, null);
		if (fluids == null)
			return 0;
		for (int i = 0; i < fluids.getTanks(); i++) {
			FluidStack fluid = fluids.getFluidInTank(i);
			if (fluid.getFluid() == CKFluids.STEAM.get()
				.getSource())
				return fluid.getAmount();
		}
		return 0;
	}

	public int getAir() {
		return outputTank.getPrimaryHandler()
			.getFluidInTank(0)
			.getAmount();
	}

	public boolean hasSteam() {
		return getSteam() >= STEAM_THRESHOLD;
	}

	public boolean hasVacuum() {
		return getAir() < VACUUM_THRESHOLD;
	}

	public boolean canProcess() {
		if (currentRecipe == null)
			return false;
		if (outputs.size() < currentRecipe.getFluidResults()
			.size())
			return false;
		if (inputTank.isEmpty())
			return false;

		SteelTankBlockEntity tank = getTankControllerBE().orElse(null);
		if (tank == null || tank.getWidth() < 2)
			return false;

		return switch (distilMode.get()) {
			case DISTIL_FLASH -> hasSteam();
			case DISTIL_ATMOSPHERIC -> tank.getWidth() == 3 && tank.heat > 1;
			case DISTIL_VACUUM -> hasVacuum() && tank.heat > 1;
		};
	}

	public float getGaugeTarget() {
		return switch (distilMode.get()) {
			case DISTIL_FLASH -> getSteam() / 4000f;
			case DISTIL_ATMOSPHERIC -> getTankControllerBE().map(tank -> (float) Math.min(1.0, tank.heat / 6f))
				.orElse(0f);
			case DISTIL_VACUUM -> 1.0f - (getAir() / (float) AIR_CAPACITY);
		};
	}

	@Override
	public void tick() {
		super.tick();

		SteelTankBlockEntity tank = getTankControllerBE().orElse(null);
		if (tank == null)
			return;

		if (level.isClientSide) {
			gaugeLevel.updateChaseTarget(getGaugeTarget());
			gaugeLevel.tickChaser();
			return;
		}

		// Air leaks back in proportionally to how tall the column is.
		if (distilMode.get() == DistilMode.DISTIL_VACUUM) {
			if (getAir() < AIR_CAPACITY)
				sendData();
			outputTank.getPrimaryHandler()
				.fill(new FluidStack(CKFluids.AIR.get()
					.getSource(), tank.getHeight() * 15), FluidAction.EXECUTE);
		}

		if (currentRecipe != null && !matchesCurrentRecipe())
			currentRecipe = null;
		if (!canProcess())
			return;

		if (timer > 0) {
			timer -= distilMode.get() == DistilMode.DISTIL_FLASH ? 2 : (int) tank.heat;
			return;
		}

		// Simulate first: if any stage cannot take its cut, back off rather than voiding product.
		for (boolean simulate : Iterate.trueAndFalse) {
			int stage = 0;
			for (FluidStack result : currentRecipe.getFluidResults()) {
				stage++;
				BlockPos pos = outputs.get(stage);
				if (pos == null)
					continue;
				if (!(level.getBlockEntity(pos) instanceof DistillationOutputBlockEntity out))
					continue;

				out.tankInventory.allowInsertion();
				int filled = out.tankInventory.getPrimaryHandler()
					.fill(result, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
				out.tankInventory.forbidInsertion();

				if (simulate && filled < result.getAmount()) {
					timer += 50;
					return;
				}
			}
		}

		consumeFeedstock();

		if (matchesCurrentRecipe())
			timer = currentRecipe.getProcessingDuration();
		else
			currentRecipe = null;
		sendData();
	}

	private void consumeFeedstock() {
		SizedFluidIngredient ingredient = currentRecipe.getFluidIngredients()
			.get(0);
		int amountRequired = ingredient.amount();
		IFluidHandler fluids = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition, null);
		if (fluids == null)
			return;

		for (int tank = 0; tank < fluids.getTanks(); tank++) {
			FluidStack fluidStack = fluids.getFluidInTank(tank);

			if (distilMode.get() == DistilMode.DISTIL_FLASH && fluidStack.getFluid() == CKFluids.STEAM.get()
				.getSource())
				fluidStack.shrink(Math.min(STEAM_PER_CYCLE, fluidStack.getAmount()));

			if (!ingredient.test(fluidStack))
				continue;
			int drained = Math.min(amountRequired, fluidStack.getAmount());
			fluidStack.shrink(drained);
			amountRequired -= drained;
		}
	}

	private boolean matchesCurrentRecipe() {
		if (currentRecipe == null)
			return false;
		if (currentRecipe.getMode() != distilMode.get())
			return false;
		return currentRecipe.hasFeedstock(level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition, null));
	}

	@Override
	public void lazyTick() {
		super.lazyTick();

		// Find, or re-check, the tank stack we are bolted to.
		if (tankController == null) {
			for (Direction d : Iterate.directions) {
				BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(d));
				if (neighbour instanceof SteelTankBlockEntity tank) {
					tankController = tank.getController();
					break;
				}
			}
		} else if (!level.getBlockState(tankController)
			.is(CKBlocks.STEEL_TANK.get())) {
			tankController = null;
		}

		if (tankController == null)
			return;

		if (level.getBlockEntity(tankController) instanceof SteelTankBlockEntity tank) {
			tank.distillationController = worldPosition;
			tank.setDistillationMode(true);
		}

		if (level.isClientSide || currentRecipe != null)
			return;

		for (RecipeHolder<DistillingRecipe> holder : level.getRecipeManager()
			.getAllRecipesFor(CKRecipeTypes.DISTILLING.<RecipeInput, DistillingRecipe>getType())) {
			DistillingRecipe recipe = holder.value();
			if (recipe.getMode() != distilMode.get())
				continue;
			if (!recipe.hasFeedstock(level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition, null)))
				continue;
			currentRecipe = recipe;
			timer = recipe.getProcessingDuration();
			recountRequiredOutputs();
			sendData();
			return;
		}
	}

	@Override
	public void remove() {
		super.remove();
		if (tankController == null)
			return;
		if (level.getBlockEntity(tankController) instanceof SteelTankBlockEntity tank) {
			tank.distillationController = null;
			tank.setDistillationMode(false);
		}
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		tag.putInt("RequiredOutputs", requiredOutputs);
		super.write(tag, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		requiredOutputs = tag.getInt("RequiredOutputs");
		super.read(tag, registries, clientPacket);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		CKLang.translate("gui.distil_mode")
			.text(":")
			.forGoggles(tooltip);
		CKLang.translate(distilMode.get()
			.getRawTranslationKey())
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip, 1);

		containedFluidTooltip(tooltip, isPlayerSneaking, fluidCapability);

		if (distilMode.get() == DistilMode.DISTIL_VACUUM && !hasVacuum())
			hint(tooltip, "gui.distil_hint.vacuum");
		if (distilMode.get() == DistilMode.DISTIL_FLASH && !hasSteam())
			hint(tooltip, "gui.distil_hint.flash");
		if (requiredOutputs > 0)
			hint(tooltip, "gui.distil_hint.missing_outputs");

		SteelTankBlockEntity tank = getTankControllerBE().orElse(null);
		if (tank == null)
			return true;
		if (distilMode.get() != DistilMode.DISTIL_FLASH && tank.heat < 2)
			hint(tooltip, "gui.distil_hint.heat");
		if (tank.getWidth() < 2 || (distilMode.get() == DistilMode.DISTIL_ATMOSPHERIC && tank.getWidth() < 3))
			hint(tooltip, "gui.distil_hint.width");

		return true;
	}

	private static void hint(List<Component> tooltip, String key) {
		CKLang.translate(key)
			.style(ChatFormatting.GOLD)
			.forGoggles(tooltip);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<DistillationControllerBlockEntity> type) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type,
			(be, context) -> context == null
				|| context.getAxis() == DistillationControllerBlock.getAxis(be.getBlockState())
					? be.fluidCapability
					: null);
	}

	private class DistilModeBoxTransform extends ValueBoxTransform.Sided {

		@Override
		protected Vec3 getSouthLocation() {
			return VecHelper.voxelSpace(8f, 8f, 16f);
		}

		@Override
		protected boolean isSideActive(BlockState state, Direction direction) {
			return DistillationControllerBlock.shouldRenderHeadOnFaceStatic(state, direction);
		}
	}
}
