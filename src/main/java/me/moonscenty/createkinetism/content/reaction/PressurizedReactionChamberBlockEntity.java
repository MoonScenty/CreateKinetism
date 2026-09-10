package me.moonscenty.createkinetism.content.reaction;

import java.util.List;
import java.util.Optional;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.common.capabilities.Capabilities;

import me.moonscenty.createkinetism.content.recipe.ReactingRecipe;
import me.moonscenty.createkinetism.content.vat.VatBlockEntity;
import me.moonscenty.createkinetism.foundation.CKLang;
import me.moonscenty.createkinetism.foundation.SidedChemicalAccess;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import org.jetbrains.annotations.Nullable;

/**
 * A vat that stands on its basin and holds the reaction's gases itself.
 *
 * <p>Mekanism's Pressurized Reaction Chamber takes an item, a fluid and a gas and gives back an item
 * and a gas. The three go where they can: the <b>basin</b> underneath holds the liquid and the item
 * and takes the item back, and the chamber keeps <b>two gas tanks</b> - one the reaction draws from,
 * one it fills. A basin cannot hold a Mekanism chemical, so the gases were never going to live down
 * there.</p>
 *
 * <p>It also looks one block down rather than two, because it is sitting on the thing it works.</p>
 */
public class PressurizedReactionChamberBlockEntity extends VatBlockEntity
	implements IMekanismChemicalHandler, IHaveGoggleInformation {

	/** One bucket each way, matching the other machines here that carry their own tank. */
	public static final long CAPACITY = 1000;

	/** What the reaction draws from. A tube fills it; nothing pulls back out. */
	public final IChemicalTank inputGasTank = BasicChemicalTank.input(CAPACITY, chemical -> true, this);

	/** What the reaction fills. The other way round: only a tube empties it. */
	public final IChemicalTank outputGasTank = BasicChemicalTank.output(CAPACITY, this);

	private final List<IChemicalTank> tanks = List.of(inputGasTank, outputGasTank);

	public PressurizedReactionChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/**
	 * Both tanks on every face.
	 *
	 * <p>Which one a tube reaches is decided by the tanks themselves rather than by geometry - the
	 * inlet refuses to be drained and the outlet refuses to be filled - so there is nothing to gain
	 * by making the player find the right side of a machine that is already sitting on a basin.</p>
	 */
	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		return tanks;
	}

	/**
	 * Gas arriving is a reason to look for work again, the way a basin change is.
	 *
	 * <p>It is also the only word the client gets about the tanks while the chamber is idle - without
	 * it, gas piped into a chamber with nothing to do stays invisible to a pair of goggles.</p>
	 */
	@Override
	public void onContentsChanged() {
		notifyUpdate();
		basinChecker.scheduleUpdate();
	}

	/**
	 * Bound to the face it was asked for - see {@link SidedChemicalAccess}.
	 *
	 * <p>The tanks refuse the wrong traffic themselves, but only when they are told the traffic is
	 * coming from outside. A machine handed out sideless counts as its own hands, and then the inlet's
	 * refusal to be drained and the outlet's refusal to be filled both stop meaning anything.</p>
	 */
	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<PressurizedReactionChamberBlockEntity> type) {
		event.registerBlockEntity(Capabilities.CHEMICAL.block(), type,
			(be, context) -> new SidedChemicalAccess(be, context));
	}

	/** The block it stands on, not the one two below - this machine has no gap under it. */
	@Override
	protected Optional<BasinBlockEntity> getBasin() {
		if (level == null)
			return Optional.empty();
		BlockEntity basin = level.getBlockEntity(worldPosition.below());
		return basin instanceof BasinBlockEntity found ? Optional.of(found) : Optional.empty();
	}

	/** The block does not carry the recipe type, so say it here. */
	@Override
	public CKRecipeTypes getRecipeType() {
		return CKRecipeTypes.REACTING;
	}

	/**
	 * Three conditions, and all of them have to hold.
	 *
	 * <p>The inlet has the gas the reaction wants, the outlet has room for what it makes, and the
	 * basin has the liquid and the item. Any one missing and the chamber simply keeps turning without
	 * running - which is the behaviour you want when someone has filled one side and not the other.
	 * There is nothing to watch for: the shaft through the chamber turns whether it is working or
	 * not.</p>
	 */
	@Override
	protected <I extends RecipeInput> boolean matchBasinRecipe(Recipe<I> recipe) {
		if (!(recipe instanceof ReactingRecipe reacting))
			return false;
		if (!reacting.matchesChemical(inputGasTank.getStack()))
			return false;
		if (!outputHasRoom(reacting))
			return false;
		return getBasin().map(basin -> BasinRecipe.match(basin, recipe))
			.orElse(false);
	}

	/** A reaction that makes no gas needs no room; one that does needs all of it at once. */
	private boolean outputHasRoom(ReactingRecipe reacting) {
		return reacting.getChemicalOutput()
			.map(made -> outputGasTank.insert(made, Action.SIMULATE, AutomationType.INTERNAL)
				.isEmpty())
			.orElse(true);
	}

	/** The basin half is Create's; the two tanks are ours, spent and filled alongside it. */
	@Override
	protected void applyBasinRecipe() {
		super.applyBasinRecipe();
		if (!(currentRecipe instanceof ReactingRecipe reacting))
			return;
		inputGasTank.extract(reacting.getRequiredAmount(), Action.EXECUTE, AutomationType.INTERNAL);
		reacting.getChemicalOutput()
			.ifPresent(made -> outputGasTank.insert(made, Action.EXECUTE, AutomationType.INTERNAL));
	}

	@Override
	protected boolean matchStaticFilters(RecipeHolder<? extends Recipe<?>> recipe) {
		return recipe.value() instanceof ReactingRecipe;
	}

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
		CKLang.builder()
			.add(Component.translatable(held.getChemical()
				.getTranslationKey()))
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip);
		CKLang.builder()
			.text(held.getAmount() + " / " + CAPACITY + "mB")
			.style(ChatFormatting.GOLD)
			.forGoggles(tooltip, 1);
		return true;
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.put("InputGas", inputGasTank.serializeNBT(registries));
		compound.put("OutputGas", outputGasTank.serializeNBT(registries));
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		if (compound.contains("InputGas"))
			inputGasTank.deserializeNBT(registries, compound.getCompound("InputGas"));
		if (compound.contains("OutputGas"))
			outputGasTank.deserializeNBT(registries, compound.getCompound("OutputGas"));
		super.read(compound, registries, clientPacket);
	}
}
