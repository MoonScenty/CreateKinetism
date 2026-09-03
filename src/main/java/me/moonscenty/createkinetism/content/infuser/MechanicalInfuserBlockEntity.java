package me.moonscenty.createkinetism.content.infuser;

import java.util.List;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

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
 * <p>The recipe lookup is deliberately not wired yet. What an infusion recipe looks like - whether
 * the infusion is a fluid the way every other chemical in this mod is, or stays an item the way the
 * README currently says - decides the shape of {@link #canProcess} and of the recipe type, and that
 * is the next thing to settle. Until then the machine runs, holds fluid and refuses to process.</p>
 */
public class MechanicalInfuserBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {

	public static final int PROCESSING_TIME = 20;

	public SmartFluidTankBehaviour tank;
	protected BeltProcessingBehaviour beltProcessing;

	public int processingTicks = -1;

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

	protected ProcessingResult onItemReceived(TransportedItemStack transported,
		TransportedItemStackHandlerBehaviour handler) {
		if (handler.blockEntity.isVirtual())
			return ProcessingResult.PASS;
		return canProcess() ? ProcessingResult.HOLD : ProcessingResult.PASS;
	}

	protected ProcessingResult whenItemHeld(TransportedItemStack transported,
		TransportedItemStackHandlerBehaviour handler) {
		if (!canProcess())
			return ProcessingResult.PASS;
		// Recipe matching goes here once the infusion format is settled.
		return ProcessingResult.PASS;
	}

	@Override
	public void tick() {
		super.tick();
		if (processingTicks >= 0)
			processingTicks--;
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putInt("ProcessingTicks", processingTicks);
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		processingTicks = compound.getInt("ProcessingTicks");
		super.read(compound, registries, clientPacket);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		return containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability());
	}
}
