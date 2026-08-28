package me.moonscenty.createkinetism.content.oil;

import java.util.List;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>The crank wheel that drives the walking beam. It needs a real 32 RPM before it will turn at
 * all, and above that its visible speed saturates - a pumpjack is a slow machine no matter how hard
 * you drive it, so over-speeding the shaft buys you nothing here.</p>
 */
public class PumpjackCrankBlockEntity extends KineticBlockEntity {

	public static final float MINIMUM_SPEED = 32f;

	public float angle;
	public LerpedFloat visualSpeed = LerpedFloat.linear();

	public PumpjackCrankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		setLazyTickRate(5);
		visualSpeed.chase(0f, 1 / 128f, LerpedFloat.Chaser.EXP);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	@Override
	public void tick() {
		super.tick();

		// Saturating curve: doubling the RPM stops buying strokes long before the shaft gives out.
		float targetSpeed = 32 * (1.0f - (float) Math.exp(-Mth.abs(getSpeed()) / 128.0f));
		if (Mth.abs(getSpeed()) < MINIMUM_SPEED)
			targetSpeed = 0;

		visualSpeed.updateChaseTarget(targetSpeed);
		visualSpeed.tickChaser();
		angle += visualSpeed.getValue() * 6 / 20f;
		angle %= 360;
	}

	@Override
	public boolean isSpeedRequirementFulfilled() {
		return Mth.abs(getSpeed()) >= MINIMUM_SPEED;
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		sendData();
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(compound, registries, clientPacket);
		compound.putFloat("Angle", angle);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		angle = compound.getFloat("Angle");
	}
}
