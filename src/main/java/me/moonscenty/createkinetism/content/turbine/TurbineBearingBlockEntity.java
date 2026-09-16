package me.moonscenty.createkinetism.content.turbine;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * The generator half of a Turbine: turns the steam its turbine used over the last second into speed and
 * stress capacity. {@value TurbineCasingBlockEntity#SPEED} RPM whenever any steam went through,
 * {@value TurbineCasingBlockEntity#SU_PER_MB} SU per mB a tick.
 *
 * <p>It reads the turbine rather than the other way round, so a bearing placed under an already-built
 * turbine starts on its own; the turbine also pokes it when its flow changes, so the output follows
 * without waiting for the next lazy tick.</p>
 */
public class TurbineBearingBlockEntity extends GeneratingKineticBlockEntity {

	/** The steam a tick this bearing is currently turning into rotation. */
	private float flow;

	public TurbineBearingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** The turbine this bearing drives out of, if it is under the middle of one's floor. */
	@Nullable
	public TurbineCasingBlockEntity getTurbine() {
		if (level == null
			|| !(level.getBlockEntity(worldPosition.above()) instanceof TurbineCasingBlockEntity casing))
			return null;
		TurbineCasingBlockEntity controller = casing.getControllerBE();
		if (controller == null || !controller.bearingPos()
			.equals(worldPosition))
			return null;
		return controller;
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		updateFromTurbine();
	}

	public void updateFromTurbine() {
		if (level == null || level.isClientSide)
			return;
		TurbineCasingBlockEntity turbine = getTurbine();
		float newFlow = turbine == null || !turbine.isTurbine() ? 0 : turbine.getAverageFlow();
		if (newFlow == flow)
			return;
		flow = newFlow;
		updateGeneratedRotation();
		setChanged();
	}

	@Override
	public float getGeneratedSpeed() {
		return flow > 0 ? TurbineCasingBlockEntity.SPEED : 0;
	}

	/** Create multiplies this by the speed, so dividing by it here leaves exactly the SU wanted. */
	@Override
	public float calculateAddedStressCapacity() {
		float capacity = flow * TurbineCasingBlockEntity.SU_PER_MB / TurbineCasingBlockEntity.SPEED;
		lastCapacityProvided = capacity;
		return capacity;
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putFloat("Flow", flow);
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		flow = compound.getFloat("Flow");
		super.read(compound, registries, clientPacket);
	}
}
