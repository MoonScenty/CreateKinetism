package me.moonscenty.createkinetism.content.accumulator;

import java.util.List;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;

import me.moonscenty.createkinetism.foundation.CKLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A stress buffer.
 *
 * <p>Create's stress is a balance, not a battery: a network has a capacity and a load, and the
 * moment load exceeds capacity everything stops. There is no stored quantity to draw on, so an
 * "energy cube" cannot be built by adding capacity out of nowhere - that would be free power.</p>
 *
 * <p>What this block does instead is shift load through time. While the network has spare capacity
 * it applies a <em>positive</em> stress impact, paying real SU to fill {@link #charge}. When the
 * load would otherwise exceed capacity it applies a <em>negative</em> impact, which subtracts from
 * the network's total load and buys the machines enough headroom to keep running until the buffer
 * runs dry. Over a full cycle it is exactly break-even.</p>
 *
 * <p>Working through the stress figure rather than through capacity is also what keeps it safe: it
 * never claims to be a rotation source, so it can never fight the network's real source over speed
 * or direction.</p>
 */
public class KineticAccumulatorBlockEntity extends KineticBlockEntity {

	/** Maximum stored charge, in stress units times ticks. Roughly a minute at the full rate. */
	public static final float MAX_CHARGE = 640_000f;
	/** Fastest charge or discharge, in stress units. */
	public static final float MAX_RATE = 512f;
	/** Share of the network's capacity left alone while charging, so machines keep breathing room. */
	private static final float RESERVE_RATIO = 0.1f;

	private float charge;
	/** Base stress impact per RPM. Positive means charging, negative means feeding the network. */
	private float impact;
	private float lastSyncedCharge;

	public KineticAccumulatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public float getCharge() {
		return charge;
	}

	public int getComparatorOutput() {
		if (charge <= 0)
			return 0;
		return 1 + Mth.floor(charge / MAX_CHARGE * 14);
	}

	@Override
	public float calculateStressApplied() {
		lastStressApplied = impact;
		return impact;
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;

		float speed = Math.abs(getTheoreticalSpeed());

		// What the network actually moved through us last tick, derived rather than remembered so it
		// stays correct when the network speeds up or slows down underneath us.
		float flow = impact * speed;
		charge = Mth.clamp(charge + flow, 0, MAX_CHARGE);

		float desired = 0;
		if (speed >= 1) {
			// Network load excluding our own contribution, so the decision cannot chase itself.
			float load = stress - flow;
			float deficit = load - capacity;

			if (deficit > 0) {
				if (charge > 0)
					desired = -Math.min(Math.min(MAX_RATE, deficit), charge);
			} else if (charge < MAX_CHARGE) {
				float spare = capacity - load - capacity * RESERVE_RATIO;
				if (spare > 0)
					desired = Math.min(Math.min(MAX_RATE, spare), MAX_CHARGE - charge);
			}
		}

		if (Math.abs(desired - flow) < 1f)
			return;

		impact = speed >= 1 ? desired / speed : 0;
		if (hasNetwork())
			getOrCreateNetwork().updateStressFor(this, impact);
		sendData();
		lastSyncedCharge = charge;
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		// Keep the goggle readout roughly live without a packet every tick.
		if (level != null && !level.isClientSide && Math.abs(charge - lastSyncedCharge) > MAX_CHARGE / 64f) {
			lastSyncedCharge = charge;
			sendData();
		}
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putFloat("Charge", charge);
		compound.putFloat("Impact", impact);
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		charge = compound.getFloat("Charge");
		impact = compound.getFloat("Impact");
		lastSyncedCharge = charge;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		CKLang.translate("gui.goggles.kinetic_accumulator")
			.forGoggles(tooltip);

		CKLang.translate("tooltip.accumulator.stored")
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip);
		CreateLang.number(charge)
			.text(" / ")
			.add(CreateLang.number(MAX_CHARGE))
			.style(ChatFormatting.AQUA)
			.forGoggles(tooltip, 1);

		float flow = impact * Math.abs(getTheoreticalSpeed());
		if (flow > 0) {
			CKLang.translate("tooltip.accumulator.charging")
				.style(ChatFormatting.GRAY)
				.forGoggles(tooltip);
			CreateLang.number(flow)
				.translate("generic.unit.stress")
				.style(ChatFormatting.GOLD)
				.forGoggles(tooltip, 1);
		} else if (flow < 0) {
			CKLang.translate("tooltip.accumulator.discharging")
				.style(ChatFormatting.GRAY)
				.forGoggles(tooltip);
			CreateLang.number(-flow)
				.translate("generic.unit.stress")
				.style(ChatFormatting.GREEN)
				.forGoggles(tooltip, 1);
		} else {
			CKLang.translate("tooltip.accumulator.idle")
				.style(ChatFormatting.GRAY)
				.forGoggles(tooltip);
		}

		return true;
	}
}
