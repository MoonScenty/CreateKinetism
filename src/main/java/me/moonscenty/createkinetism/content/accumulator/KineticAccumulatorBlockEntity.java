package me.moonscenty.createkinetism.content.accumulator;

import java.util.List;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.KineticScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;

import me.moonscenty.createkinetism.content.tool.KineticDisassemblerItem;
import me.moonscenty.createkinetism.foundation.CKLang;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import org.jetbrains.annotations.Nullable;

/**
 * Buys rotation on the shaft, sells it back through the cogwheel.
 *
 * <p>The two are different networks - see
 * {@link me.moonscenty.createkinetism.mixin.RotationPropagatorMixin} - so nothing flows straight
 * through. While the shaft turns, {@link #calculateStressApplied} is charged to that network every
 * tick and banked in {@link #charge}. A redstone signal spends the bank: {@link #getOutputSpeed}
 * starts answering with the dial's setting, the cogwheel on top turns at it, and the bank drains at
 * that speed until the signal drops or it runs dry.</p>
 */
public class KineticAccumulatorBlockEntity extends GeneratingKineticBlockEntity {

	/** Maximum stored charge, in stress units times ticks. */
	public static final float MAX_CHARGE = 640_000f;

	/**
	 * Fastest winding handed to a tool, in stress units times ticks per tick. A full Disassembler
	 * takes about twenty-five seconds, and a full accumulator winds ten of them.
	 */
	public static final int WIND_RATE = 128;

	/** The dial. Create's own, so it counts in real RPM and carries the two direction rows. */
	public KineticScrollValueBehaviour outputSpeed;

	/**
	 * The tool sitting on top. Charge leaves this block two ways - here and through the cogwheel - and
	 * both are honest: it only ever filled up by paying real stress in first.
	 */
	public final ItemStackHandler chargingInv = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return stack.getItem() instanceof KineticDisassemblerItem;
		}

		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
			sendData();
		}
	};

	private float charge;
	private boolean discharging;
	private float lastSyncedCharge;

	public KineticAccumulatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);

		int max = AllConfigs.server().kinetics.maxRotationSpeed.get();
		outputSpeed = new KineticScrollValueBehaviour(CKLang.translate("gui.accumulator.output_speed")
			.component(), this, new OutputDialTransform());
		outputSpeed.between(-max, max);
		outputSpeed.value = 64;
		outputSpeed.withCallback(rpm -> updateOutput());
		behaviours.add(outputSpeed);
	}

	/**
	 * Only the two faces the shaft does not come out of - a dial on the end of an axle would be
	 * unreachable. Position and scale are Create's own, so it sits where a Speed Controller's does.
	 */
	private static class OutputDialTransform extends ValueBoxTransform.Sided {

		@Override
		protected Vec3 getSouthLocation() {
			return VecHelper.voxelSpace(8, 11f, 15.5f);
		}

		@Override
		protected boolean isSideActive(BlockState state, Direction direction) {
			if (direction.getAxis()
				.isVertical())
				return false;
			return state.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS) != direction.getAxis();
		}

		@Override
		public float getScale() {
			return 0.5f;
		}
	}

	/**
	 * What the cogwheel above is turning at. Zero unless the block is actually paying for it, which
	 * is what makes the redstone signal a switch rather than a suggestion.
	 */
	public float getOutputSpeed() {
		return discharging && outputSpeed != null ? outputSpeed.getValue() : 0;
	}

	/**
	 * While discharging this block is a generator, not a consumer.
	 *
	 * <p>That is the only shape Create allows for something that produces rotation without being
	 * driven, and it is what the accumulator has to be: a battery is used precisely when the thing
	 * that charged it has stopped. Charging and discharging never overlap, so it is never both a
	 * source and a load on the same connection.</p>
	 */
	@Override
	public float getGeneratedSpeed() {
		return discharging ? outputSpeed.getValue() : 0;
	}

	/** Paying in only happens while charging - a discharging accumulator takes nothing. */
	@Override
	public float calculateStressApplied() {
		if (discharging) {
			lastStressApplied = 0;
			return 0;
		}
		return super.calculateStressApplied();
	}

	/** And the other way round: it only holds a network up while it is the one turning it. */
	@Override
	public float calculateAddedStressCapacity() {
		if (!discharging) {
			lastCapacityProvided = 0;
			return 0;
		}
		float capacity = (float) BlockStressValues.getCapacity(getBlockState().getBlock());
		lastCapacityProvided = capacity;
		return capacity;
	}

	public float getCharge() {
		return charge;
	}

	public boolean isDischarging() {
		return discharging;
	}

	public int getComparatorOutput() {
		if (charge <= 0)
			return 0;
		return 1 + Mth.floor(charge / MAX_CHARGE * 14);
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;

		windHeldTool();
		bank();
		spend();

		if (Math.abs(charge - lastSyncedCharge) > MAX_CHARGE / 64f) {
			lastSyncedCharge = charge;
			sendData();
		}
	}

	/** The shaft side. Always on while it turns - there is no idling to save power. */
	private void bank() {
		float speed = Math.abs(getTheoreticalSpeed());
		if (speed < 1)
			return;
		charge = Math.min(charge + calculateStressApplied() * speed, MAX_CHARGE);
		setChanged();
	}

	/** The cogwheel side. Redstone opens it, running dry closes it. */
	private void spend() {
		boolean wanted =
			level.hasNeighborSignal(worldPosition) && charge > 0 && outputSpeed.getValue() != 0;
		if (wanted)
			charge = Math.max(0, charge - Math.abs(outputSpeed.getValue()));

		if (discharging == wanted)
			return;
		discharging = wanted;
		updateOutput();
		sendData();
	}

	/**
	 * Push the new output speed out to the cogwheel.
	 *
	 * <p>Deliberately only a re-propagation. Create's controller can afford to tear its network down
	 * first, because the player changing its dial is not also the thing that gives it a source. Ours
	 * is: dropping the source here leaves the block claiming a speed with no network behind it, which
	 * the propagator destroys on sight - and the guard that covers that window then keeps answering
	 * zero, because the window never closes on its own. Leaving the shaft's source alone avoids both
	 * halves of that.</p>
	 */
	private void updateOutput() {
		if (level == null || level.isClientSide)
			return;
		// The standard call for a generator whose output has changed.
		updateGeneratedRotation();
	}

	/** Pour stored charge into the tool on top, a little each tick. */
	private void windHeldTool() {
		ItemStack tool = chargingInv.getStackInSlot(0);
		if (tool.isEmpty() || charge <= 0)
			return;

		int stored = KineticDisassemblerItem.getCharge(tool);
		int room = KineticDisassemblerItem.CAPACITY - stored;
		if (room <= 0)
			return;

		int wound = Math.min(Math.min(WIND_RATE, room), Mth.floor(charge));
		if (wound <= 0)
			return;

		KineticDisassemblerItem.setCharge(tool, stored + wound);
		charge -= wound;
		setChanged();
		if (stored + wound == KineticDisassemblerItem.CAPACITY)
			sendData();
	}

	public ItemStack getHeldTool() {
		return chargingInv.getStackInSlot(0);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<KineticAccumulatorBlockEntity> type) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type,
			(be, context) -> be.getItemHandler(context));
	}

	/** The top face only, which is the face you set a tool down on. */
	private IItemHandler getItemHandler(@Nullable Direction side) {
		return side == null || side == Direction.UP ? chargingInv : null;
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putFloat("Charge", charge);
		compound.putBoolean("Discharging", discharging);
		compound.put("ChargingInv", chargingInv.serializeNBT(registries));
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		charge = compound.getFloat("Charge");
		discharging = compound.getBoolean("Discharging");
		if (compound.contains("ChargingInv"))
			chargingInv.deserializeNBT(registries, compound.getCompound("ChargingInv"));
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

		boolean charging = calculateStressApplied() * Math.abs(getTheoreticalSpeed()) > 0;
		CKLang.translate(discharging ? "tooltip.accumulator.discharging"
			: charging ? "tooltip.accumulator.charging" : "tooltip.accumulator.idle")
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip);

		return true;
	}
}
