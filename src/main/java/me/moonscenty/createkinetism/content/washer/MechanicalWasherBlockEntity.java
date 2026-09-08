package me.moonscenty.createkinetism.content.washer;

import java.util.List;

import com.simibubi.create.foundation.utility.CreateLang;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.common.capabilities.Capabilities;

import me.moonscenty.createkinetism.content.machine.ProcessingMachineBlockEntity;
import me.moonscenty.createkinetism.content.recipe.VatRecipe;
import me.moonscenty.createkinetism.content.recipe.WashingRecipe;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.createmod.catnip.animation.AnimationTickHolder;

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
 * Mekanism: Chemical Washer. Dirty slurry plus a great deal of water.
 *
 * <p>Unlike the vats this holds its own fluid rather than working out of a basin - it is a sealed
 * vessel with an auger down the middle, and the shaft comes in underneath where a basin would
 * otherwise sit. Everything but the auger's angle and the slurries comes from
 * {@link ProcessingMachineBlockEntity}.</p>
 *
 * <p>Three connections, then: water into the fluid tank through a Create pipe, dirty slurry into
 * {@link #dirtyTank} and clean out of {@link #cleanTank} through Mekanism tubes. The water is a real
 * fluid and the slurries are not - they exist only as Mekanism chemicals - so the machine genuinely
 * needs both kinds of pipe, which is the one place in this mod where that is true rather than a
 * choice.</p>
 */
public class MechanicalWasherBlockEntity extends ProcessingMachineBlockEntity
	implements IMekanismChemicalHandler {

	/** Slurry held on each side. One ore's worth of dirty slurry is 1000mB. */
	public static final long TANK_CAPACITY = 4000;

	// Named for what they hold rather than which way they go: the base class already has an
	// inputTank, and that one is for water.
	/** Fills from outside, empties only into a recipe. */
	public final IChemicalTank dirtyTank =
		BasicChemicalTank.input(TANK_CAPACITY, chemical -> true, this);

	/** Fills only from a recipe, empties to whatever is pulling on it. */
	public final IChemicalTank cleanTank = BasicChemicalTank.output(TANK_CAPACITY, this);

	private final List<IChemicalTank> tanks = List.of(dirtyTank, cleanTank);

	public MechanicalWasherBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
	 * {@code BasicChemicalTank} factories above - so a tube offered the pair still cannot put clean
	 * slurry back in or siphon the dirty out.
	 */
	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		return tanks;
	}

	public static void registerChemicalCapability(RegisterCapabilitiesEvent event,
		BlockEntityType<MechanicalWasherBlockEntity> type) {
		event.registerBlockEntity(Capabilities.CHEMICAL.block(), type, (be, context) -> be);
	}

	@Override
	protected CKRecipeTypes getRecipeType() {
		return CKRecipeTypes.WASHING;
	}

	/**
	 * Dirty in, clean out, water in between.
	 *
	 * <p>Both slurry halves are checked before {@code super.apply} touches anything, because that is
	 * what consumes the water - a batch whose clean slurry has nowhere to go must not drink first. It
	 * can still refuse afterwards, so nothing is drained or filled until it has agreed.</p>
	 */
	@Override
	protected boolean apply(VatRecipe recipe, boolean simulate) {
		if (!(recipe instanceof WashingRecipe washing))
			return super.apply(recipe, simulate);

		if (!washing.matchesChemical(dirtyTank.getStack()))
			return false;
		ChemicalStack yield = washing.getChemicalOutput();
		if (!cleanTank.insert(yield, Action.SIMULATE, AutomationType.INTERNAL)
			.isEmpty())
			return false;
		if (!super.apply(recipe, simulate))
			return false;

		if (!simulate) {
			dirtyTank.extract(washing.getRequiredAmount(), Action.EXECUTE, AutomationType.INTERNAL);
			cleanTank.insert(yield, Action.EXECUTE, AutomationType.INTERNAL);
		}
		return true;
	}

	/**
	 * The auger's angle. Driven straight off the shaft rather than off a work timer, so a washer with
	 * nothing in it still turns - the shaft is connected either way, and a stopped auger on a live
	 * network would read as a broken machine.
	 */
	public float getPropellerAngle(float partialTicks) {
		return AnimationTickHolder.getRenderTime(level) * getSpeed() * 3 / 10f % 360;
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.put("DirtyTank", dirtyTank.serializeNBT(registries));
		compound.put("CleanTank", cleanTank.serializeNBT(registries));
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		if (compound.contains("DirtyTank"))
			dirtyTank.deserializeNBT(registries, compound.getCompound("DirtyTank"));
		if (compound.contains("CleanTank"))
			cleanTank.deserializeNBT(registries, compound.getCompound("CleanTank"));
		super.read(compound, registries, clientPacket);
	}

	/** Neither slurry tank has a window, so the goggles are the only way to read them. */
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		added |= describe(tooltip, dirtyTank);
		added |= describe(tooltip, cleanTank);
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
			.add(CreateLang.number(TANK_CAPACITY))
			.add(CreateLang.text("mB"))
			.style(ChatFormatting.GOLD)
			.forGoggles(tooltip, 1);
		return true;
	}
}
