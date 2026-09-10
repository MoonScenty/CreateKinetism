package me.moonscenty.createkinetism.content.waste;

import java.util.List;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.common.capabilities.Capabilities;

import me.moonscenty.createkinetism.content.recipe.DecayingRecipe;
import me.moonscenty.createkinetism.foundation.CKChemicalTanks;
import me.moonscenty.createkinetism.foundation.SidedChemicalAccess;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import org.jetbrains.annotations.Nullable;

/**
 * A drum of waste that quietly gets smaller.
 *
 * <p>Two things it does, and both are about the same problem - waste is the one thing in this mod
 * that every recipe makes and nothing consumes.</p>
 *
 * <p><b>It decays.</b> One millibucket a second, flat, whatever is in it - the same rate Mekanism's
 * Radioactive Waste Barrel runs at. Slow enough that a drum is not a disposal chute (a reactor line
 * will outrun one), but it makes waste a storage problem rather than a dead end. A {@code decaying}
 * recipe says <em>what</em> may go in and what it becomes; the speed is the drum's own, so a pack
 * adding a waste does not get to make it rot faster.</p>
 *
 * <p><b>It falls.</b> A drum with another drum beneath it hands its contents down, so a column of
 * them fills from the bottom and reads as one deep tank. Only into another drum: pushing into
 * whatever happened to be under it would make the drum a pipe, and the point of the block is that
 * waste stops here.</p>
 *
 * <p>What it holds is a Mekanism chemical. Every waste in the game is one - nuclear waste has no
 * fluid form at all - and this block sat empty for exactly as long as it was asking for a fluid.</p>
 */
public class RadioactiveWasteDrumBlockEntity extends SmartBlockEntity
	implements IHaveGoggleInformation, IMekanismChemicalHandler {

	public static final long CAPACITY = 1000;

	/** Ticks between decay steps - the block's lazy tick rate, kept here so the maths can name it. */
	private static final int LAZY_TICK_RATE = 20;

	/** Per decay step, and so per second. One a second, as Mekanism's barrel does it. */
	public static final long DECAY_PER_SECOND = 1;

	/** Per tick, into the drum below. Only ever moves what that one still has room for. */
	private static final long SETTLE_RATE = 20;

	/**
	 * Fills from outside, and nothing takes it back out.
	 *
	 * <p>That refusal is the block. Waste that could be siphoned off again would make this a tank;
	 * what it is is the end of the line. The validator is the second half of the same idea - see
	 * {@link #isWaste}.</p>
	 */
	public final IChemicalTank tank =
		CKChemicalTanks.radioactiveInput(CAPACITY, this::isWaste, this);

	private final List<IChemicalTank> tanks = List.of(tank);

	public RadioactiveWasteDrumBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		setLazyTickRate(LAZY_TICK_RATE);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		return tanks;
	}

	@Override
	public void onContentsChanged() {
		notifyUpdate();
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<RadioactiveWasteDrumBlockEntity> type) {
		event.registerBlockEntity(Capabilities.CHEMICAL.block(), type,
			(be, context) -> new SidedChemicalAccess(be, context));
	}

	/**
	 * Whatever a {@code decaying} recipe names, and nothing else.
	 *
	 * <p>The drum has no drain of its own, so a chemical it cannot rot is one a player cannot get
	 * back out by any means the block offers. Refusing at the inlet is the only place to say so.</p>
	 */
	public boolean isWaste(ChemicalStack stack) {
		return stack.isEmpty() || findRecipe(stack) != null;
	}

	@Nullable
	private DecayingRecipe findRecipe(ChemicalStack held) {
		if (level == null || held.isEmpty())
			return null;
		for (RecipeHolder<DecayingRecipe> holder : level.getRecipeManager()
			.getAllRecipesFor(CKRecipeTypes.DECAYING.<RecipeInput, DecayingRecipe>getType())) {
			// Type only, not amount: what is being asked here is whether the drum takes this at all,
			// and a pipe offers whatever it happens to be carrying.
			if (holder.value()
				.matches(held))
				return holder.value();
		}
		return null;
	}

	public ChemicalStack getContents() {
		return tank.getStack();
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		settle();
	}

	/** One millibucket, once a second, into whatever the recipe says - usually into nothing. */
	@Override
	public void lazyTick() {
		super.lazyTick();
		if (level == null || level.isClientSide)
			return;
		ChemicalStack held = getContents();
		if (held.isEmpty())
			return;

		DecayingRecipe recipe = findRecipe(held);
		if (recipe == null)
			return;

		long lost = Math.min(DECAY_PER_SECOND, held.getAmount());
		ChemicalStack made = recipe.yieldFor(lost);

		tank.extract(lost, Action.EXECUTE, AutomationType.INTERNAL);
		// A recipe with no result is the usual case: the waste simply goes away.
		if (!made.isEmpty())
			tank.insert(made, Action.EXECUTE, AutomationType.INTERNAL);
		notifyUpdate();
	}

	/** Hand what fits down to the drum below. */
	private void settle() {
		ChemicalStack held = getContents();
		if (held.isEmpty())
			return;

		BlockEntity beneath = level.getBlockEntity(worldPosition.below());
		if (!(beneath instanceof RadioactiveWasteDrumBlockEntity drum))
			return;

		IChemicalHandler below = level.getCapability(Capabilities.CHEMICAL.block(), drum.getBlockPos(),
			Direction.UP);
		if (below == null)
			return;

		ChemicalStack offer = held.copyWithAmount(Math.min(SETTLE_RATE, held.getAmount()));
		long moved = offer.getAmount() - below.insertChemical(offer, Action.EXECUTE)
			.getAmount();
		if (moved <= 0)
			return;

		tank.extract(moved, Action.EXECUTE, AutomationType.INTERNAL);
		notifyUpdate();
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.put("Tank", tank.serializeNBT(registries));
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		if (compound.contains("Tank"))
			tank.deserializeNBT(registries, compound.getCompound("Tank"));
		super.read(compound, registries, clientPacket);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		ChemicalStack held = getContents();
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
}
