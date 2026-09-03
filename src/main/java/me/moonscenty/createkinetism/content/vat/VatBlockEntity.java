package me.moonscenty.createkinetism.content.vat;

import java.util.Optional;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.item.SmartInventory;

import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.createmod.catnip.data.Couple;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Processing loop for {@link VatBlock}, a trimmed-down copy of Create's
 * {@code MechanicalMixerBlockEntity}.
 *
 * <p>The animation cycle is Create's: 20 ticks to lower the head, the recipe runs while the head is
 * down, 20 ticks to raise it. The duration of the "head down" phase scales with RPM the same way the
 * Mixer's does, which is how a Mekanism machine's energy-per-tick maps onto rotational force.</p>
 */
public class VatBlockEntity extends BasinOperatingBlockEntity {

	public int runningTicks;
	public int processingTicks;
	public boolean running;

	public VatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public CKRecipeTypes getRecipeType() {
		return getBlockState().getBlock() instanceof VatBlock vat ? vat.getRecipeType() : CKRecipeTypes.PURIFYING;
	}

	public float getRenderedHeadOffset(float partialTicks) {
		int localTick;
		float offset = 0;
		if (running) {
			if (runningTicks < 20) {
				localTick = runningTicks;
				float num = (localTick + partialTicks) / 20f;
				num = ((2 - Mth.cos((float) (num * Math.PI))) / 2);
				offset = num - .5f;
			} else if (runningTicks <= 20) {
				offset = 1;
			} else {
				localTick = 40 - runningTicks;
				float num = (localTick - partialTicks) / 20f;
				num = ((2 - Mth.cos((float) (num * Math.PI))) / 2);
				offset = num - .5f;
			}
		}
		return offset + 7 / 16f;
	}

	public float getRenderedHeadRotationSpeed(float partialTicks) {
		float speed = getSpeed();
		if (running) {
			if (runningTicks < 15)
				return speed;
			if (runningTicks <= 20)
				return speed * 2;
			return speed;
		}
		return speed / 2;
	}

	@Override
	protected AABB createRenderBoundingBox() {
		return new AABB(worldPosition).expandTowards(0, -1.5, 0);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		running = compound.getBoolean("Running");
		runningTicks = compound.getInt("Ticks");
		super.read(compound, registries, clientPacket);

		if (clientPacket && hasLevel())
			getBasin().ifPresent(basin -> basin.setAreFluidsMoving(running && runningTicks <= 20));
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putBoolean("Running", running);
		compound.putInt("Ticks", runningTicks);
		super.write(compound, registries, clientPacket);
	}

	@Override
	public void tick() {
		super.tick();

		if (runningTicks >= 40) {
			running = false;
			runningTicks = 0;
			basinChecker.scheduleUpdate();
			return;
		}

		float speed = Math.abs(getSpeed());
		if (running && level != null) {
			if (level.isClientSide && runningTicks == 20)
				renderParticles();

			if (getSpeed() == 0 || !isSpeedRequirementFulfilled()) {
				if (runningTicks < 20)
					runningTicks = 40 - runningTicks;
				else if (runningTicks == 20)
					runningTicks++;
			}

			if ((!level.isClientSide || isVirtual()) && runningTicks == 20) {
				if (processingTicks < 0) {
					float recipeSpeed = 1;
					// ProcessingRecipe, not StandardProcessingRecipe: getProcessingDuration is declared on
					// the former, and the Combiner extends it directly so it can carry its infusion
					// ingredient. Widening this covers every basin recipe either way.
					if (currentRecipe instanceof ProcessingRecipe<?, ?> processingRecipe) {
						int duration = processingRecipe.getProcessingDuration();
						if (duration != 0)
							recipeSpeed = duration / 100f;
					}

					processingTicks = Math.max((Mth.log2((int) (512 / speed))) * Mth.ceil(recipeSpeed * 15) + 1, 1);

					Optional<BasinBlockEntity> basin = getBasin();
					if (basin.isPresent()) {
						Couple<SmartFluidTankBehaviour> tanks = basin.get()
							.getTanks();
						if (!tanks.getFirst()
							.isEmpty()
							|| !tanks.getSecond()
								.isEmpty())
							level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT,
								SoundSource.BLOCKS, .75f, speed < 65 ? .75f : 1.5f);
					}

				} else {
					processingTicks--;
					if (processingTicks == 0) {
						runningTicks++;
						processingTicks = -1;
						applyBasinRecipe();
						sendData();
					}
				}
			}

			if (runningTicks != 20)
				runningTicks++;
		}
	}

	public void renderParticles() {
		Optional<BasinBlockEntity> basin = getBasin();
		if (basin.isEmpty() || level == null)
			return;

		for (SmartInventory inv : basin.get()
			.getInvs()) {
			for (int slot = 0; slot < inv.getSlots(); slot++) {
				ItemStack stackInSlot = inv.getItem(slot);
				if (stackInSlot.isEmpty())
					continue;
				spillParticle(new ItemParticleOption(ParticleTypes.ITEM, stackInSlot));
			}
		}
	}

	protected void spillParticle(ParticleOptions data) {
		float angle = level.random.nextFloat() * 360;
		Vec3 offset = new Vec3(0, 0, 0.25f);
		offset = VecHelper.rotate(offset, angle, Axis.Y);
		Vec3 target = VecHelper.rotate(offset, getSpeed() > 0 ? 25 : -25, Axis.Y)
			.add(0, .25f, 0);
		Vec3 center = offset.add(VecHelper.getCenterOf(worldPosition));
		target = VecHelper.offsetRandomly(target.subtract(offset), level.random, 1 / 128f);
		level.addParticle(data, center.x, center.y - 1.75f, center.z, target.x, target.y, target.z);
	}

	@Override
	protected boolean matchStaticFilters(RecipeHolder<? extends Recipe<?>> recipe) {
		return recipe.value()
			.getType() == getRecipeType().getType();
	}

	@Override
	public void startProcessingBasin() {
		if (running && runningTicks <= 20)
			return;
		super.startProcessingBasin();
		running = true;
		runningTicks = 0;
	}

	@Override
	public boolean continueWithPreviousRecipe() {
		runningTicks = 20;
		return true;
	}

	@Override
	protected void onBasinRemoved() {
		if (!running)
			return;
		runningTicks = 40;
		running = false;
	}

	@Override
	protected Object getRecipeCacheKey() {
		return getRecipeType();
	}

	@Override
	protected boolean isRunning() {
		return running;
	}
}
