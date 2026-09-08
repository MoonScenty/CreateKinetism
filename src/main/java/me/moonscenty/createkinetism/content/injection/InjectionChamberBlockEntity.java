package me.moonscenty.createkinetism.content.injection;

import java.util.List;

import com.simibubi.create.content.processing.basin.BasinRecipe;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.common.capabilities.Capabilities;

import me.moonscenty.createkinetism.content.recipe.InjectingRecipe;
import me.moonscenty.createkinetism.content.vat.VatBlockEntity;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import org.jetbrains.annotations.Nullable;

/**
 * A vat that also holds a chemical tank of its own.
 *
 * <p>Mekanism's Chemical Injection Chamber takes an item and a chemical. The basin under this block
 * is the item half, matching every other vat; this tank is the chemical half, checked and consumed on
 * either side of the basin match the same way the Combiner's held infusion item is -
 * {@link BasinRecipe#match} only knows about the basin, so it has no way to see a requirement that
 * lives somewhere else.</p>
 *
 * <p>A Mekanism {@link IChemicalTank}, not a Create fluid tank. Most of Mekanism's injection recipes
 * want water vapour, which has no fluid form at all, so a Create pipe could never have carried them;
 * the chamber takes a pressurized tube instead. Hydrogen chloride, the one the ore chain uses, does
 * have a fluid form - but a machine that accepted its gas two different ways depending on which
 * recipe was running would be worse than one that always wants the same connection.</p>
 */
public class InjectionChamberBlockEntity extends VatBlockEntity implements IMekanismChemicalHandler {

	/** How far past the vat's own 7px rest position the head plunges at the peak of a cycle. */
	private static final float PLUNGE_AMPLITUDE = 10 / 16f;

	/** Millibuckets of chemical the chamber holds. */
	public static final long CAPACITY = 1000;

	public final IChemicalTank chemicalTank = BasicChemicalTank.createAllValid(CAPACITY, this);

	/** One tank, offered on every side - see {@link #getChemicalTanks}. */
	private final List<IChemicalTank> tanks = List.of(chemicalTank);

	/**
	 * How full the tank looks, chasing how full it is. Create's own tank behaviour smoothed this for
	 * us; the Mekanism tank has no idea it is being drawn, so the easing lives here.
	 */
	public final LerpedFloat fillLevel = LerpedFloat.linear()
		.startWithValue(0)
		.chase(0, .25f, Chaser.EXP);

	public InjectionChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/**
	 * The tank's listener as well as the capability's.
	 *
	 * <p>The basin checker only wakes for the basin, and a tube filling this tank is not that. Without
	 * the nudge a machine whose basin was already loaded would sit still until something happened to
	 * disturb the basin again.</p>
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
		BlockEntityType<InjectionChamberBlockEntity> type) {
		event.registerBlockEntity(Capabilities.CHEMICAL.block(), type, (be, context) -> be);
	}

	public ChemicalStack getStoredChemical() {
		return chemicalTank.getStack();
	}

	public float getFillFraction() {
		return chemicalTank.isEmpty() ? 0 : chemicalTank.getStored() / (float) CAPACITY;
	}

	@Override
	public void tick() {
		super.tick();
		if (level != null && level.isClientSide) {
			fillLevel.chase(getFillFraction(), .25f, Chaser.EXP);
			fillLevel.tickChaser();
		}
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
	 * The basin half is Create's; the chemical half is ours. Both have to be satisfied, or the head
	 * never comes down - the behaviour you want when someone has filled the basin but not the tank.
	 */
	@Override
	protected <I extends RecipeInput> boolean matchBasinRecipe(Recipe<I> recipe) {
		if (!(recipe instanceof InjectingRecipe injecting))
			return false;
		if (!injecting.matchesChemical(getStoredChemical()))
			return false;
		return getBasin().map(basin -> BasinRecipe.match(basin, recipe))
			.orElse(false);
	}

	/** The recipe's chemical cost, drained from our own tank as the basin half is applied. */
	@Override
	protected void applyBasinRecipe() {
		super.applyBasinRecipe();
		if (!(currentRecipe instanceof InjectingRecipe injecting))
			return;
		chemicalTank.extract(injecting.getRequiredAmount(), Action.EXECUTE, AutomationType.INTERNAL);
	}

	@Override
	protected boolean matchStaticFilters(RecipeHolder<? extends Recipe<?>> recipe) {
		return recipe.value() instanceof InjectingRecipe;
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
}
