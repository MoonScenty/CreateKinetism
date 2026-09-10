package me.moonscenty.createkinetism.content.centrifuge;

import java.util.List;

import com.simibubi.create.foundation.utility.CreateLang;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.common.capabilities.Capabilities;

import me.moonscenty.createkinetism.content.machine.BasinCarryingBlockEntity;
import me.moonscenty.createkinetism.content.recipe.CentrifugingRecipe;
import me.moonscenty.createkinetism.content.recipe.VatRecipe;
import me.moonscenty.createkinetism.foundation.CKChemicalTanks;
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

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import org.jetbrains.annotations.Nullable;

/**
 * Mekanism: Isotopic Centrifuge. Nuclear waste into plutonium.
 *
 * <p>Carries its own basin like the Dissolution Chamber, and looks the same. Where that one tips the
 * table a few degrees to keep acid moving over ore, this one swings it flat about the vertical: a
 * quarter turn one way, a quarter turn back, over and over.</p>
 *
 * <p>Nothing is held in that basin any more. Mekanism's centrifuge takes a gas and gives a gas back
 * with no item slots anywhere, so both halves are the machine's own chemical tanks - see
 * {@link CentrifugingRecipe}. What the basin is now is the rotor: the thing that swings, and the
 * part the machine will not run without.</p>
 */
public class IsotopicCentrifugeBlockEntity extends BasinCarryingBlockEntity
	implements IMekanismChemicalHandler {

	/** How far the table turns from centre, in degrees. A quarter turn each way. */
	public static final float SWING_ANGLE = 90f;

	/** Four buckets each way. Ten mB of waste buys one of plutonium, so the inlet is the busy side. */
	public static final long CAPACITY = 4000;

	/**
	 * Fills from outside, empties only into a recipe.
	 *
	 * <p>Radioactive-friendly: nuclear waste and uranium hexafluoride are the only two things this
	 * machine takes, and an ordinary input tank turns both away - see {@link CKChemicalTanks}.</p>
	 */
	public final IChemicalTank inputGasTank = CKChemicalTanks.radioactiveInput(CAPACITY, this);

	/** Fills only from a recipe, empties to whatever is pulling on it. */
	public final IChemicalTank outputGasTank = BasicChemicalTank.output(CAPACITY, this);

	private final List<IChemicalTank> tanks = List.of(inputGasTank, outputGasTank);

	public IsotopicCentrifugeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	protected CKRecipeTypes getRecipeType() {
		return CKRecipeTypes.CENTRIFUGING;
	}

	/**
	 * Both, on every side. Which way each one goes is the tank's own business, so a tube offered the
	 * pair still cannot put plutonium back in or siphon the waste out.
	 */
	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		return tanks;
	}

	/**
	 * The tanks' listener as well as the capability's.
	 *
	 * <p>{@code contentsChanged} is how this machine knows to go looking for work. With no items and
	 * no fluids left, a tube filling the inlet is the only thing that ever will.</p>
	 */
	@Override
	public void onContentsChanged() {
		setChanged();
		contentsChanged = true;
	}

	public static void registerChemicalCapability(RegisterCapabilitiesEvent event,
		BlockEntityType<IsotopicCentrifugeBlockEntity> type) {
		event.registerBlockEntity(Capabilities.CHEMICAL.block(), type,
			(be, context) -> new SidedChemicalAccess(be, context));
	}

	/**
	 * Gas in, gas out, and nothing in between.
	 *
	 * <p>Both halves have to agree before anything is spent, and {@code super.apply} is asked last -
	 * the same order the Dissolution Chamber uses, so a recipe whose product has nowhere to go never
	 * drains the inlet.</p>
	 */
	@Override
	protected boolean apply(VatRecipe recipe, boolean simulate) {
		if (!(recipe instanceof CentrifugingRecipe centrifuging))
			return super.apply(recipe, simulate);

		if (!centrifuging.matchesChemical(inputGasTank.getStack()))
			return false;
		ChemicalStack yield = centrifuging.getChemicalOutput();
		if (!outputGasTank.insert(yield, Action.SIMULATE, AutomationType.INTERNAL)
			.isEmpty())
			return false;
		if (!super.apply(recipe, simulate))
			return false;

		if (!simulate) {
			inputGasTank.extract(centrifuging.getRequiredAmount(), Action.EXECUTE,
				AutomationType.INTERNAL);
			outputGasTank.insert(yield, Action.EXECUTE, AutomationType.INTERNAL);
		}
		return true;
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.put("InputTank", inputGasTank.serializeNBT(registries));
		compound.put("OutputTank", outputGasTank.serializeNBT(registries));
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		if (compound.contains("InputTank"))
			inputGasTank.deserializeNBT(registries, compound.getCompound("InputTank"));
		if (compound.contains("OutputTank"))
			outputGasTank.deserializeNBT(registries, compound.getCompound("OutputTank"));
		super.read(compound, registries, clientPacket);
	}

	/** Neither tank has a window, so the goggles are the only way to read them. */
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		added |= describe(tooltip, inputGasTank);
		added |= describe(tooltip, outputGasTank);
		return added;
	}

	private static boolean describe(List<Component> tooltip, IChemicalTank tank) {
		ChemicalStack held = tank.getStack();
		if (held.isEmpty())
			return false;
		CreateLang.text("")
			.add(Component.translatable(held.getChemical()
				.getTranslationKey()))
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip);
		CreateLang.number(held.getAmount())
			.add(CreateLang.text(" / "))
			.add(CreateLang.number(CAPACITY))
			.add(CreateLang.text("mB"))
			.style(ChatFormatting.GOLD)
			.forGoggles(tooltip, 1);
		return true;
	}

	/**
	 * The table's heading, in degrees, between -90 and +90.
	 *
	 * <p>Not a sine. A sine is always turning, and this has to look like something that starts from a
	 * standstill, winds up, and is caught at the far end - so the phase is a triangle wave run through
	 * smoothstep, which is zero velocity at both ends and fastest through the middle. Faster
	 * rotation only shortens the cycle; the shape of the swing stays the same.</p>
	 */
	public float getSwingAngle(float renderTime) {
		if (!running)
			return 0;
		float frequency = 0.010f + Math.min(Math.abs(getSpeed()) / 128f, 1f) * 0.020f;
		return swingAngle(renderTime * frequency);
	}

	/**
	 * The curve itself, given a position in cycles. Public and static so the JEI panel swings on
	 * exactly the same shape as the block does in world - the panel is where a player learns to tell
	 * this machine from the Dissolution Chamber, so the two must not drift apart.
	 */
	public static float swingAngle(float cycles) {
		float cycle = cycles % 2f;
		float triangle = cycle < 1 ? cycle : 2 - cycle;
		float eased = triangle * triangle * (3 - 2 * triangle);
		return (eased * 2 - 1) * SWING_ANGLE;
	}
}
