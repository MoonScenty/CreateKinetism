package me.moonscenty.createkinetism.content.chamber;

import java.util.List;
import java.util.Optional;

import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.recipe.RecipeApplier;

import me.moonscenty.createkinetism.content.recipe.ChamberRecipe;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Mekanism's Enrichment Chamber as a press: it works on what is underneath it rather than on an
 * inventory of its own.
 *
 * <p>The other chambers are millstones - you feed them and they hold what you gave them. This one is
 * built on Create's Mechanical Press instead, which means it reads its input off a depot, a belt, a
 * basin or a loose item on the ground below, and the head coming down is what actually performs the
 * recipe. Create does all of that in {@link EnrichingBehaviour}; the only thing that differs here is
 * which recipe type gets looked up.</p>
 *
 * <p>Reusing that behaviour also brings the parts of a press that are easy to forget: it drives its
 * own head animation, it lets belts keep moving items through, and it will not start again until the
 * head has come back up.</p>
 */
public class MechanicalEnricherBlockEntity extends BasinOperatingBlockEntity
	implements EnrichingBehaviour.EnrichingBehaviourSpecifics {

	private static final Object RECIPE_CACHE_KEY = new Object();

	public EnrichingBehaviour pressingBehaviour;

	public MechanicalEnricherBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** Room for the head reaching down and for the poles standing above the block. */
	@Override
	protected AABB createRenderBoundingBox() {
		return new AABB(worldPosition).expandTowards(0, -1.5, 0)
			.expandTowards(0, 1, 0);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		pressingBehaviour = new EnrichingBehaviour(this);
		behaviours.add(pressingBehaviour);
	}

	public EnrichingBehaviour getPressingBehaviour() {
		return pressingBehaviour;
	}

	public Optional<RecipeHolder<ChamberRecipe>> getRecipe(ItemStack item) {
		return CKRecipeTypes.ENRICHING.find(new SingleRecipeInput(item), level);
	}

	@Override
	public boolean tryProcessInWorld(ItemEntity itemEntity, boolean simulate) {
		ItemStack item = itemEntity.getItem();
		Optional<RecipeHolder<ChamberRecipe>> recipe = getRecipe(item);
		if (recipe.isEmpty())
			return false;
		if (simulate)
			return true;

		pressingBehaviour.particleItems.add(item);

		// One at a time, so a stack dropped on the floor does not turn into a stack of results in a
		// single stroke. The belt path below is where throughput is meant to come from.
		for (ItemStack result : RecipeApplier.applyRecipeOn(level, item.copyWithCount(1),
			recipe.get()
				.value(),
			true)) {
			ItemEntity created =
				new ItemEntity(level, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), result);
			created.setDefaultPickUpDelay();
			created.setDeltaMovement(VecHelper.offsetRandomly(Vec3.ZERO, level.random, .05f));
			level.addFreshEntity(created);
		}
		item.shrink(1);
		return true;
	}

	@Override
	public boolean tryProcessOnBelt(TransportedItemStack input, List<ItemStack> outputList, boolean simulate) {
		Optional<RecipeHolder<ChamberRecipe>> recipe = getRecipe(input.stack);
		if (recipe.isEmpty())
			return false;
		if (simulate)
			return true;

		pressingBehaviour.particleItems.add(input.stack);
		outputList.addAll(RecipeApplier.applyRecipeOn(level, input.stack.copyWithCount(1), recipe.get()
			.value(), true));
		return true;
	}

	@Override
	public boolean tryProcessInBasin(boolean simulate) {
		applyBasinRecipe();

		Optional<BasinBlockEntity> basin = getBasin();
		if (basin.isPresent()) {
			SmartInventory inputs = basin.get()
				.getInputInventory();
			for (int slot = 0; slot < inputs.getSlots(); slot++) {
				ItemStack stackInSlot = inputs.getItem(slot);
				if (!stackInSlot.isEmpty())
					pressingBehaviour.particleItems.add(stackInSlot);
			}
		}

		return true;
	}

	@Override
	public void onPressingCompleted() {
		if (pressingBehaviour.onBasin() && matchBasinRecipe(currentRecipe)
			&& getBasin().filter(BasinBlockEntity::canContinueProcessing)
				.isPresent())
			startProcessingBasin();
		else
			basinChecker.scheduleUpdate();
	}

	@Override
	protected boolean matchStaticFilters(RecipeHolder<? extends Recipe<?>> recipe) {
		return recipe.value()
			.getType() == CKRecipeTypes.ENRICHING.getType();
	}

	@Override
	public float getKineticSpeed() {
		return getSpeed();
	}

	/**
	 * One item per stroke, always. Create makes this a config knob because a press can compact whole
	 * stacks; an enricher doubling ore that way would undo the point of building a line of them.
	 */
	@Override
	public boolean canProcessInBulk() {
		return false;
	}

	@Override
	public int getParticleAmount() {
		return 15;
	}

	@Override
	protected Object getRecipeCacheKey() {
		return RECIPE_CACHE_KEY;
	}

	@Override
	public void startProcessingBasin() {
		if (pressingBehaviour.running && pressingBehaviour.runningTicks <= EnrichingBehaviour.CYCLE / 2)
			return;
		super.startProcessingBasin();
		pressingBehaviour.start(EnrichingBehaviour.Mode.BASIN);
	}

	@Override
	protected void onBasinRemoved() {
		pressingBehaviour.particleItems.clear();
		pressingBehaviour.running = false;
		pressingBehaviour.runningTicks = 0;
		sendData();
	}

	@Override
	protected boolean isRunning() {
		return pressingBehaviour.running;
	}
}
