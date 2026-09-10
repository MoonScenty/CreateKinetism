package me.moonscenty.createkinetism.content.solar;

import java.util.List;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.common.capabilities.Capabilities;

import me.moonscenty.createkinetism.content.recipe.ActivatingRecipe;
import me.moonscenty.createkinetism.foundation.CKChemicalTanks;
import me.moonscenty.createkinetism.foundation.CKLang;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import org.jetbrains.annotations.Nullable;

/**
 * One gas turned into another by standing in the sun.
 *
 * <p>No basin any more and no rotation either. A basin cannot hold a Mekanism chemical, and this
 * machine's recipe is a gas on both sides, so both tanks are the machine's own - see
 * {@link ActivatingRecipe}. Nothing drives it: in Mekanism it runs on daylight, and inventing a
 * shaft for it would be inventing a cost the machine never had.</p>
 *
 * <p>What gates it is the sky, and the block that has to see it is the panel on top - see
 * {@link #hasSunlight()}. Losing the sun mid-cycle pauses rather than resets; a machine that threw
 * away nine seconds of work because a cloud arrived would read as broken, and Mekanism's does not do
 * that either.</p>
 */
public class SolarNeutronActivatorBlockEntity extends SmartBlockEntity
	implements IMekanismChemicalHandler, IHaveGoggleInformation {

	/** 07:00 and 17:00 in ticks, counting from dawn at 0. */
	public static final int SUNRISE = 1000;
	public static final int SUNSET = 11000;

	/** One bucket each way, matching the other machines here that carry their own tanks. */
	public static final long CAPACITY = 1000;

	// Radioactive-friendly: nuclear waste is what this machine is for, and an ordinary input tank
	// turns it away. See CKChemicalTanks.
	public final IChemicalTank inputTank = CKChemicalTanks.radioactiveInput(CAPACITY, this);
	public final IChemicalTank outputTank = BasicChemicalTank.output(CAPACITY, this);

	private final List<IChemicalTank> tanks = List.of(inputTank, outputTank);

	public int processingTicks = -1;

	/** Set whenever a tank moves; cleared once the machine has looked for work again. */
	private boolean searchForRecipe = true;

	public SolarNeutronActivatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		return tanks;
	}

	/**
	 * The tanks' listener.
	 *
	 * <p>Gas arriving is the only thing that makes a stopped machine worth a look, and it is also the
	 * only thing the client would otherwise never hear about: nothing else here sends an update while
	 * the machine is idle, so a tank filled at night stayed empty on the client and the goggles had
	 * nothing to show. {@code sendData} coalesces per tick, so saying it on every change is cheap.</p>
	 */
	@Override
	public void onContentsChanged() {
		notifyUpdate();
		searchForRecipe = true;
	}

	/**
	 * Bound to the face it was asked for - see {@link SidedChemicalAccess}.
	 *
	 * <p>Handing out the machine itself would let a pipe fill the outlet and drain the inlet, because
	 * a sideless handler counts as the machine's own hands.</p>
	 */
	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<SolarNeutronActivatorBlockEntity> type) {
		event.registerBlockEntity(Capabilities.CHEMICAL.block(), type,
			(be, context) -> new SidedChemicalAccess(be, context));
	}

	/**
	 * Whether the panel is being paid.
	 *
	 * <p>Four things have to hold: a dimension that has a sky at all, clear weather, the ten hours
	 * between 07:00 and 17:00, and nothing standing over the panel. {@code getDayTime} counts from
	 * dawn, so 07:00 is tick 1000.</p>
	 *
	 * <p>The cell tested is the one <em>above the panel</em>, not the panel's own: the panel block
	 * fills its cell, and a block cannot see the sky through itself.</p>
	 */
	public boolean hasSunlight() {
		if (level == null)
			return false;
		if (!level.dimensionType()
			.hasSkyLight())
			return false;
		if (level.isRaining())
			return false;
		long time = level.getDayTime() % 24000L;
		if (time < SUNRISE || time >= SUNSET)
			return false;
		return level.canSeeSky(worldPosition.above(2));
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;

		if (!hasSunlight()) {
			// Paused, not cancelled: the work already done is still there when the cloud passes. The
			// flag is left standing, so the sun coming back is enough to start the search again.
			return;
		}

		if (processingTicks > 0) {
			processingTicks--;
			return;
		}

		if (processingTicks == 0) {
			// Looked up again rather than remembered: the tanks can change underneath a running
			// machine, and finishing a recipe they no longer satisfy would make gas out of nothing.
			ActivatingRecipe recipe = findRecipe();
			if (recipe != null)
				apply(recipe);
			processingTicks = -1;
			searchForRecipe = true;
			sendData();
			return;
		}

		// Only look for work when something actually changed.
		if (!searchForRecipe)
			return;
		searchForRecipe = false;
		ActivatingRecipe recipe = findRecipe();
		if (recipe == null)
			return;
		processingTicks = Math.max(recipe.processingTime(), 20);
		sendData();
	}

	/** The first recipe the inlet satisfies and the outlet has room for. */
	@Nullable
	private ActivatingRecipe findRecipe() {
		if (level == null)
			return null;
		ChemicalStack held = inputTank.getStack();
		if (held.isEmpty())
			return null;

		for (RecipeHolder<ActivatingRecipe> holder : level.getRecipeManager()
			.getAllRecipesFor(CKRecipeTypes.ACTIVATING.<RecipeInput, ActivatingRecipe>getType())) {
			ActivatingRecipe recipe = holder.value();
			if (!recipe.matches(held))
				continue;
			if (!outputTank.insert(recipe.getChemicalOutput(), Action.SIMULATE, AutomationType.INTERNAL)
				.isEmpty())
				continue;
			return recipe;
		}
		return null;
	}

	private void apply(ActivatingRecipe recipe) {
		inputTank.extract(recipe.getRequiredAmount(), Action.EXECUTE, AutomationType.INTERNAL);
		outputTank.insert(recipe.getChemicalOutput(), Action.EXECUTE, AutomationType.INTERNAL);
	}

	/**
	 * Always says something, even standing empty in the sun.
	 *
	 * <p>Nothing drives this machine, so there is no stress line under it the way there is under
	 * every other machine here - if the tanks were left out when empty, a new activator would answer
	 * a pair of goggles with nothing at all and read as unfinished. Both tanks are named and the sky
	 * is reported whether or not there is work going on.</p>
	 */
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		CKLang.translate("gui.goggles.solar_neutron_activator")
			.forGoggles(tooltip);

		describe(tooltip, "gui.solar_neutron_activator.input", inputTank);
		describe(tooltip, "gui.solar_neutron_activator.output", outputTank);

		boolean sunlit = hasSunlight();
		CKLang.translate(sunlit ? "gui.solar_neutron_activator.sunlit"
			: "gui.solar_neutron_activator.no_sun")
			.style(sunlit ? ChatFormatting.GRAY : ChatFormatting.GOLD)
			.forGoggles(tooltip);
		return true;
	}

	/** One tank: its name, then what is in it - or that it is empty, which is worth saying too. */
	private static void describe(List<Component> tooltip, String label, IChemicalTank tank) {
		CKLang.translate(label)
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip);

		ChemicalStack held = tank.getStack();
		if (held.isEmpty()) {
			CKLang.translate("gui.solar_neutron_activator.empty")
				.style(ChatFormatting.DARK_GRAY)
				.forGoggles(tooltip, 1);
			return;
		}

		CKLang.builder()
			.add(Component.translatable(held.getChemical()
				.getTranslationKey()))
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip, 1);
		CKLang.builder()
			.text(held.getAmount() + " / " + CAPACITY + "mB")
			.style(ChatFormatting.GOLD)
			.forGoggles(tooltip, 1);
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putInt("ProcessingTicks", processingTicks);
		compound.put("InputTank", inputTank.serializeNBT(registries));
		compound.put("OutputTank", outputTank.serializeNBT(registries));
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		processingTicks = compound.getInt("ProcessingTicks");
		if (compound.contains("InputTank"))
			inputTank.deserializeNBT(registries, compound.getCompound("InputTank"));
		if (compound.contains("OutputTank"))
			outputTank.deserializeNBT(registries, compound.getCompound("OutputTank"));
		super.read(compound, registries, clientPacket);
	}
}
