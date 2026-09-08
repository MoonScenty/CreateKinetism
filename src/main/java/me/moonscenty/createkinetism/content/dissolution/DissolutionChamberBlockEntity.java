package me.moonscenty.createkinetism.content.dissolution;

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
import me.moonscenty.createkinetism.content.recipe.DissolvingRecipe;
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
 * Mekanism: Chemical Dissolution Chamber. Raw ore plus sulfuric acid, the first 5x step.
 *
 * <p>Carries its own basin like the Purification Vibrator - see {@link BasinCarryingBlockEntity} -
 * but where that one shakes the table straight up and down, this one rocks it. Dissolving is a slow
 * swirl, not a tremor: the table tips one way, holds, and tips back, the way a lab rocker keeps acid
 * moving over ore without splashing it.</p>
 *
 * <p>The only machine here with a chemical on both sides, so it has two tanks rather than one. The
 * acid goes in the {@link #acidTank}, the slurry comes out of the {@link #slurryTank}, and each
 * refuses the other's direction from outside - a pressurized tube can fill one and drain the other
 * but never the reverse. Slurries exist only as Mekanism chemicals, with no fluid form at all, which
 * is why this machine had nothing to run between the two changes that got it here.</p>
 */
public class DissolutionChamberBlockEntity extends BasinCarryingBlockEntity
	implements IMekanismChemicalHandler {

	/** How far the table tips from level, in degrees. */
	public static final float ROCK_ANGLE = 7f;

	/** Acid held. Four buckets covers the dearest recipe, which is fluorite at nine per tick. */
	public static final long INPUT_CAPACITY = 4000;

	/** Slurry held. A raw ore block dissolves into six buckets of it in one go. */
	public static final long OUTPUT_CAPACITY = 10000;

	// Named for what they hold rather than which way they go: the base class already has an
	// inputTank, and that one is for fluids.
	/** Fills from outside, empties only into a recipe. */
	public final IChemicalTank acidTank =
		BasicChemicalTank.input(INPUT_CAPACITY, chemical -> true, this);

	/** Fills only from a recipe, empties to whatever is pulling on it. */
	public final IChemicalTank slurryTank = BasicChemicalTank.output(OUTPUT_CAPACITY, this);

	private final List<IChemicalTank> tanks = List.of(acidTank, slurryTank);

	public DissolutionChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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

	/**
	 * Both, on every side. Which way each one goes is the tank's own business - see the two
	 * {@code BasicChemicalTank} factories above - so a tube offered the pair still cannot put slurry
	 * back in or siphon the acid out.
	 */
	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		return tanks;
	}

	public static void registerChemicalCapability(RegisterCapabilitiesEvent event,
		BlockEntityType<DissolutionChamberBlockEntity> type) {
		event.registerBlockEntity(Capabilities.CHEMICAL.block(), type, (be, context) -> be);
	}

	@Override
	protected CKRecipeTypes getRecipeType() {
		return CKRecipeTypes.DISSOLVING;
	}

	/**
	 * Acid in, slurry out, items in between.
	 *
	 * <p>Both chemical halves are checked before {@code super.apply} touches anything, because it is
	 * the one that consumes the items - a recipe whose slurry has nowhere to go must not eat the ore
	 * first. It can still refuse afterwards, so nothing is drained or filled until it has agreed.</p>
	 */
	@Override
	protected boolean apply(VatRecipe recipe, boolean simulate) {
		if (!(recipe instanceof DissolvingRecipe dissolving))
			return super.apply(recipe, simulate);

		if (!dissolving.matchesChemical(acidTank.getStack()))
			return false;
		ChemicalStack yield = dissolving.getChemicalOutput();
		if (!slurryTank.insert(yield, Action.SIMULATE, AutomationType.INTERNAL)
			.isEmpty())
			return false;
		if (!super.apply(recipe, simulate))
			return false;

		if (!simulate) {
			acidTank.extract(dissolving.getRequiredAmount(), Action.EXECUTE, AutomationType.INTERNAL);
			slurryTank.insert(yield, Action.EXECUTE, AutomationType.INTERNAL);
		}
		return true;
	}

	/**
	 * The table's tilt, in degrees. Slower than the vibrator's tremor and eased at the ends of the
	 * stroke rather than sinusoidal throughout, so it reads as a deliberate rock rather than a wobble.
	 */
	public float getRockAngle(float renderTime) {
		if (!running)
			return 0;
		float frequency = 0.12f + Math.min(Math.abs(getSpeed()) / 128f, 1f) * 0.18f;
		float phase = Mth.sin(renderTime * frequency);
		// Cubing keeps the ends of the stroke slow and the middle quick - a rocker pauses at the tip.
		return phase * phase * phase * ROCK_ANGLE;
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.put("InputTank", acidTank.serializeNBT(registries));
		compound.put("OutputTank", slurryTank.serializeNBT(registries));
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		if (compound.contains("InputTank"))
			acidTank.deserializeNBT(registries, compound.getCompound("InputTank"));
		if (compound.contains("OutputTank"))
			slurryTank.deserializeNBT(registries, compound.getCompound("OutputTank"));
		super.read(compound, registries, clientPacket);
	}

	/** Neither tank has a window, so the goggles are the only way to read them. */
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		added |= describe(tooltip, acidTank, INPUT_CAPACITY);
		added |= describe(tooltip, slurryTank, OUTPUT_CAPACITY);
		return added;
	}

	private static boolean describe(List<Component> tooltip, IChemicalTank tank, long capacity) {
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
			.add(CreateLang.number(capacity))
			.add(CreateLang.text("mB"))
			.style(ChatFormatting.GOLD)
			.forGoggles(tooltip, 1);
		return true;
	}
}
