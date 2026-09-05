package me.moonscenty.createkinetism.content.multimeter;

import java.util.List;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import com.simibubi.create.content.kinetics.gauge.GaugeBlockEntity;
import com.simibubi.create.content.kinetics.gauge.SpeedGaugeBlockEntity;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;

import me.moonscenty.createkinetism.foundation.CKLang;

import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Two needles on one face: the inherited dial reads speed, the second reads stress.
 *
 * <p>{@link GaugeBlockEntity} already carries one dial - target, smoothed state, previous state and
 * colour - so that one is kept as the speed needle and a second set is added beside it. Both halves
 * compute exactly what Create's own gauges compute: the speed curve is
 * {@link SpeedGaugeBlockEntity#getDialTarget}, and the stress fraction comes out of
 * {@link #updateFromNetwork}, which every kinetic block entity is handed by its network.</p>
 *
 * <p>The two are driven by different events. Speed changes when this block's own rotation changes;
 * stress changes whenever anything anywhere on the network is added, removed or loaded, without this
 * block's speed moving at all. So both are recomputed on a speed change, and the stress half again on
 * every network update.</p>
 */
public class MultimeterBlockEntity extends GaugeBlockEntity {

	/** The second needle. The first is {@code dialTarget}/{@code dialState}, inherited. */
	public float stressDialTarget;
	public float stressDialState;
	public float prevStressDialState;
	public int stressColor;

	public MultimeterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void tick() {
		// Moves the speed needle, among everything else a kinetic block entity does.
		super.tick();

		prevStressDialState = stressDialState;
		stressDialState += (stressDialTarget - stressDialState) * .125f;
		// Create's own overshoot wobble past the end of the scale, so a pegged needle looks strained
		// rather than parked.
		if (stressDialState > 1 && level.random.nextFloat() < 1 / 2f)
			stressDialState -= (stressDialState - 1) * level.random.nextFloat();
	}

	/** The speed half, and a nudge to the stress half - a stopped network reads zero stress. */
	@Override
	public void onSpeedChanged(float prevSpeed) {
		super.onSpeedChanged(prevSpeed);

		float rpm = Math.abs(getSpeed());
		dialTarget = SpeedGaugeBlockEntity.getDialTarget(rpm);
		color = Color.mixColors(SpeedLevel.of(rpm)
			.getColor(), 0xffffff, .25f);

		if (getSpeed() == 0) {
			stressDialTarget = 0;
			sendData();
			setChanged();
			return;
		}

		updateFromNetwork(capacity, stress, getOrCreateNetwork().getSize());
	}

	/**
	 * The stress half. Create hands this to every kinetic block entity on the network, so a
	 * Stressometer is simply one that listens - and so is this.
	 */
	@Override
	public void updateFromNetwork(float maxStress, float currentStress, int networkSize) {
		super.updateFromNetwork(maxStress, currentStress, networkSize);

		if (!StressImpact.isEnabled() || maxStress == 0)
			stressDialTarget = 0;
		else if (isOverStressed())
			stressDialTarget = 1.125f;
		else
			stressDialTarget = currentStress / maxStress;

		if (stressDialTarget > 0) {
			if (stressDialTarget < .5f)
				stressColor = Color.mixColors(0x00FF00, 0xFFFF00, stressDialTarget * 2);
			else if (stressDialTarget < 1)
				stressColor = Color.mixColors(0xFFFF00, 0xFF0000, stressDialTarget * 2 - 1);
			else
				stressColor = 0xFF0000;
		}

		sendData();
		setChanged();
	}

	@Override
	public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putFloat("StressValue", stressDialTarget);
		compound.putInt("StressColor", stressColor);
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		stressDialTarget = compound.getFloat("StressValue");
		stressColor = compound.getInt("StressColor");
	}

	/** Both readouts under one header, in the order the needles sit. */
	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		CKLang.translate("gui.multimeter.info_header")
			.forGoggles(tooltip);

		CreateLang.translate("gui.speedometer.title")
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip);
		SpeedLevel.getFormattedSpeedText(getSpeed(), isOverStressed())
			.forGoggles(tooltip);

		if (!StressImpact.isEnabled())
			return true;

		double networkCapacity = capacity;
		double fraction = stress / (networkCapacity == 0 ? 1 : networkCapacity);

		CreateLang.translate("gui.stressometer.title")
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip);

		if (getTheoreticalSpeed() == 0) {
			CreateLang.text(TooltipHelper.makeProgressBar(3, 0))
				.translate("gui.stressometer.no_rotation")
				.style(ChatFormatting.DARK_GRAY)
				.forGoggles(tooltip);
			return true;
		}

		StressImpact.getFormattedStressText(fraction)
			.forGoggles(tooltip);
		CreateLang.translate("gui.stressometer.capacity")
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip);

		double remaining = networkCapacity - stress;
		LangBuilder su = CreateLang.translate("generic.unit.stress");
		LangBuilder line = CreateLang.number(remaining)
			.add(su)
			.style(StressImpact.of(fraction)
				.getRelativeColor());
		if (remaining != networkCapacity)
			line.text(ChatFormatting.GRAY, " / ")
				.add(CreateLang.number(networkCapacity)
					.add(su)
					.style(ChatFormatting.DARK_GRAY));
		line.forGoggles(tooltip, 1);

		return true;
	}
}
