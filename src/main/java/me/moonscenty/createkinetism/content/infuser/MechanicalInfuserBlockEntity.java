package me.moonscenty.createkinetism.content.infuser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.FluidFX;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;

import me.moonscenty.createkinetism.content.recipe.InfusingRecipe;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.createmod.catnip.math.VecHelper;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The infuser's working half: a spout that has to be turned.
 *
 * <p>Create's spout is passive, so everything about paying for it is new. {@link KineticBlockEntity}
 * supplies the shaft, the stress impact and the goggle readout; the tank and the belt hook are the
 * spout's, because the machine still works on whatever passes underneath rather than on slots of its
 * own.</p>
 *
 * <p>The infusion is a fluid rather than a held item, which is what makes a spout the right shape for
 * this machine: the Oxidation Vat turns redstone or coal into an infusion fluid, a pipe brings it
 * here, and the nozzle drips it onto the item below.</p>
 */
public class MechanicalInfuserBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {

	public static final int PROCESSING_TIME = 20;

	public SmartFluidTankBehaviour tank;
	protected BeltProcessingBehaviour beltProcessing;

	public int processingTicks = -1;
	public boolean sendSplash;

	public MechanicalInfuserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<MechanicalInfuserBlockEntity> type) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type,
			(be, context) -> be.tank == null ? null : be.tank.getCapability());
	}

	/** The nozzle reaches down to the depot two blocks below. */
	@Override
	protected AABB createRenderBoundingBox() {
		return super.createRenderBoundingBox().expandTowards(0, -2, 0);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);

		tank = SmartFluidTankBehaviour.single(this, 1000);
		behaviours.add(tank);

		beltProcessing = new BeltProcessingBehaviour(this).whenItemEnters(this::onItemReceived)
			.whileItemHeld(this::whenItemHeld);
		behaviours.add(beltProcessing);
	}

	public FluidStack getCurrentFluidInTank() {
		return tank.getPrimaryHandler()
			.getFluid();
	}

	/**
	 * Nothing happens on a stalled shaft. This is the whole difference from Create's spout, and it is
	 * checked on both belt callbacks so an item is neither held nor consumed while the machine is
	 * stopped.
	 */
	private boolean canProcess() {
		return getSpeed() != 0 && !getCurrentFluidInTank().isEmpty();
	}

	/**
	 * The recipe that the item below and the fluid in the tank together satisfy.
	 *
	 * <p>Both halves are checked here rather than in the recipe's own {@code matches}: the belt hands
	 * us only the item, so the infusion is matched against our own tank.</p>
	 */
	private Optional<InfusingRecipe> getMatchingRecipe(ItemStack stack) {
		if (level == null)
			return Optional.empty();
		SingleRecipeInput input = new SingleRecipeInput(stack);
		FluidStack available = getCurrentFluidInTank();
		for (RecipeHolder<InfusingRecipe> holder : level.getRecipeManager()
			.getAllRecipesFor(CKRecipeTypes.INFUSING.<RecipeInput, InfusingRecipe>getType())) {
			InfusingRecipe recipe = holder.value();
			if (recipe.matches(input, level) && recipe.matchesFluid(available))
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
			processingTicks = Math.max(recipe.getProcessingDuration(), 10);
			notifyUpdate();
			AllSoundEvents.SPOUTING.playOnServer(level, worldPosition, 0.75f,
				0.9f + 0.2f * (float) Math.random());
			return ProcessingResult.HOLD;
		}

		// Process finished
		FluidStack fluid = getCurrentFluidInTank();
		int cost = recipe.getRequiredFluid()
			.amount();
		tank.getPrimaryHandler()
			.setFluid(FluidHelper.copyStackWithAmount(fluid, fluid.getAmount() - cost));

		transported.stack.shrink(1);
		transported.clearFanProcessingData();

		List<TransportedItemStack> outList = new ArrayList<>();
		TransportedItemStack result = transported.copy();
		result.stack = recipe.getResultItem()
			.copy();
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
		if (processingTicks >= 0)
			processingTicks--;
		if (processingTicks >= 8 && level != null && level.isClientSide)
			spawnProcessingParticles(tank.getPrimaryTank()
				.getRenderedFluid());
	}

	private void spawnProcessingParticles(FluidStack fluid) {
		if (isVirtual() || fluid.isEmpty())
			return;
		Vec3 vec = VecHelper.getCenterOf(worldPosition)
			.subtract(0, 8 / 16f, 0);
		ParticleOptions particle = FluidFX.getFluidParticle(fluid);
		level.addAlwaysVisibleParticle(particle, vec.x, vec.y, vec.z, 0, -.1f, 0);
	}

	/** The infusion landing on the item, two blocks down. */
	private void spawnSplash(FluidStack fluid) {
		if (isVirtual() || fluid.isEmpty())
			return;
		Vec3 vec = VecHelper.getCenterOf(worldPosition)
			.subtract(0, 2 - 5 / 16f, 0);
		ParticleOptions particle = FluidFX.getFluidParticle(fluid);
		for (int i = 0; i < 20; i++) {
			Vec3 m = VecHelper.offsetRandomly(Vec3.ZERO, level.random, 0.125f);
			m = new Vec3(m.x, Math.abs(m.y), m.z);
			level.addAlwaysVisibleParticle(particle, vec.x, vec.y, vec.z, m.x, m.y, m.z);
		}
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putInt("ProcessingTicks", processingTicks);
		if (sendSplash && clientPacket) {
			compound.putBoolean("Splash", true);
			sendSplash = false;
		}
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		processingTicks = compound.getInt("ProcessingTicks");
		super.read(compound, registries, clientPacket);
		if (clientPacket && compound.contains("Splash"))
			spawnSplash(tank.getPrimaryTank()
				.getRenderedFluid());
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		return containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability());
	}
}
