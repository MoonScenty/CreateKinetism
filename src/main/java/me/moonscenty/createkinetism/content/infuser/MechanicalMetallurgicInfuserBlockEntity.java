package me.moonscenty.createkinetism.content.infuser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.common.capabilities.Capabilities;

import me.moonscenty.createkinetism.content.recipe.ConvertingRecipe;
import me.moonscenty.createkinetism.content.recipe.InfusingRecipe;
import me.moonscenty.createkinetism.content.recipe.MekanismRecipes;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.VecHelper;

import org.jetbrains.annotations.Nullable;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The infuser's working half: a spout that has to be turned.
 *
 * <p>Create's spout is passive, so everything about paying for it is new. {@link KineticBlockEntity}
 * supplies the shaft, the stress impact and the goggle readout; the belt hook is the spout's,
 * because the machine works on whatever passes underneath rather than on slots of its own.</p>
 *
 * <p>What it drips is a Mekanism {@link ChemicalStack} out of a Mekanism {@link IChemicalTank} - an
 * infuse type, which is what Mekanism actually infuses with. This mod registers no fluid standing in
 * for one. Mekanism gives its infuse types no fluid form either, so a Create pipe cannot bring one
 * here - it does not have to, because the machine makes its own.</p>
 *
 * <p>That is the other half, and Mekanism puts it on this same machine: drop an Enriched item into
 * {@link #getInventory()} - or the plain one, at an eighth of the yield - and a {@code converting}
 * recipe dissolves it into the tank. This mod needed a separate Chemical Tank block for that back
 * when the infusion had to arrive as a fluid through a pipe; it does not any more.</p>
 */
public class MechanicalMetallurgicInfuserBlockEntity extends KineticBlockEntity
	implements IHaveGoggleInformation, IMekanismChemicalHandler {

	public static final int PROCESSING_TIME = 20;

	/** Millibuckets of infusion the machine holds. Eighty is one Enriched item's worth. */
	public static final long CAPACITY = 1000;

	protected BeltProcessingBehaviour beltProcessing;

	/**
	 * Anything Mekanism will let in. The recipe decides what is useful, and refusing chemicals here
	 * as well would only mean a pressurized tube failing silently against a machine that looks idle.
	 */
	public final IChemicalTank chemicalTank = BasicChemicalTank.createAllValid(CAPACITY, this);

	/** One tank, offered on every side - see {@link #getChemicalTanks}. */
	private final List<IChemicalTank> tanks = List.of(chemicalTank);

	/**
	 * The infusion slot. Takes only what a {@code converting} recipe knows how to dissolve, so a
	 * funnel aimed at this machine cannot silt it up with the iron it is meant to be infusing.
	 */
	private final ItemStackHandler inventory = new ItemStackHandler(1) {
		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return findConverting(stack) != null;
		}
	};

	public int processingTicks = -1;
	public boolean sendSplash;

	/**
	 * How full the tank looks, chasing how full it is. Create's own tank behaviour smoothed this for
	 * us; the Mekanism tank has no idea it is being drawn, so the easing lives here.
	 */
	public final LerpedFloat fillLevel = LerpedFloat.linear()
		.startWithValue(0)
		.chase(0, .25f, Chaser.EXP);

	public MechanicalMetallurgicInfuserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** {@link IMekanismChemicalHandler} is also the tank's listener, so this covers both. */
	@Override
	public void onContentsChanged() {
		setChanged();
	}

	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		return tanks;
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<MechanicalMetallurgicInfuserBlockEntity> type) {
		// Mekanism's own capability, so a pressurized tube can top the machine up as an alternative to
		// feeding it solids. Same tank either way.
		event.registerBlockEntity(Capabilities.CHEMICAL.block(), type, (be, context) -> be);
		event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, type,
			(be, context) -> be.inventory);
	}

	/** The nozzle reaches down to the depot two blocks below. */
	@Override
	protected AABB createRenderBoundingBox() {
		return super.createRenderBoundingBox().expandTowards(0, -2, 0);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);

		beltProcessing = new BeltProcessingBehaviour(this).whenItemEnters(this::onItemReceived)
			.whileItemHeld(this::whenItemHeld);
		behaviours.add(beltProcessing);
	}

	public ChemicalStack getStoredChemical() {
		return chemicalTank.getStack();
	}

	/** For the comparator: how full the infusion tank is, on the usual 0-15 scale. */
	public int getComparatorOutput() {
		if (chemicalTank.isEmpty())
			return 0;
		return 1 + Mth.floor(chemicalTank.getStored() / (double) CAPACITY * 14);
	}

	/**
	 * Nothing happens on a stalled shaft. This is the whole difference from Create's spout, and it is
	 * checked on both belt callbacks so an item is neither held nor consumed while the machine is
	 * stopped.
	 */
	private boolean canProcess() {
		return getSpeed() != 0 && !chemicalTank.isEmpty();
	}

	/**
	 * The recipe that the item below and the chemical in the tank together satisfy.
	 *
	 * <p>Both halves are checked here rather than in the recipe's own {@code matches}: the belt hands
	 * us only the item, so the infusion is matched against our own tank.</p>
	 */
	private Optional<InfusingRecipe> getMatchingRecipe(ItemStack stack) {
		if (level == null)
			return Optional.empty();
		SingleRecipeInput input = new SingleRecipeInput(stack);
		ChemicalStack available = getStoredChemical();
		for (RecipeHolder<InfusingRecipe> holder : MekanismRecipes.infusing(level)) {
			InfusingRecipe recipe = holder.value();
			if (recipe.matches(input, level) && recipe.matchesChemical(available))
				return Optional.of(recipe);
		}
		return Optional.empty();
	}

	protected ProcessingResult onItemReceived(TransportedItemStack transported,
		TransportedItemStackHandlerBehaviour handler) {
		if (handler.blockEntity.isVirtual())
			return ProcessingResult.PASS;
		if (!canProcess())
			return ProcessingResult.PASS;
		// An item with no recipe is waved through rather than held, so a belt can carry unrelated
		// cargo underneath a working infuser.
		return getMatchingRecipe(transported.stack).isPresent() ? ProcessingResult.HOLD
			: ProcessingResult.PASS;
	}

	protected ProcessingResult whenItemHeld(TransportedItemStack transported,
		TransportedItemStackHandlerBehaviour handler) {
		if (processingTicks != -1 && processingTicks != 5)
			return ProcessingResult.HOLD;
		if (!canProcess())
			return ProcessingResult.PASS;

		Optional<InfusingRecipe> match = getMatchingRecipe(transported.stack);
		if (match.isEmpty())
			return ProcessingResult.PASS;
		InfusingRecipe recipe = match.get();

		if (processingTicks == -1) {
			// The renderer retracts the nozzle over the last ten ticks, so anything shorter than that
			// would finish before the nozzle had finished reaching down.
			processingTicks = Math.max(recipe.processingTime(), 10);
			notifyUpdate();
			AllSoundEvents.SPOUTING.playOnServer(level, worldPosition, 0.75f,
				0.9f + 0.2f * (float) Math.random());
			return ProcessingResult.HOLD;
		}

		// Process finished
		chemicalTank.extract(recipe.getRequiredAmount(), Action.EXECUTE, AutomationType.INTERNAL);

		transported.stack.shrink(1);
		transported.clearFanProcessingData();

		List<TransportedItemStack> outList = new ArrayList<>();
		TransportedItemStack result = transported.copy();
		result.stack = recipe.getResultItem();
		outList.add(result);
		TransportedItemStack held = transported.stack.isEmpty() ? null : transported.copy();
		handler.handleProcessingOnItem(transported, TransportedResult.convertToAndLeaveHeld(outList, held));

		sendSplash = true;
		notifyUpdate();
		return ProcessingResult.HOLD;
	}

	@Override
	public void tick() {
		super.tick();
		if (level != null && !level.isClientSide)
			dissolveOne();
		if (level != null && level.isClientSide) {
			fillLevel.chase(getFillFraction(), .25f, Chaser.EXP);
			fillLevel.tickChaser();
		}
		if (processingTicks >= 0)
			processingTicks--;
		if (processingTicks >= 8 && level != null && level.isClientSide)
			spawnProcessingParticles();
	}

	public float getFillFraction() {
		return chemicalTank.isEmpty() ? 0 : chemicalTank.getStored() / (float) CAPACITY;
	}

	/**
	 * One item per tick at most, and only when the whole yield fits - a half-dissolved item would be
	 * spent for nothing. Deliberately not gated on the shaft: dissolving is not the work, dripping
	 * is, so a stopped infuser can still be filled ready to run.
	 */
	private void dissolveOne() {
		ItemStack stack = inventory.getStackInSlot(0);
		if (stack.isEmpty())
			return;

		ConvertingRecipe recipe = findConverting(stack);
		if (recipe == null)
			return;

		ChemicalStack yield = recipe.output();
		if (!chemicalTank.insert(yield, Action.SIMULATE, AutomationType.INTERNAL)
			.isEmpty())
			return;

		chemicalTank.insert(yield, Action.EXECUTE, AutomationType.INTERNAL);
		stack.shrink(1);
		notifyUpdate();
	}

	/** What the slot would turn this item into, or null if it would not take it at all. */
	private ConvertingRecipe findConverting(ItemStack stack) {
		if (level == null || stack.isEmpty())
			return null;
		SingleRecipeInput input = new SingleRecipeInput(stack);
		for (RecipeHolder<ConvertingRecipe> holder : MekanismRecipes.converting(level))
			if (holder.value()
				.matches(input, level))
				return holder.value();
		return null;
	}

	public ItemStackHandler getInventory() {
		return inventory;
	}

	/**
	 * A chemical has no fluid particle, so the infusion is drawn in its own tint instead - the same
	 * colour Mekanism paints it with in its own tanks.
	 */
	private ParticleOptions infusionParticle() {
		return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,
			0xFF000000 | getStoredChemical().getChemical()
				.getTint());
	}

	private void spawnProcessingParticles() {
		if (isVirtual() || getStoredChemical().isEmpty())
			return;
		Vec3 vec = VecHelper.getCenterOf(worldPosition)
			.subtract(0, 8 / 16f, 0);
		level.addAlwaysVisibleParticle(infusionParticle(), vec.x, vec.y, vec.z, 0, -.1f, 0);
	}

	/** The infusion landing on the item, two blocks down. */
	private void spawnSplash() {
		if (isVirtual() || getStoredChemical().isEmpty())
			return;
		Vec3 vec = VecHelper.getCenterOf(worldPosition)
			.subtract(0, 2 - 5 / 16f, 0);
		ParticleOptions particle = infusionParticle();
		for (int i = 0; i < 20; i++) {
			Vec3 m = VecHelper.offsetRandomly(Vec3.ZERO, level.random, 0.125f);
			m = new Vec3(m.x, Math.abs(m.y), m.z);
			level.addAlwaysVisibleParticle(particle, vec.x, vec.y, vec.z, m.x, m.y, m.z);
		}
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putInt("ProcessingTicks", processingTicks);
		compound.put("Inventory", inventory.serializeNBT(registries));
		compound.put("ChemicalTank", chemicalTank.serializeNBT(registries));
		if (sendSplash && clientPacket) {
			compound.putBoolean("Splash", true);
			sendSplash = false;
		}
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		processingTicks = compound.getInt("ProcessingTicks");
		if (compound.contains("Inventory"))
			inventory.deserializeNBT(registries, compound.getCompound("Inventory"));
		if (compound.contains("ChemicalTank"))
			chemicalTank.deserializeNBT(registries, compound.getCompound("ChemicalTank"));
		super.read(compound, registries, clientPacket);
		if (clientPacket && compound.contains("Splash"))
			spawnSplash();
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);

		ChemicalStack held = getStoredChemical();
		if (held.isEmpty())
			return true;

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
