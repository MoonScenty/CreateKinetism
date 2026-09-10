package me.moonscenty.createkinetism.content.crystallization;

import java.util.List;

import com.simibubi.create.foundation.utility.CreateLang;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.common.capabilities.Capabilities;

import me.moonscenty.createkinetism.content.recipe.CrystallizingRecipe;
import me.moonscenty.createkinetism.content.vat.VatBlockEntity;
import me.moonscenty.createkinetism.foundation.SidedChemicalAccess;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import org.jetbrains.annotations.Nullable;

/**
 * A vat wearing the Injection Chamber's housing, fed by tube instead of by basin.
 *
 * <p>Every other machine that took a chemical tank kept something item- or fluid-shaped going into
 * its basin. This one's only input is the clean slurry, so once that moved into the block the basin
 * had nothing left to hold on the way in. Rather than take the basin away, it became output-only:
 * the slurry arrives through a Mekanism tube into {@link #chemicalTank}, and the basin underneath
 * catches the crystals.</p>
 *
 * <p>Create's basin machinery turns out to allow that without a fight. {@code BasinRecipe.apply}
 * walks the recipe's item and fluid ingredients and there simply are none, so it falls through to
 * the part that checks whether the results fit - which is exactly the question worth asking here.
 * What it does not do is notice a tank filling up, so {@link #onContentsChanged()} pokes the basin
 * checker itself.</p>
 */
public class CrystallizationChamberBlockEntity extends VatBlockEntity
	implements IMekanismChemicalHandler {

	/** How far past the vat's own 7px rest position the head plunges at the peak of a cycle. */
	private static final float PLUNGE_AMPLITUDE = 10 / 16f;

	/** Slurry held. One batch out of the washer is 200mB, and one crystal costs that. */
	public static final long CAPACITY = 4000;

	public final IChemicalTank chemicalTank =
		BasicChemicalTank.input(CAPACITY, chemical -> true, this);

	private final List<IChemicalTank> tanks = List.of(chemicalTank);

	public CrystallizationChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/**
	 * The tank's listener as well as the capability's.
	 *
	 * <p>The basin checker only wakes for the basin, and a tube filling this tank is not that. Without
	 * the nudge a chamber sitting over an empty basin would take the slurry and then do nothing until
	 * something else happened to disturb the basin.</p>
	 */
	@Override
	public void onContentsChanged() {
		setChanged();
		if (basinChecker != null)
			basinChecker.scheduleUpdate();
	}

	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		return tanks;
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<CrystallizationChamberBlockEntity> type) {
		event.registerBlockEntity(Capabilities.CHEMICAL.block(), type,
			(be, context) -> new SidedChemicalAccess(be, context));
	}

	public ChemicalStack getStoredChemical() {
		return chemicalTank.getStack();
	}

	/**
	 * The basin half is Create's and asks only whether the crystal fits; the slurry half is ours.
	 * Both have to be satisfied, or the head never comes down.
	 */
	@Override
	protected <I extends RecipeInput> boolean matchBasinRecipe(Recipe<I> recipe) {
		if (!(recipe instanceof CrystallizingRecipe crystallizing))
			return false;
		if (!crystallizing.matchesChemical(getStoredChemical()))
			return false;
		return super.matchBasinRecipe(recipe);
	}

	/** The recipe's slurry cost, drained as the crystal is put in the basin. */
	@Override
	protected void applyBasinRecipe() {
		super.applyBasinRecipe();
		if (!(currentRecipe instanceof CrystallizingRecipe crystallizing))
			return;
		chemicalTank.extract(crystallizing.getRequiredAmount(), Action.EXECUTE, AutomationType.INTERNAL);
	}

	/**
	 * The vat's own curve, rescaled the way the Injection Chamber rescales it: the mixer head swings
	 * almost a full block to press into the basin, but this housing only needs to read as "the plunger
	 * moved" - ten pixels rather than sixteen.
	 */
	@Override
	public float getRenderedHeadOffset(float partialTicks) {
		float hump = super.getRenderedHeadOffset(partialTicks) - 7 / 16f;
		return 7 / 16f + hump * PLUNGE_AMPLITUDE;
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

	/** The tank has no window, so the goggles are the only way to read it. */
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
