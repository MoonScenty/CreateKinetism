package me.moonscenty.createkinetism.content.oil;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.motor.KineticScrollValueBehaviour;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import me.moonscenty.createkinetism.content.recipe.EngineFuelRecipe;
import me.moonscenty.createkinetism.foundation.CKLang;
import me.moonscenty.createkinetism.config.CKStress;
import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;
import me.moonscenty.createkinetism.registry.CKSoundEvents;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.math.VecHelper;
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
 * <p>Drives a Powered Shaft rather than turning itself. The RPM dial sets the direction and
 * magnitude handed to the shaft; the shaft works out capacity from this block's registered stress
 * value, so ganging several engines onto one shaft adds their output.</p>
 *
 * <p>Fuel burn tracks the shaft's network load with a 50% floor - a diesel idles thirstier than the
 * gasoline engine, which is the trade for its output.</p>
 */
public class DieselEngineBlockEntity extends SteamEngineBlockEntity implements IHaveGoggleInformation {

	public SmartFluidTankBehaviour tank;
	public ScrollValueBehaviour targetSpeed;
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

		targetSpeed = new KineticScrollValueBehaviour(
			CreateLang.translateDirect("kinetics.speed_controller.rotation_speed"), this, new DialBoxTransform());
		int maxRpm = CKStress.getMaxRpm(getBlockState().getBlock());
		targetSpeed.between(-maxRpm, maxRpm);
		targetSpeed.value = 64;
		targetSpeed.withCallback(i -> updateRotation());
		behaviours.add(targetSpeed);
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

		shaft.update(worldPosition, rotationSpeed * targetSpeed.getValue(), 1.0f);
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

	private class DialBoxTransform extends ValueBoxTransform.Sided {

		@Override
		protected Vec3 getSouthLocation() {
			return VecHelper.voxelSpace(8, 8, 12.5f);
		}

		@Override
		protected boolean isSideActive(BlockState state, Direction direction) {
			return direction == Direction.UP;
		}
	}
}
