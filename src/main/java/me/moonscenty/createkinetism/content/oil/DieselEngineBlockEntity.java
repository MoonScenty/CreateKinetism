package me.moonscenty.createkinetism.content.oil;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity.RotationDirection;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import me.moonscenty.createkinetism.content.recipe.EngineFuelRecipe;
import me.moonscenty.createkinetism.foundation.CKLang;
import me.moonscenty.createkinetism.foundation.CKScrollOptionBehaviour;
import me.moonscenty.createkinetism.config.CKStress;
import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;
import me.moonscenty.createkinetism.registry.CKSoundEvents;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup;
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
 * <p>Drives a Powered Shaft rather than turning itself. The fuel's recipe sets both the speed and
 * the capacity handed to the shaft, and the dial only reverses it; ganging several engines onto one
 * shaft still adds their output.</p>
 *
 * <p>Fuel burn tracks the shaft's network load with a 50% floor - a diesel idles thirstier than the
 * gasoline engine, which is the trade for its output.</p>
 */
public class DieselEngineBlockEntity extends SteamEngineBlockEntity implements IHaveGoggleInformation {

	public SmartFluidTankBehaviour tank;
	// The dial itself is SteamEngineBlockEntity's own protected movementDirection field, which this
	// class had left null by never calling super.addBehaviours(). Registering it here rather than
	// keeping a parallel field means anything inherited that reads it now sees the real value.
	public EngineFuelRecipe currentFuel;

	public boolean redstoneDisabled;
	public float load;
	public float consumption;

	private float consumptionCounter;

	@OnlyIn(Dist.CLIENT)
	private EngineSoundInstance soundInstance;

	public DieselEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		setLazyTickRate(10);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		tank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.TYPE, this, 1, 4000, true);
		tank.whenFluidUpdates(this::fluidUpdate);
		behaviours.add(tank);

		movementDirection = new CKScrollOptionBehaviour<>(RotationDirection.class,
			CreateLang.translateDirect("contraptions.windmill.rotation_direction"), this, new DialBoxTransform());
		movementDirection.withCallback($ -> updateRotation());
		behaviours.add(movementDirection);
	}

	public void fluidUpdate() {
		if (level == null)
			return;
		FluidStack fluid = tank.getPrimaryHandler()
			.getFluidInTank(0);

		if (fluid.isEmpty()) {
			if (currentFuel != null) {
				currentFuel = null;
				updateRotation();
			}
			return;
		}
		if (currentFuel != null)
			return;

		Optional<EngineFuelRecipe> match = level.getRecipeManager()
			.getAllRecipesFor(CKRecipeTypes.DIESEL_ENGINE_FUEL.<RecipeInput, EngineFuelRecipe>getType())
			.stream()
			.map(RecipeHolder::value)
			.filter(recipe -> recipe.match(fluid))
			.findAny();
		if (match.isEmpty())
			return;

		currentFuel = match.get();
		updateRotation();
	}

	/** Tell the shaft what we are driving it with, or that we have stopped. */
	public void updateRotation() {
		PoweredShaftBlockEntity shaft = getShaft();
		if (shaft == null)
			return;

		BlockState blockState = getBlockState();
		if (!CKBlocks.DIESEL_ENGINE.has(blockState))
			return;

		if (currentFuel == null || redstoneDisabled) {
			shaft.update(worldPosition, 0, 0);
			return;
		}

		Direction facing = SteamEngineBlock.getFacing(blockState);
		BlockState shaftState = shaft.getBlockState();
		Axis targetAxis = shaftState.getBlock() instanceof IRotate rotate ? rotate.getRotationAxis(shaftState) : Axis.X;
		boolean verticalTarget = targetAxis == Axis.Y;

		if (facing.getAxis() == Axis.Y)
			facing = blockState.getValue(SteamEngineBlock.FACING);

		int rotationSpeed = verticalTarget ? 1 : (int) GeneratingKineticBlockEntity.convertToDirection(1, facing);
		if (targetAxis == Axis.Z)
			rotationSpeed *= -1;

		// Do not fight a shaft that is already being driven the other way.
		float shaftSpeed = shaft.getTheoreticalSpeed();
		if (shaft.hasSource() && shaftSpeed != 0 && rotationSpeed != 0 && (shaftSpeed > 0) != (rotationSpeed > 0))
			rotationSpeed *= -1;

		shaft.update(worldPosition, rotationSpeed * (movementDirection.get() == RotationDirection.COUNTER_CLOCKWISE ? -1 : 1), fuelEfficiency());
	}

	/**
	 * Create's Powered Shaft does not take a free RPM. It quantises to 16/32/48/64 by turning the
	 * efficiency we hand it into one of four tiers, and derives its capacity from the same number
	 * against the engine block's registered stress value. One knob, two outputs - so the diesel can
	 * honour a fuel's {@code rpm} by picking the nearest tier, but not its {@code stress}: that stays
	 * the block's configured capacity. The two engines that generate rotation directly, the gasoline
	 * engine and the turbine, take both figures straight from the recipe.
	 */
	private float fuelEfficiency() {
		if (currentFuel == null)
			return 0;
		return switch (Mth.clamp(Math.round(currentFuel.getRpm() / 16f), 1, 4)) {
			case 1 -> 0.2f;
			case 2 -> 0.3f;
			case 3 -> 0.75f;
			default -> 1.0f;
		};
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		if (level.isClientSide) {
			CatnipServices.PLATFORM.executeOnClientOnly(() -> this::tickEngineAudio);
			return;
		}

		PoweredShaftBlockEntity shaft = getShaft();
		if (shaft == null)
			return;

		float previousLoad = load;
		load = networkLoadOf(shaft);
		if (previousLoad != load)
			sendData();

		if (currentFuel == null || shaft.getGeneratedSpeed() == 0)
			return;

		consumptionCounter += getConsumption();
		if (consumptionCounter > 1f) {
			tank.getPrimaryHandler()
				.drain(Mth.floor(consumptionCounter), FluidAction.EXECUTE);
			consumptionCounter = Mth.frac(consumptionCounter);
		}
	}

	/**
	 * How hard the shaft's network is working, 0..1. Read off the network rather than the block
	 * entity so no access widening is needed.
	 */
	private static float networkLoadOf(PoweredShaftBlockEntity shaft) {
		if (!shaft.hasNetwork())
			return 0;
		KineticNetwork network = shaft.getOrCreateNetwork();
		float capacity = network.calculateCapacity();
		if (capacity <= 0)
			return 0;
		float value = network.calculateStress() / capacity;
		return Float.isNaN(value) ? 0 : value;
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (level != null && !level.isClientSide)
			updateRotation();
	}

	/**
	 * SteamEngineBlockEntity has no tickAudio hook of its own, so this is driven from
	 * {@link #tick()} behind a client-only dispatch.
	 */
	@OnlyIn(Dist.CLIENT)
	private void tickEngineAudio() {
		PoweredShaftBlockEntity shaft = getShaft();

		if (shaft == null || shaft.getSpeed() == 0) {
			if (soundInstance != null && !soundInstance.isStopped())
				soundInstance.cease();
			return;
		}

		if (soundInstance == null || soundInstance.isStopped()) {
			soundInstance = new EngineSoundInstance(CKSoundEvents.DIESEL.get(), this);
			Minecraft.getInstance()
				.getSoundManager()
				.play(soundInstance);
		}

		soundInstance.setPitch(0.4f + load * 0.2f + Mth.abs(shaft.getSpeed() / 256f) * 0.2f);
		soundInstance.setVolume(0.1f + Mth.abs(shaft.getSpeed() / 256f) * 0.07f);
	}

	/**
	 * The crank angle the renderer animates from - taken from the shaft we drive, so the piston stays
	 * locked to the rotation the player can see. Null when there is nothing to read.
	 */
	@Override
	@Nullable
	@OnlyIn(Dist.CLIENT)
	public Float getTargetAngle() {
		BlockState blockState = getBlockState();
		if (!CKBlocks.DIESEL_ENGINE.has(blockState))
			return null;

		PoweredShaftBlockEntity shaft = getShaft();
		if (shaft == null)
			return null;

		Direction facing = SteamEngineBlock.getFacing(blockState);
		Axis facingAxis = facing.getAxis();
		Axis axis = KineticBlockEntityRenderer.getRotationAxisOf(shaft);
		if (axis == facingAxis)
			return null;

		float angle = KineticBlockEntityRenderer.getAngleForBe(shaft, shaft.getBlockPos(), axis);
		if (axis.isHorizontal()
			&& (facingAxis == Axis.X ^ facing.getAxisDirection() == Direction.AxisDirection.POSITIVE))
			angle *= -1;
		if (axis == Axis.X && facing == Direction.DOWN)
			angle *= -1;
		return angle;
	}

	public float getConsumption() {
		if (currentFuel == null)
			return 0;
		return currentFuel.getConsumptionRate() * (float) Math.max(load, 0.5);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		PoweredShaftBlockEntity shaft = getShaft();
		if (shaft != null) {
			shaft.addToEngineTooltip(tooltip, isPlayerSneaking);
			if (shaft.getGeneratedSpeed() != 0) {
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
			}
		}
		containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability());
		return true;
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.putFloat("Load", load);
		tag.putFloat("Consumption", getConsumption());
		tag.putBoolean("RedstoneDisabled", redstoneDisabled);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		load = tag.getFloat("Load");
		consumption = tag.getFloat("Consumption");
		redstoneDisabled = tag.getBoolean("RedstoneDisabled");
	}


	/**
	 * The dial sits on the engine's back - the flat end, opposite the housing that juts out.
	 *
	 * <p>Not {@code getFacing().getOpposite()}: that is {@code getConnectedDirection}, which points
	 * along the shaft, so its opposite is whatever the engine is bolted to - the floor, for one
	 * standing on the ground. Front and back here are the separate horizontal {@code FACING}
	 * property.</p>
	 *
	 * <p>Which end is the back comes off the model. Its two horizontal ends are not alike: one juts
	 * out to {@code z 17.8}, past the block boundary, while the other is a flat bump flush at
	 * {@code z 0}. In the unrotated variant - {@code face=floor,facing=north}, which carries no
	 * rotation at all - that flat end is the north face, and north is what {@code FACING} reads. The
	 * moving parts are no help in deciding: the renderer drives the piston, linkage and connector
	 * along the shaft axis, so both ends stay still.</p>
	 *
	 * <p>Create's own {@code SteamEngineValueBox} could not do this. It is built for the Steam Engine,
	 * which is lopsided in a different way and always has a boiler behind it, so it picks a
	 * <em>side</em> face by testing which its model leaves recessed - a test our body gives no
	 * purchase on.</p>
	 */
	private class DialBoxTransform extends ValueBoxTransform.Sided {

		/**
		 * The underside case is placed directly rather than through {@code getSouthLocation}.
		 *
		 * <p>{@code Sided} rotates the south location by the face the box lands on and nothing else,
		 * so on the bottom face a fixed nudge would point the same way in the world no matter which
		 * way the engine is turned - head-ward for one {@code FACING} and tail-ward for another.
		 * Building the point from the facing's own step vector keeps it a voxel towards the head for
		 * all four.</p>
		 */
		@Override
		public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
			Direction facing = SteamEngineBlock.getFacing(state);
			if (facing.getAxis()
				.isHorizontal())
				return VecHelper.voxelSpace(8 + facing.getStepX(), 0.5, 8 + facing.getStepZ());
			return super.getLocalOffset(level, pos, state);
		}

		@Override
		protected boolean isSideActive(BlockState state, Direction side) {
			// Floor and ceiling mountings leave the horizontal FACING face free, and that is the flat
			// end of the model. A wall mounting does not: there getFacing IS FACING, so that face is
			// where the shaft leaves. The underside is the one that stays clear.
			return SteamEngineBlock.getFacing(state)
				.getAxis()
				.isVertical() ? side == state.getValue(SteamEngineBlock.FACING) : side == Direction.DOWN;
		}

		/**
		 * A voxel towards the head and half a voxel into the casing, so the box reads as set into the
		 * engine rather than stuck on the flat of it.
		 *
		 * <p>The head is not always the same way up. {@code Sided} rotates this point by the face the
		 * box lands on and nothing else, but the ceiling variant draws the whole model flipped
		 * ({@code x: 180}) - so a fixed y that hugs the head on one mounting sits two voxels off it on
		 * the other. Mirroring about the block centre follows the model instead of fighting it.</p>
		 */
		@Override
		protected Vec3 getSouthLocation() {
			// getFacing is UP for the floor mounting - the one you place looking down at the ground.
			return VecHelper.voxelSpace(8, SteamEngineBlock.getFacing(getBlockState()) == Direction.UP ? 9 : 7,
				15.5f);
		}
	}
}
