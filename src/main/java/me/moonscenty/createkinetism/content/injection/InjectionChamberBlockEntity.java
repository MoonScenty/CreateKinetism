package me.moonscenty.createkinetism.content.injection;

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

import me.moonscenty.createkinetism.content.recipe.InjectingRecipe;
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
 * The Injection Chamber's working half, built exactly like the Mechanical Infuser's: a spout that
 * only runs while the shaft turns, working on whatever passes underneath rather than on a slot of
 * its own.
 *
 * <p>The two machines share this base for a reason - Chemical Injection is item plus gas to item,
 * the same shape as Metallurgic Infusing, just with a different chemical and a different housing
 * around it. Where they differ is entirely in {@code InjectionChamberRenderer}: this one has its own
 * cog and its own vacuum-and-pipe housing rather than the infuser's telescoping nozzle.</p>
 */
public class InjectionChamberBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {

	public SmartFluidTankBehaviour tank;
	protected BeltProcessingBehaviour beltProcessing;

	public int processingTicks = -1;
	/**
	 * How long the current process was scheduled to take. {@link #processingTicks} only counts down,
	 * so this is what the renderer needs to turn "ticks left" into "how far through the plunge we
	 * are" for the head's travel animation.
	 */
	public int totalProcessingTicks = -1;
	public boolean sendSplash;

	public InjectionChamberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<InjectionChamberBlockEntity> type) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type,
			(be, context) -> be.tank == null ? null : be.tank.getCapability());
	}

	/** The housing pokes only slightly past the block on every side - nothing like the infuser's
	 *  two-block nozzle - so a modest pad is all the moving cog and the fluid need. */
	@Override
	protected AABB createRenderBoundingBox() {
		return super.createRenderBoundingBox().inflate(0.25);
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

	/** Nothing happens on a stalled shaft, checked on both belt callbacks. */
	private boolean canProcess() {
		return getSpeed() != 0 && !getCurrentFluidInTank().isEmpty();
	}

	/**
	 * The recipe that the item below and the gas in the tank together satisfy. Both halves are
	 * checked here rather than in the recipe's own {@code matches}: the belt hands us only the item,
	 * so the gas is matched against our own tank.
	 */
	private Optional<InjectingRecipe> getMatchingRecipe(ItemStack stack) {
		if (level == null)
			return Optional.empty();
		SingleRecipeInput input = new SingleRecipeInput(stack);
		FluidStack available = getCurrentFluidInTank();
		for (RecipeHolder<InjectingRecipe> holder : level.getRecipeManager()
			.getAllRecipesFor(CKRecipeTypes.INJECTING.<RecipeInput, InjectingRecipe>getType())) {
			InjectingRecipe recipe = holder.value();
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
		// cargo underneath a working chamber.
		return getMatchingRecipe(transported.stack).isPresent() ? ProcessingResult.HOLD
			: ProcessingResult.PASS;
	}

	protected ProcessingResult whenItemHeld(TransportedItemStack transported,
		TransportedItemStackHandlerBehaviour handler) {
		if (processingTicks != -1 && processingTicks != 5)
			return ProcessingResult.HOLD;
		if (!canProcess())
			return ProcessingResult.PASS;

		Optional<InjectingRecipe> match = getMatchingRecipe(transported.stack);
		if (match.isEmpty())
			return ProcessingResult.PASS;
		InjectingRecipe recipe = match.get();

		if (processingTicks == -1) {
			processingTicks = Math.max(recipe.getProcessingDuration(), 10);
			totalProcessingTicks = processingTicks;
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
			.subtract(0, 6 / 16f, 0);
		ParticleOptions particle = FluidFX.getFluidParticle(fluid);
		level.addAlwaysVisibleParticle(particle, vec.x, vec.y, vec.z, 0, -.1f, 0);
	}

	/** The gas landing on the item directly below - one block down, not two, unlike the infuser. */
	private void spawnSplash(FluidStack fluid) {
		if (isVirtual() || fluid.isEmpty())
			return;
		Vec3 vec = VecHelper.getCenterOf(worldPosition)
			.subtract(0, 1 - 5 / 16f, 0);
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
		compound.putInt("TotalProcessingTicks", totalProcessingTicks);
		if (sendSplash && clientPacket) {
			compound.putBoolean("Splash", true);
			sendSplash = false;
		}
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		processingTicks = compound.getInt("ProcessingTicks");
		totalProcessingTicks = compound.getInt("TotalProcessingTicks");
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
