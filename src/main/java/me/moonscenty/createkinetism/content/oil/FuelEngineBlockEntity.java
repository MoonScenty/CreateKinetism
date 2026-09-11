package me.moonscenty.createkinetism.content.oil;

import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity.RotationDirection;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import me.moonscenty.createkinetism.content.recipe.EngineFuelRecipe;
import me.moonscenty.createkinetism.foundation.CKLang;
import me.moonscenty.createkinetism.foundation.CKScrollOptionBehaviour;
import me.moonscenty.createkinetism.config.CKStress;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;
import me.moonscenty.createkinetism.registry.CKSoundEvents;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Burns a fuel and turns a shaft. One class covers the engines - the block decides which fuels
 * it accepts and how much it is worth, exactly the way the chambers and vats work.</p>
 *
 * <p>Two things make an engine different from Create's own generators:</p>
 *
 * <ul>
 * <li><b>The fuel sets the output.</b> Both the speed and the stress capacity come from the fuel's
 * recipe, so a tank of LPG and a tank of steam do not drive the same engine the same way. Scrolling
 * the top face only reverses the shaft.</li>
 * <li><b>Fuel burn follows load.</b> An engine idling on a lightly-loaded network sips; one running
 * at capacity drinks. Idle draw floors at 30%, so leaving engines running still costs something.</li>
 * </ul>
 *
 * <p>Where the fuel is <em>kept</em> is the one thing a subclass changes. This class holds a fluid
 * tank; {@link GasTurbineBlockEntity} holds a Mekanism chemical tank instead, because gases belong
 * in a tube. Everything else - load, burn rate, stress, the dial - is shared, so the four hooks
 * below ({@link #addFuelBehaviours}, {@link #hasFuel}, {@link #drainFuel},
 * {@link #appendFuelTooltip}) are all a fuel store has to answer for.</p>
 */
public class FuelEngineBlockEntity extends GeneratingKineticBlockEntity {

	public SmartFluidTankBehaviour tank;
	/**
	 * Which way round the shaft turns. The magnitude comes from the fuel, not from here.
	 *
	 * <p>Create already has exactly this control - {@code RotationDirection} is what the Windmill
	 * Bearing and the Steam Engine put on their own dials - so this reuses it rather than inventing a
	 * two-position number. That also means the icons and the translated names come for free.</p>
	 */
	public ScrollOptionBehaviour<RotationDirection> movementDirection;
	public EngineFuelRecipe currentFuel;

	/** Network load, 0..1. Drives both fuel burn and the goggle readout. */
	public float load;
	public float consumption;

	/**
	 * Redstone governor, 1 at no signal down to 0 at full signal. Scales the RPM the engine actually
	 * puts out, so a comparator or lever can throttle an engine without touching its dial.
	 */
	public float speedModulator = 1.0f;

	private float consumptionCounter;

	@OnlyIn(Dist.CLIENT)
	private EngineSoundInstance soundInstance;

	public FuelEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	private FuelEngineBlock block() {
		return getBlockState().getBlock() instanceof FuelEngineBlock engine ? engine : null;
	}

	protected CKRecipeTypes fuelRecipeType() {
		FuelEngineBlock block = block();
		return block == null ? CKRecipeTypes.GASOLINE_ENGINE_FUEL : block.getFuelRecipeType();
	}

	/** Two litres unless the block says otherwise, whichever kind of tank ends up holding it. */
	protected int fuelCapacity() {
		FuelEngineBlock block = block();
		return block == null ? 2000 : block.getTankCapacity();
	}

	/** The fluid tank. Overridden to nothing by an engine that stores its fuel some other way. */
	protected void addFuelBehaviours(List<BlockEntityBehaviour> behaviours) {
		tank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.TYPE, this, 1, fuelCapacity(), true);
		tank.whenFluidUpdates(this::fluidUpdate);
		behaviours.add(tank);
	}

	/** Whether there is anything left to burn. */
	protected boolean hasFuel() {
		return tank != null && !tank.isEmpty();
	}

	/** Take that many millibuckets out of the store. */
	protected void drainFuel(int amount) {
		if (tank != null)
			tank.getPrimaryHandler()
				.drain(amount, FluidAction.EXECUTE);
	}

	/**
	 * What the goggles say the engine is running on. Returns whether it actually added anything - an
	 * empty tank adds nothing, and the caller has to know that to avoid claiming otherwise (Create's
	 * own {@code GoggleOverlayRenderer} trusts that claim literally enough to crash on it).
	 */
	protected boolean appendFuelTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		return tank != null && containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability());
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		addFuelBehaviours(behaviours);

		movementDirection = new CKScrollOptionBehaviour<>(RotationDirection.class,
			CreateLang.translateDirect("contraptions.windmill.rotation_direction"), this,
			new SpeedValueBoxTransform());
		movementDirection.withCallback($ -> updateGeneratedRotation());
		behaviours.add(movementDirection);
	}

	@Override
	public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
		super.updateFromNetwork(maxStress, currentStress, networkSize);
		load = maxStress <= 0 ? 0 : currentStress / maxStress;
		sendData();
	}

	/** Look up whatever is in the tank against this engine's fuel list. */
	public void fluidUpdate() {
		if (level == null)
			return;
		FluidStack fluid = tank.getPrimaryHandler()
			.getFluidInTank(0);

		if (fluid.isEmpty()) {
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
			.filter(recipe -> recipe.match(fluid))
			.findAny();

		if (match.isEmpty())
			return;
		currentFuel = match.get();
		updateGeneratedRotation();
	}

	/**
	 * The engine note tracks both how hard it is working and how fast it is turning, so a loaded
	 * network sounds different from an idling one even at the same RPM.
	 */
	@Override
	@OnlyIn(Dist.CLIENT)
	public void tickAudio() {
		super.tickAudio();

		if (calculateAddedStressCapacity() == 0) {
			if (soundInstance != null)
				soundInstance.cease();
			return;
		}

		if (soundInstance == null || soundInstance.isStopped()) {
			soundInstance = new EngineSoundInstance(loopSound(), this);
			Minecraft.getInstance()
				.getSoundManager()
				.play(soundInstance);
		}

		soundInstance.setPitch(0.5f + load * 0.5f + Mth.abs(getSpeed() / 256f) * 0.5f);
		soundInstance.setVolume(0.15f + Mth.abs(getSpeed() / 256f) * 0.1f);
	}

	/** The turbine whines; the gasoline engine chugs. */
	@OnlyIn(Dist.CLIENT)
	private SoundEvent loopSound() {
		return fuelRecipeType() == CKRecipeTypes.TURBINE_FUEL ? CKSoundEvents.TURBINE.get()
			: CKSoundEvents.ENGINE.get();
	}

	public float getConsumption() {
		if (currentFuel == null)
			return 0;
		return currentFuel.getConsumptionRate() * (float) Math.max(load, 0.3);
	}

	@Override
	public void tick() {
		super.tick();
		if (currentFuel == null || getSpeed() == 0)
			return;

		consumptionCounter += getConsumption();
		if (consumptionCounter > 1f) {
			drainFuel(Mth.floor(consumptionCounter));
			consumptionCounter = Mth.frac(consumptionCounter);
		}
	}

	/**
	 * Straight from the fuel. Create caches this per block via {@code lastCapacityProvided}, so
	 * {@link #updateGeneratedRotation()} has to run whenever the fuel changes - which it does, from
	 * {@link #fluidUpdate()}.
	 */
	@Override
	public float calculateAddedStressCapacity() {
		if (currentFuel == null || getGeneratedSpeed() == 0)
			return lastCapacityProvided = 0;
		return lastCapacityProvided = currentFuel.getStress();
	}

	@Override
	public float getGeneratedSpeed() {
		if (level == null || level.isClientSide)
			return getSpeed();
		if (currentFuel == null || !hasFuel())
			return 0;
		float sign = movementDirection.get() == RotationDirection.COUNTER_CLOCKWISE ? -1 : 1;
		return convertToDirection(currentFuel.getRpm() * sign * speedModulator,
			getBlockState().getValue(FuelEngineBlock.HORIZONTAL_FACING));
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

		if (getSpeed() != 0) {
			CKLang.translate("gui.engine.load")
				.style(ChatFormatting.GRAY)
				.forGoggles(tooltip);
			IRotate.StressImpact.getFormattedStressText(load)
				.forGoggles(tooltip, 1);

			CKLang.translate("gui.engine.consumption")
				.style(ChatFormatting.GRAY)
				.forGoggles(tooltip);
			LangBuilder millibuckets = CreateLang.translate("generic.unit.millibuckets");
			CKLang.builder()
				.add(CreateLang.number(consumption))
				.add(millibuckets)
				.text("/t")
				.style(ChatFormatting.AQUA)
				.forGoggles(tooltip, 1);
			added = true;
		}

		// Must reflect whether a line was actually added - see appendFuelTooltip's doc. Returning
		// true unconditionally here is exactly what crashed Create's own GoggleOverlayRenderer: an
		// idle, empty engine added nothing, yet claimed it had, and the renderer trusted that enough
		// to remove(-1) from a tooltip list it never touched.
		return appendFuelTooltip(tooltip, isPlayerSneaking) || added;
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.putFloat("Load", load);
		tag.putFloat("Consumption", getConsumption());
		tag.putFloat("SpeedModulator", speedModulator);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		load = tag.getFloat("Load");
		consumption = tag.getFloat("Consumption");
		speedModulator = tag.getFloat("SpeedModulator");
	}

	private class SpeedValueBoxTransform extends ValueBoxTransform.Sided {

		@Override
		public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
			super.rotate(level, pos, state, ms);
			TransformStack.of(ms)
				.rotateZDegrees(-AngleHelper.horizontalAngle(state.getValue(FuelEngineBlock.HORIZONTAL_FACING)));
		}

		@Override
		protected Vec3 getSouthLocation() {
			FuelEngineBlock engine = block();
			return VecHelper.voxelSpace(8, 8, engine == null ? 12.5f : engine.getValueBoxDepth());
		}

		@Override
		protected boolean isSideActive(BlockState state, Direction direction) {
			return direction == Direction.UP;
		}
	}
}
