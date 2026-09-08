package me.moonscenty.createkinetism.content.vibrator;

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
import me.moonscenty.createkinetism.content.recipe.PurifyingRecipe;
import me.moonscenty.createkinetism.content.recipe.VatRecipe;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import org.jetbrains.annotations.Nullable;

/**
 * Mekanism: Purification Chamber. The 3x ore step.
 *
 * <p>Everything about carrying its own basin lives in {@link BasinCarryingBlockEntity}. What is left
 * here is the shake - a fast, shallow tremor rather than the slow press of a vat, because this
 * machine agitates the ore rather than squashing it - and the oxygen.</p>
 *
 * <p>The oxygen used to go in the basin as a fluid. It is a Mekanism {@link ChemicalStack} in a tank
 * of the machine's own now, which is where Mekanism keeps it: ore in the chamber, gas piped to the
 * chamber. Oxygen does have a fluid form and this could have stayed as it was; it did not, because
 * the Injection Chamber one step along the same chain has no such choice - water vapour has no fluid
 * at all - and one ore line that wants two different kinds of connection would be worse than one
 * that always wants a pressurized tube.</p>
 */
public class PurificationVibratorBlockEntity extends BasinCarryingBlockEntity
	implements IMekanismChemicalHandler {

	/** How far the head and basin travel from rest, in blocks. */
	public static final float SHAKE_AMPLITUDE = 1 / 16f;

	/** Millibuckets of gas the chamber holds. */
	public static final long CAPACITY = 1000;

	public final IChemicalTank chemicalTank = BasicChemicalTank.createAllValid(CAPACITY, this);

	/** One tank, offered on every side - see {@link #getChemicalTanks}. */
	private final List<IChemicalTank> tanks = List.of(chemicalTank);

	public PurificationVibratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/**
	 * The tank's listener as well as the capability's.
	 *
	 * <p>{@code contentsChanged} is how this machine knows to go looking for work, and the item
	 * inventory and fluid tanks all set it. A chemical tank filled by a tube has to as well, or a
	 * machine with everything else already in place would never start.</p>
	 */
	@Override
	public void onContentsChanged() {
		setChanged();
		contentsChanged = true;
	}

	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		return tanks;
	}

	/**
	 * Named apart from the base class's own registrar rather than overriding it - that one is generic
	 * over every processing machine and hands out the item and fluid handlers, which this machine still
	 * wants. Both get called; see {@code CKBlockEntityTypes}.
	 */
	public static void registerChemicalCapability(RegisterCapabilitiesEvent event,
		BlockEntityType<PurificationVibratorBlockEntity> type) {
		event.registerBlockEntity(Capabilities.CHEMICAL.block(), type, (be, context) -> be);
	}

	public ChemicalStack getStoredChemical() {
		return chemicalTank.getStack();
	}

	@Override
	protected CKRecipeTypes getRecipeType() {
		return CKRecipeTypes.PURIFYING;
	}

	/**
	 * The item half is the machine's own inventory, handled upstream; the gas half is this tank.
	 *
	 * <p>Checked before the items so a recipe that cannot be paid for never reserves anything, and
	 * drained only once the items have actually been taken - {@code super.apply} is what decides that,
	 * and it can still refuse after the chemical check has passed.</p>
	 */
	@Override
	protected boolean apply(VatRecipe recipe, boolean simulate) {
		if (recipe instanceof PurifyingRecipe purifying
			&& !purifying.matchesChemical(getStoredChemical()))
			return false;
		if (!super.apply(recipe, simulate))
			return false;
		if (!simulate && recipe instanceof PurifyingRecipe purifying)
			chemicalTank.extract(purifying.getRequiredAmount(), Action.EXECUTE, AutomationType.INTERNAL);
		return true;
	}

	/** How far the head and the basin have shaken from rest. */
	public float getShakeOffset(float renderTime) {
		if (!running)
			return 0;
		float frequency = 0.6f + Math.min(Math.abs(getSpeed()) / 128f, 1f) * 0.8f;
		return Mth.sin(renderTime * frequency) * SHAKE_AMPLITUDE;
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.put("ChemicalTank", chemicalTank.serializeNBT(registries));
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		if (compound.contains("ChemicalTank"))
			chemicalTank.deserializeNBT(registries, compound.getCompound("ChemicalTank"));
		super.read(compound, registries, clientPacket);
	}

	/** The gas has no window to show it through, so the goggles are the only way to read the tank. */
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

		ChemicalStack held = getStoredChemical();
		if (held.isEmpty())
			return added;

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
}
