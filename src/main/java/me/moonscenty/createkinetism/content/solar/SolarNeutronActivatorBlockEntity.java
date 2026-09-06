package me.moonscenty.createkinetism.content.solar;

import java.util.List;
import java.util.Optional;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;

import me.moonscenty.createkinetism.foundation.CKLang;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.ChatFormatting;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A basin operator on a timer instead of a driveshaft.
 *
 * <p>{@link BasinOperatingBlockEntity} is Create's, and it is written for kinetic machines: its
 * {@code updateBasin} refuses to look for work while the speed is zero, which for this block is
 * always. {@link #updateBasin()} is therefore that method with the two speed tests swapped for
 * {@link #hasSunlight()}, and everything else it gives us - the recipe trie, the deferral the basin
 * pokes when its contents change, {@code applyBasinRecipe} - is used unchanged.</p>
 *
 * <p>Losing the sun mid-cycle pauses rather than resets. A machine that threw away nine seconds of
 * work because a cloud arrived would be read as broken, and Mekanism's does not do that either.</p>
 */
public class SolarNeutronActivatorBlockEntity extends BasinOperatingBlockEntity {

	/** 07:00 and 17:00 in ticks, counting from dawn at 0. */
	public static final int SUNRISE = 1000;
	public static final int SUNSET = 11000;

	/** What a recipe that does not say otherwise takes. */
	private static final int DEFAULT_DURATION = 100;

	/** How often an idle machine looks around. Dawn is not a basin change, so nothing wakes it. */
	private static final int IDLE_POLL = 20;

	public boolean running;
	public int processingTicks;

	public SolarNeutronActivatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/**
	 * Up, not down - this machine reaches over its head.
	 *
	 * <p>Every other basin machine hangs above its basin, and Create's default looks {@code below(2)}
	 * for that reason. Note that {@code BasinBlockEntity.getOperator} looks {@code above(2)} and is not
	 * overridable, so the basin never pokes this block when its contents change - which is what the
	 * idle poll in {@link #tick()} is for.</p>
	 */
	@Override
	protected Optional<BasinBlockEntity> getBasin() {
		if (level == null)
			return Optional.empty();
		BlockEntity basin = level.getBlockEntity(worldPosition.above(2));
		return basin instanceof BasinBlockEntity found ? Optional.of(found) : Optional.empty();
	}

	/**
	 * Whether the panel is being paid.
	 *
	 * <p>Four things have to hold: a dimension that has a sky at all, an unobstructed one above the
	 * block, clear weather, and the ten hours between 07:00 and 17:00. {@code getDayTime} counts from
	 * dawn, so 07:00 is tick 1000.</p>
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
		return panelsSeeSky();
	}

	/**
	 * Whether any of the four wings is out in the open.
	 *
	 * <p>Asking about the block's own column would answer no every time: the basin this machine works
	 * stands two above it. But the panel is not up there - it is five slabs at y 6, four of which fan a
	 * full block out to the sides, so the cells that matter are the four horizontal neighbours. One of
	 * them catching the sun is enough; a machine that needed all four would be unbuildable against a
	 * wall for no reason a player could see.</p>
	 */
	private boolean panelsSeeSky() {
		for (Direction wing : Iterate.horizontalDirections)
			if (level.canSeeSky(worldPosition.relative(wing)))
				return true;
		return false;
	}

	@Override
	protected boolean updateBasin() {
		if (isRunning())
			return true;
		if (level == null || level.isClientSide)
			return true;
		if (!hasSunlight())
			return true;
		Optional<BasinBlockEntity> basin = getBasin();
		if (!basin.filter(BasinBlockEntity::canContinueProcessing)
			.isPresent())
			return true;

		List<Recipe<?>> recipes = getMatchingRecipes();
		if (recipes.isEmpty())
			return true;
		currentRecipe = recipes.get(0);
		startProcessingBasin();
		sendData();
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;

		if (!running) {
			if (level.getGameTime() % IDLE_POLL == 0)
				basinChecker.scheduleUpdate();
			return;
		}

		// Nothing tells us the basin was broken, so check while we hold a job.
		if (getBasin().isEmpty()) {
			onBasinRemoved();
			sendData();
			return;
		}

		// A cloud, or nightfall, holds the count where it is.
		if (!hasSunlight())
			return;

		if (--processingTicks > 0)
			return;

		applyBasinRecipe();
		running = false;
		processingTicks = 0;
		basinChecker.scheduleUpdate();
		sendData();
	}

	@Override
	public void startProcessingBasin() {
		if (running)
			return;
		super.startProcessingBasin();
		running = true;
		processingTicks = DEFAULT_DURATION;
		if (currentRecipe instanceof ProcessingRecipe<?, ?> processing && processing.getProcessingDuration() > 0)
			processingTicks = processing.getProcessingDuration();
	}

	@Override
	public boolean continueWithPreviousRecipe() {
		return true;
	}

	@Override
	protected void onBasinRemoved() {
		running = false;
		processingTicks = 0;
	}

	@Override
	protected boolean isRunning() {
		return running;
	}

	@Override
	protected Object getRecipeCacheKey() {
		return CKRecipeTypes.ACTIVATING;
	}

	@Override
	protected boolean matchStaticFilters(RecipeHolder<? extends Recipe<?>> recipe) {
		return recipe.value()
			.getType() == CKRecipeTypes.ACTIVATING.getType();
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putBoolean("Running", running);
		compound.putInt("Ticks", processingTicks);
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		running = compound.getBoolean("Running");
		processingTicks = compound.getInt("Ticks");
		super.read(compound, registries, clientPacket);
	}

	/**
	 * Kinetic stats would be a lie on a block with no shaft, so this replaces
	 * {@code KineticBlockEntity}'s tooltip outright and says the one thing that decides whether the
	 * machine runs.
	 */
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		CKLang.translate("gui.goggles.solar_neutron_activator")
			.forGoggles(tooltip);
		CKLang.translate(hasSunlight() ? "tooltip.solar.in_sunlight" : "tooltip.solar.no_sunlight")
			.style(hasSunlight() ? ChatFormatting.AQUA : ChatFormatting.GRAY)
			.forGoggles(tooltip);
		return true;
	}
}
