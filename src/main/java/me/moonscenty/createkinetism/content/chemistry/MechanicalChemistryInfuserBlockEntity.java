package me.moonscenty.createkinetism.content.chemistry;

import java.util.List;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.common.capabilities.Capabilities;

import me.moonscenty.createkinetism.content.recipe.ChemicalInfusingRecipe;
import me.moonscenty.createkinetism.foundation.SidedChemicalAccess;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;

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

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import org.jetbrains.annotations.Nullable;

/**
 * Two gases in the sides, a third out of the middle.
 *
 * <p>Three chemical tanks and nothing else - no inventory, no fluid tank, no basin. The two side
 * tanks take what a pressurized tube offers to their own face and refuse to be drained from outside;
 * the main tank is the other way round. Which face is which follows {@link
 * MechanicalChemistryInfuserBlock#FACING}, so turning the machine turns its plumbing with it.</p>
 *
 * <p>Kinetic rather than passive, like every other machine here: it needs a shaft under it turning,
 * and the work takes as long as the recipe says.</p>
 */
public class MechanicalChemistryInfuserBlockEntity extends KineticBlockEntity
	implements IMekanismChemicalHandler {

	/** Each side tank. One bucket of gas is plenty for the recipes that exist. */
	public static final long SIDE_CAPACITY = 4000;

	/** The middle. Bigger, because nothing empties it but a tube the player has to remember. */
	public static final long MAIN_CAPACITY = 8000;

	/** Fed through the left face; a tube cannot pull back out of it. */
	public final IChemicalTank leftTank = BasicChemicalTank.input(SIDE_CAPACITY, chemical -> true, this);

	/** Fed through the right face, same terms. */
	public final IChemicalTank rightTank = BasicChemicalTank.input(SIDE_CAPACITY, chemical -> true, this);

	/** Filled only by a recipe, emptied by whatever is pulling on it. */
	public final IChemicalTank mainTank = BasicChemicalTank.output(MAIN_CAPACITY, this);

	private final List<IChemicalTank> all = List.of(leftTank, rightTank, mainTank);
	private final List<IChemicalTank> leftOnly = List.of(leftTank);
	private final List<IChemicalTank> rightOnly = List.of(rightTank);
	private final List<IChemicalTank> mainOnly = List.of(mainTank);

	/** How full each tank looks, chasing how full it is. Client-side only - see the renderer. */
	public final LerpedFloat leftLevel = level(), rightLevel = level(), mainLevel = level();

	private static LerpedFloat level() {
		return LerpedFloat.linear()
			.startWithValue(0)
			.chase(0, .25f, Chaser.EXP);
	}

	public int processingTicks = -1;
	private boolean contentsChanged = true;

	public MechanicalChemistryInfuserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** The tanks' listener as well as the capability's. */
	@Override
	public void onContentsChanged() {
		setChanged();
		contentsChanged = true;
	}

	/**
	 * The face the left tank shows to the world.
	 *
	 * <p>Unrotated, the model's front looks north and its left tank sits at x 0-7, which is west -
	 * and west is what {@code NORTH.getCounterClockWise()} gives. Every other facing follows.</p>
	 */
	public Direction leftFace() {
		return getBlockState().getValue(MechanicalChemistryInfuserBlock.FACING)
			.getCounterClockWise();
	}

	public Direction rightFace() {
		return getBlockState().getValue(MechanicalChemistryInfuserBlock.FACING)
			.getClockWise();
	}

	/**
	 * One tank per face, so a tube plumbed into the side it can see gets the tank behind it.
	 *
	 * <p>A null side is the unsided query - something asking what the block holds rather than what it
	 * would trade through a particular face - and that gets all three.</p>
	 */
	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		if (side == null)
			return all;
		if (side == leftFace())
			return leftOnly;
		if (side == rightFace())
			return rightOnly;
		return mainOnly;
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<MechanicalChemistryInfuserBlockEntity> type) {
		event.registerBlockEntity(Capabilities.CHEMICAL.block(), type,
			(be, context) -> new SidedChemicalAccess(be, context));
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;

		if (level.isClientSide) {
			chase(leftLevel, leftTank, SIDE_CAPACITY);
			chase(rightLevel, rightTank, SIDE_CAPACITY);
			chase(mainLevel, mainTank, MAIN_CAPACITY);
			return;
		}

		if (getSpeed() == 0) {
			if (processingTicks != -1) {
				processingTicks = -1;
				sendData();
			}
			return;
		}

		if (processingTicks > 0) {
			processingTicks--;
			return;
		}

		if (processingTicks == 0) {
			// Looked up again rather than remembered: the tanks can change underneath a running
			// machine, and finishing a recipe they no longer satisfy would make gas out of nothing.
			ChemicalInfusingRecipe recipe = findRecipe();
			if (recipe != null)
				apply(recipe);
			processingTicks = -1;
			contentsChanged = true;
			sendData();
			return;
		}

		// Only look for work when something actually changed, the way a basin does.
		if (!contentsChanged)
			return;
		contentsChanged = false;
		ChemicalInfusingRecipe recipe = findRecipe();
		if (recipe == null)
			return;
		processingTicks = Math.max(recipe.processingTime(), 20);
		sendData();
	}

	private static void chase(LerpedFloat lerp, IChemicalTank tank, long capacity) {
		lerp.chase(tank.isEmpty() ? 0 : tank.getStored() / (float) capacity, .25f, Chaser.EXP);
		lerp.tickChaser();
	}

	/** The first recipe both side tanks satisfy and the main tank has room for. */
	@Nullable
	private ChemicalInfusingRecipe findRecipe() {
		if (level == null)
			return null;
		ChemicalStack left = leftTank.getStack();
		ChemicalStack right = rightTank.getStack();
		if (left.isEmpty() || right.isEmpty())
			return null;

		for (RecipeHolder<ChemicalInfusingRecipe> holder : level.getRecipeManager()
			.getAllRecipesFor(CKRecipeTypes.CHEMICAL_INFUSING.<RecipeInput, ChemicalInfusingRecipe>getType())) {
			ChemicalInfusingRecipe recipe = holder.value();
			if (!recipe.matches(left, right))
				continue;
			if (!mainTank.insert(recipe.getChemicalOutput(), Action.SIMULATE, AutomationType.INTERNAL)
				.isEmpty())
				continue;
			return recipe;
		}
		return null;
	}

	private void apply(ChemicalInfusingRecipe recipe) {
		ChemicalStack left = leftTank.getStack();
		ChemicalStack right = rightTank.getStack();
		leftTank.extract(recipe.costFor(left, right, true), Action.EXECUTE, AutomationType.INTERNAL);
		rightTank.extract(recipe.costFor(left, right, false), Action.EXECUTE, AutomationType.INTERNAL);
		mainTank.insert(recipe.getChemicalOutput(), Action.EXECUTE, AutomationType.INTERNAL);
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putInt("ProcessingTicks", processingTicks);
		compound.put("LeftTank", leftTank.serializeNBT(registries));
		compound.put("RightTank", rightTank.serializeNBT(registries));
		compound.put("MainTank", mainTank.serializeNBT(registries));
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		processingTicks = compound.getInt("ProcessingTicks");
		if (compound.contains("LeftTank"))
			leftTank.deserializeNBT(registries, compound.getCompound("LeftTank"));
		if (compound.contains("RightTank"))
			rightTank.deserializeNBT(registries, compound.getCompound("RightTank"));
		if (compound.contains("MainTank"))
			mainTank.deserializeNBT(registries, compound.getCompound("MainTank"));
		super.read(compound, registries, clientPacket);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		added |= describe(tooltip, leftTank, SIDE_CAPACITY);
		added |= describe(tooltip, rightTank, SIDE_CAPACITY);
		added |= describe(tooltip, mainTank, MAIN_CAPACITY);
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
