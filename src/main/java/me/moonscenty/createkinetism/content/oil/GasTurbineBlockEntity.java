package me.moonscenty.createkinetism.content.oil;

import java.util.List;
import java.util.Optional;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import me.moonscenty.createkinetism.content.recipe.EngineFuelRecipe;
import me.moonscenty.createkinetism.foundation.CKLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * The Gas Turbine, which burns gases and nothing else.
 *
 * <p>Everything an engine does is {@link FuelEngineBlockEntity}'s; the only difference is where the
 * fuel sits. This one keeps a Mekanism chemical tank instead of a fluid tank, so LPG, natural gas,
 * sour gas and Mekanism's own water vapour arrive down a pressurized tube - and a bucket of diesel
 * has nowhere to go, which is the point. A turbine that could be fed kerosene was a turbine in name
 * only.</p>
 *
 * <p>The tank refuses to be drained from outside: fuel goes in and is burned, not shuttled back out
 * by whatever tube filled it.</p>
 */
public class GasTurbineBlockEntity extends FuelEngineBlockEntity implements IMekanismChemicalHandler {

	/**
	 * Ticks between syncs of the tank to the client, matching what Create's own fluid tank does.
	 *
	 * <p>The turbine burns up to 500mB a tick on water vapour, so a packet per change would be a
	 * packet per tick for a readout nobody is watching unless they are wearing goggles.</p>
	 */
	private static final int SYNC_RATE = 8;

	public final IChemicalTank chemicalTank;
	private final List<IChemicalTank> tanks;
	private int syncCooldown;

	public GasTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		int capacity = state.getBlock() instanceof FuelEngineBlock engine ? engine.getTankCapacity() : 2000;
		chemicalTank = BasicChemicalTank.input(capacity, chemical -> true, this);
		tanks = List.of(chemicalTank);
	}

	/** No fluid tank at all - see the class comment. */
	@Override
	protected void addFuelBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		return tanks;
	}

	/** The tank's listener: whatever just arrived might be a fuel this turbine does not know yet. */
	@Override
	public void onContentsChanged() {
		setChanged();
		chemicalUpdate();
		if (syncCooldown == 0)
			syncCooldown = SYNC_RATE;
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide || syncCooldown == 0)
			return;
		if (--syncCooldown == 0)
			sendData();
	}

	/** Look up whatever is in the tank against the turbine's fuel list. Mirrors {@code fluidUpdate}. */
	public void chemicalUpdate() {
		if (level == null || level.isClientSide)
			return;
		ChemicalStack fuel = chemicalTank.getStack();

		if (fuel.isEmpty()) {
			if (currentFuel != null) {
				currentFuel = null;
				updateGeneratedRotation();
			}
			return;
		}

		if (currentFuel != null)
			return;

		Optional<EngineFuelRecipe> match = level.getRecipeManager()
			.getAllRecipesFor(fuelRecipeType().<RecipeInput, EngineFuelRecipe>getType())
			.stream()
			.map(RecipeHolder::value)
			.filter(recipe -> recipe.match(fuel))
			.findAny();

		if (match.isEmpty())
			return;
		currentFuel = match.get();
		updateGeneratedRotation();
	}

	@Override
	protected boolean hasFuel() {
		return !chemicalTank.isEmpty();
	}

	@Override
	protected void drainFuel(int amount) {
		chemicalTank.extract(amount, Action.EXECUTE, AutomationType.INTERNAL);
	}

	@Override
	protected boolean appendFuelTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		ChemicalStack held = chemicalTank.getStack();
		if (held.isEmpty())
			return false;
		CKLang.builder()
			.text("")
			.add(Component.translatable(held.getChemical()
				.getTranslationKey()))
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip);
		CKLang.builder()
			.text(held.getAmount() + " / " + chemicalTank.getCapacity() + "mB")
			.style(ChatFormatting.GOLD)
			.forGoggles(tooltip, 1);
		return true;
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.put("ChemicalTank", chemicalTank.serializeNBT(registries));
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		if (tag.contains("ChemicalTank"))
			chemicalTank.deserializeNBT(registries, tag.getCompound("ChemicalTank"));
	}
}
