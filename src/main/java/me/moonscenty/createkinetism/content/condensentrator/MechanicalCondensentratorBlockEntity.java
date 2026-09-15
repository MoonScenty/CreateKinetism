package me.moonscenty.createkinetism.content.condensentrator;

import java.util.List;
import java.util.Optional;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.foundation.utility.CreateLang;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.api.chemical.attribute.ChemicalAttributeValidator;
import mekanism.api.functions.ConstantPredicates;
import mekanism.api.recipes.RotaryRecipe;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.recipe.MekanismRecipeType;

import me.moonscenty.createkinetism.content.heat.CKHeatLevels;
import me.moonscenty.createkinetism.content.machine.BasinCarryingBlockEntity;
import me.moonscenty.createkinetism.content.recipe.VatRecipe;
import me.moonscenty.createkinetism.foundation.SidedChemicalAccess;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import org.jetbrains.annotations.Nullable;

/**
 * Mekanism: Rotary Condensentrator. Gas to liquid and liquid to gas, running Mekanism's own
 * {@code mekanism:rotary} recipes.
 *
 * <p>Mekanism picks the direction with a mode button. Here the heater decides, from under the basin
 * the machine carries - two blocks down, past the gap placement keeps free (see
 * {@link CondensentratorPlacement}):</p>
 * <ul>
 *   <li>Chilled - condensentrating: the gas tank's contents become liquid in the fluid tank.</li>
 *   <li>Heated or hotter - decondensentrating: the fluid tank's contents become gas.</li>
 * </ul>
 * <p>Anything else, and nothing happens.</p>
 *
 * <p>One fluid tank and one gas tank, both {@value #CAPACITY} mB like Mekanism's, and both open both
 * ways on every side: whichever way the machine is running, one of them is the input and the other the
 * output, and pipes can fill or drain either. Each only takes what some rotary recipe starts from.
 * The item inventories and paired basin tanks this inherits go unused.</p>
 *
 * <p>Continuous rather than batched, the way Mekanism runs it: every tick, one recipe per
 * {@value #RPM_PER_OPERATION} RPM (at least one), as far as the input and the room in the output
 * allow. It runs only with a basin fitted and the shaft turning.</p>
 */
public class MechanicalCondensentratorBlockEntity extends BasinCarryingBlockEntity
	implements IMekanismChemicalHandler {

	/** Each tank, in mB - the Rotary Condensentrator's ten buckets. */
	public static final int CAPACITY = 10_000;

	/** Shaft speed that buys one more recipe a tick. */
	public static final int RPM_PER_OPERATION = 8;

	/** Liquid side: fills or empties from outside, whichever way the machine runs. */
	public SmartFluidTankBehaviour fluidTank;
	private IFluidHandler fluidCapability;

	/** Gas side, the same. Radioactive gases allowed - uranium hexafluoride has a rotary recipe. */
	public final IChemicalTank chemicalTank = BasicChemicalTank.createModern(CAPACITY,
		ConstantPredicates.alwaysTrue(), ConstantPredicates.alwaysTrue(), this::isValidChemical,
		ChemicalAttributeValidator.ALWAYS_ALLOW, this);

	private final List<IChemicalTank> chemicalTanks = List.of(chemicalTank);

	/** Whether a recipe ran this tick - what spins the table. Synced. */
	private boolean converting;
	private boolean chemicalDirty;

	public MechanicalCondensentratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		fluidTank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.TYPE, this, 1, CAPACITY, false);
		behaviours.add(fluidTank);
		fluidCapability = new RotaryFluidHandler(fluidTank.getCapability());
	}

	/** Not one of this mod's recipe types - the work is done in {@link #tick} against Mekanism's. */
	@Override
	protected CKRecipeTypes getRecipeType() {
		throw new UnsupportedOperationException("The Mechanical Condensentrator runs Mekanism's rotary recipes");
	}

	@Override
	protected Optional<VatRecipe> findRecipe() {
		return Optional.empty();
	}

	// ------------------------------------------------------------------ processing

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;

		boolean ran = false;
		if (hasBasin() && getSpeed() != 0) {
			int operations = Math.max(1, (int) Math.abs(getSpeed()) / RPM_PER_OPERATION);
			HeatLevel heat = BasinBlockEntity.getHeatLevelOf(level.getBlockState(worldPosition.below(2)));
			if (heat == CKHeatLevels.CHILLED)
				ran = condensentrate(operations);
			else if (HeatCondition.HEATED.testBlazeBurner(heat))
				ran = decondensentrate(operations);
		}

		if (ran != converting) {
			converting = ran;
			sendData();
		}
	}

	/** Gas tank to fluid tank. */
	private boolean condensentrate(int operations) {
		ChemicalStack held = chemicalTank.getStack();
		RotaryRecipe recipe = MekanismRecipeType.ROTARY.getInputCache()
			.findFirstRecipe(level, held);
		if (recipe == null || !recipe.hasChemicalToFluid())
			return false;

		ChemicalStack perRun = recipe.getChemicalInput()
			.getMatchingInstance(held);
		if (perRun.isEmpty())
			return false;
		FluidStack product = recipe.getFluidOutput(perRun);
		if (product.isEmpty())
			return false;

		SmartFluidTank tank = fluidTank.getPrimaryHandler();
		long runs = Math.min(operations, held.getAmount() / perRun.getAmount());
		int room = tank.fill(product.copyWithAmount(CAPACITY), FluidAction.SIMULATE);
		runs = Math.min(runs, room / product.getAmount());
		if (runs <= 0)
			return false;

		chemicalTank.extract(perRun.getAmount() * runs, Action.EXECUTE, AutomationType.INTERNAL);
		tank.fill(product.copyWithAmount((int) (product.getAmount() * runs)), FluidAction.EXECUTE);
		return true;
	}

	/** Fluid tank to gas tank. */
	private boolean decondensentrate(int operations) {
		SmartFluidTank tank = fluidTank.getPrimaryHandler();
		FluidStack held = tank.getFluid();
		RotaryRecipe recipe = MekanismRecipeType.ROTARY.getInputCache()
			.findFirstRecipe(level, held);
		if (recipe == null || !recipe.hasFluidToChemical())
			return false;

		FluidStack perRun = recipe.getFluidInput()
			.getMatchingInstance(held);
		if (perRun.isEmpty())
			return false;
		ChemicalStack product = recipe.getChemicalOutput(perRun);
		if (product.isEmpty())
			return false;

		long runs = Math.min(operations, held.getAmount() / perRun.getAmount());
		ChemicalStack remainder = chemicalTank.insert(product.copyWithAmount(CAPACITY), Action.SIMULATE,
			AutomationType.INTERNAL);
		long room = CAPACITY - remainder.getAmount();
		runs = Math.min(runs, room / product.getAmount());
		if (runs <= 0)
			return false;

		tank.drain(perRun.getAmount() * (int) runs, FluidAction.EXECUTE);
		chemicalTank.insert(product.copyWithAmount(product.getAmount() * runs), Action.EXECUTE,
			AutomationType.INTERNAL);
		return true;
	}

	/** The table spins while this is true - see {@link #getSpinAngle}. */
	public boolean isConverting() {
		return converting;
	}

	/**
	 * How far the table has turned about the vertical, in degrees, while a recipe runs. It keeps going
	 * one way - clockwise seen from above, which about Minecraft's up axis is a negative angle -
	 * whichever way the shaft turns, at Create's own rate for a part at the shaft's speed (0.3 degrees
	 * a tick per RPM). Stopped, the table sits square.
	 */
	public float getSpinAngle(float renderTime) {
		if (!converting)
			return 0;
		return -(renderTime * Math.abs(getSpeed()) * 0.3f % 360);
	}

	// ------------------------------------------------------------------ tanks

	private boolean isValidChemical(ChemicalStack stack) {
		return level == null || MekanismRecipeType.ROTARY.getInputCache()
			.containsInput(level, stack);
	}

	private boolean isValidFluid(FluidStack stack) {
		return level == null || MekanismRecipeType.ROTARY.getInputCache()
			.containsInput(level, stack);
	}

	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		return chemicalTanks;
	}

	/** Synced in batches from {@link #lazyTick} - a tube moves gas every tick. */
	@Override
	public void onContentsChanged() {
		setChanged();
		chemicalDirty = true;
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (chemicalDirty && level != null && !level.isClientSide) {
			chemicalDirty = false;
			sendData();
		}
	}

	public static void registerRotaryCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<MechanicalCondensentratorBlockEntity> type) {
		event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, type,
			(be, side) -> be.fluidCapability);
		event.registerBlockEntity(Capabilities.CHEMICAL.block(), type,
			(be, side) -> new SidedChemicalAccess(be, side));
	}

	/** The fluid tank as pipes see it: both ways, but only fluids a rotary recipe starts from. */
	private class RotaryFluidHandler implements IFluidHandler {

		private final IFluidHandler tank;

		RotaryFluidHandler(IFluidHandler tank) {
			this.tank = tank;
		}

		@Override
		public int getTanks() {
			return tank.getTanks();
		}

		@Override
		public FluidStack getFluidInTank(int index) {
			return tank.getFluidInTank(index);
		}

		@Override
		public int getTankCapacity(int index) {
			return tank.getTankCapacity(index);
		}

		@Override
		public boolean isFluidValid(int index, FluidStack stack) {
			return isValidFluid(stack) && tank.isFluidValid(index, stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return isValidFluid(resource) ? tank.fill(resource, action) : 0;
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return tank.drain(resource, action);
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return tank.drain(maxDrain, action);
		}
	}

	// ------------------------------------------------------------------ persistence and display

	@Override
	protected AABB createRenderBoundingBox() {
		// The basin hangs a block below us.
		return new AABB(worldPosition).expandTowards(0, -1.5, 0);
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.put("ChemicalTank", chemicalTank.serializeNBT(registries));
		if (clientPacket)
			compound.putBoolean("Converting", converting);
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		if (compound.contains("ChemicalTank"))
			chemicalTank.deserializeNBT(registries, compound.getCompound("ChemicalTank"));
		if (clientPacket)
			converting = compound.getBoolean("Converting");
		super.read(compound, registries, clientPacket);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

		FluidStack fluid = fluidTank.getPrimaryHandler()
			.getFluid();
		if (!fluid.isEmpty()) {
			CreateLang.text("")
				.add(CreateLang.fluidName(fluid))
				.style(ChatFormatting.GRAY)
				.forGoggles(tooltip);
			amount(fluid.getAmount()).forGoggles(tooltip, 1);
			added = true;
		}

		ChemicalStack chemical = chemicalTank.getStack();
		if (!chemical.isEmpty()) {
			CreateLang.text("")
				.add(Component.translatable(chemical.getChemical()
					.getTranslationKey()))
				.style(ChatFormatting.GRAY)
				.forGoggles(tooltip);
			amount(chemical.getAmount()).forGoggles(tooltip, 1);
			added = true;
		}
		return added;
	}

	private static net.createmod.catnip.lang.LangBuilder amount(long amount) {
		return CreateLang.number(amount)
			.add(CreateLang.text(" / "))
			.add(CreateLang.number(CAPACITY))
			.add(CreateLang.text("mB"))
			.style(ChatFormatting.GOLD);
	}
}
