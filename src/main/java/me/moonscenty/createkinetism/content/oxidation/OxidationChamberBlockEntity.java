package me.moonscenty.createkinetism.content.oxidation;

import java.util.List;

import com.simibubi.create.foundation.utility.CreateLang;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.common.capabilities.Capabilities;

import me.moonscenty.createkinetism.content.recipe.OxidizingRecipe;
import me.moonscenty.createkinetism.content.vat.VatBlockEntity;

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
 * A vat wearing the Injection Chamber's housing, with the gas leaving through the block.
 *
 * <p>The Crystallization Chamber turned inside out. There the only input was a chemical, so its
 * basin could only catch the output; here the only output is a chemical, so the basin can only be
 * the input. The item goes in the basin as it always did, and a Mekanism tube draws the gas out of
 * {@link #chemicalTank}.</p>
 *
 * <p>What the basin no longer does is hold the gas. It used to, back when this mod registered its
 * own fluid for every chemical - and that was always the weaker reading, because a basin is an open
 * bowl and the thing in it was a gas.</p>
 */
public class OxidationChamberBlockEntity extends VatBlockEntity implements IMekanismChemicalHandler {

	/** How far past the vat's own 7px rest position the head plunges at the peak of a cycle. */
	private static final float PLUNGE_AMPLITUDE = 10 / 16f;

	/** Gas held. Two buckets covers the dearest recipe, which is fissile fuel at 2000mB. */
	public static final long CAPACITY = 4000;

	/** Fills only from a recipe, empties to whatever is pulling on it. */
	public final IChemicalTank chemicalTank = BasicChemicalTank.output(CAPACITY, this);

	private final List<IChemicalTank> tanks = List.of(chemicalTank);

	public OxidationChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/**
	 * The tank's listener as well as the capability's.
	 *
	 * <p>The basin checker only wakes for the basin. A chamber that stalled with a full tank has to
	 * be told when a tube drains it, or it would sit there with ore in the basin and room to work.</p>
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
		BlockEntityType<OxidationChamberBlockEntity> type) {
		event.registerBlockEntity(Capabilities.CHEMICAL.block(), type, (be, context) -> be);
	}

	public ChemicalStack getStoredChemical() {
		return chemicalTank.getStack();
	}

	/**
	 * The basin half is Create's and finds the item; ours is whether the gas has anywhere to go.
	 * Checked before the item is committed, so a full tank stalls the machine rather than eating ore.
	 */
	@Override
	protected <I extends RecipeInput> boolean matchBasinRecipe(Recipe<I> recipe) {
		if (!(recipe instanceof OxidizingRecipe oxidizing))
			return false;
		if (!chemicalTank.insert(oxidizing.getChemicalOutput(), Action.SIMULATE, AutomationType.INTERNAL)
			.isEmpty())
			return false;
		return super.matchBasinRecipe(recipe);
	}

	/** The gas, put in the tank as the basin's item is taken. */
	@Override
	protected void applyBasinRecipe() {
		super.applyBasinRecipe();
		if (!(currentRecipe instanceof OxidizingRecipe oxidizing))
			return;
		chemicalTank.insert(oxidizing.getChemicalOutput(), Action.EXECUTE, AutomationType.INTERNAL);
	}

	/**
	 * The vat's own curve, rescaled exactly as the Injection Chamber rescales it: the mixer head
	 * swings almost a full block to press into the basin, but this housing only needs to read as
	 * "the plunger moved" - ten pixels rather than sixteen.
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
