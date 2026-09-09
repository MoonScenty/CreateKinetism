package me.moonscenty.createkinetism.content.oil;

import java.util.List;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.common.capabilities.Capabilities;

import me.moonscenty.createkinetism.foundation.CKLang;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import org.jetbrains.annotations.Nullable;

/**
 * The valve's handwheel, and the gas it lets past when it has been turned all the way.
 *
 * <p>The opening half is Create's and is left to Create: the shaft turns, its own pointer chases
 * one end or the other, and it flips {@code ENABLED} when the pointer arrives. A valve half-turned
 * is a valve shut.</p>
 *
 * <p>{@link #pointer} is a second copy of that dial, and exists for one reason - Create keeps its
 * own package-private, and {@code GasValveRenderer} needs a value to turn the handwheel by. It is
 * fed the same way from the same speed, so it tracks Create's exactly; nothing reads it but the
 * renderer, and nothing but Create decides whether the valve is open.</p>
 *
 * <p>The transport half is a {@link GasPipeBlockEntity}'s, narrowed twice over: only along the pipe
 * axis, and only while open. Closed, the tank refuses everything at the inlet, so a pipe run backs
 * up against it rather than dribbling through.</p>
 */
public class GasValveBlockEntity extends FluidValveBlockEntity
	implements IHaveGoggleInformation, IMekanismChemicalHandler {

	/** The same as a plain segment, so a valve in a run does not throttle it while open. */
	public static final long CAPACITY = GasPipeBlockEntity.CAPACITY;

	/** How far round the handwheel is, 0 shut to 1 open. Drives the renderer and nothing else. */
	public LerpedFloat pointer;

	public final IChemicalTank tank =
		BasicChemicalTank.createModern(CAPACITY, stack -> true, stack -> isOpen(), stack -> true, this);

	private final List<IChemicalTank> tanks = List.of(tank);

	public GasValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		pointer = LerpedFloat.linear()
			.startWithValue(0)
			.chase(0, 0, Chaser.LINEAR);
	}

	public boolean isOpen() {
		BlockState state = getBlockState();
		return state.hasProperty(FluidValveBlock.ENABLED) && state.getValue(FluidValveBlock.ENABLED);
	}

	/** Whether that face is one of the two ends of this segment. */
	public boolean isAlongPipe(Direction side) {
		return side.getAxis() == FluidValveBlock.getPipeAxis(getBlockState());
	}

	/** Shut, it has nothing to offer any face - which is also what stops a tube pulling through it. */
	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		if (!isOpen())
			return List.of();
		if (side == null || isAlongPipe(side))
			return tanks;
		return List.of();
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		// Create's own, untouched. Each of these blocks has its own transport behaviour with its own
		// idea of which faces are ends - a pipe's is not a valve's - and PipeAttachmentModel reads it
		// to decide the rims and connectors. Swapping in one shared replacement erased all of them.
		//
		// Nothing liquid can reach these anyway: what a gas pipe connects to is decided in
		// GasPipeBlock.canConnectToGas, which looks for chemical handlers and nothing else.
		super.addBehaviours(behaviours);
	}

	@Override
	public void onContentsChanged() {
		setChanged();
	}

	@Override
	public void onSpeedChanged(float previousSpeed) {
		super.onSpeedChanged(previousSpeed);
		float speed = getSpeed();
		pointer.chase(speed > 0 ? 1 : 0, chaseSpeed(), Chaser.LINEAR);
		sendData();
	}

	/** A full turn takes about a second at 16 RPM, and proportionally less faster. */
	private float chaseSpeed() {
		return Mth.clamp(Math.abs(getSpeed()) / 16 / 20, 0, 1);
	}

	@Override
	public void tick() {
		super.tick();
		pointer.tickChaser();
		if (level == null || level.isClientSide)
			return;

		// Whether the valve is open is Create's answer, already settled by super.tick().
		if (!isOpen() || tank.isEmpty())
			return;
		transport();
	}

	/** {@link GasPipeBlockEntity}'s rule, along this segment's own axis only. */
	private void transport() {
		Axis axis = FluidValveBlock.getPipeAxis(getBlockState());
		for (AxisDirection sign : AxisDirection.values()) {
			if (tank.isEmpty())
				return;
			Direction side = Direction.fromAxisAndDirection(axis, sign);
			BlockPos other = worldPosition.relative(side);

			IChemicalTank theirs = neighbourTank(other);
			long allowance = theirs == null ? tank.getStored() : evenOut(theirs);
			if (allowance <= 0)
				continue;

			IChemicalHandler target = level.getCapability(Capabilities.CHEMICAL.block(), other,
				side.getOpposite());
			if (target == null)
				continue;

			ChemicalStack offered = tank.extract(allowance, Action.SIMULATE, AutomationType.INTERNAL);
			if (offered.isEmpty())
				continue;
			ChemicalStack rejected = target.insertChemical(offered, Action.EXECUTE);
			long moved = offered.getAmount() - rejected.getAmount();
			if (moved > 0)
				tank.extract(moved, Action.EXECUTE, AutomationType.INTERNAL);
		}
	}

	@Nullable
	private IChemicalTank neighbourTank(BlockPos pos) {
		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof GasPipeBlockEntity pipe)
			return pipe.tank;
		if (be instanceof SmartGasPipeBlockEntity pipe)
			return pipe.tank;
		if (be instanceof GasValveBlockEntity valve)
			return valve.tank;
		return null;
	}

	private long evenOut(IChemicalTank theirs) {
		ChemicalStack held = tank.getStack();
		ChemicalStack other = theirs.getStack();
		if (!other.isEmpty() && !other.is(held.getChemical()))
			return 0;
		return (tank.getStored() - theirs.getStored()) / 2;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);

		CKLang.translate(isOpen() ? "gui.gas_valve.open" : "gui.gas_valve.closed")
			.style(isOpen() ? ChatFormatting.GREEN : ChatFormatting.GRAY)
			.forGoggles(tooltip);

		ChemicalStack held = tank.getStack();
		if (!held.isEmpty()) {
			CKLang.builder()
				.add(Component.translatable(held.getChemical()
					.getTranslationKey()))
				.style(ChatFormatting.GRAY)
				.forGoggles(tooltip);
			CKLang.builder()
				.text(held.getAmount() + " / " + CAPACITY + "mB")
				.style(ChatFormatting.GOLD)
				.forGoggles(tooltip, 1);
		}
		return true;
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.put("Pointer", pointer.writeNBT());
		tag.put("Tank", tank.serializeNBT(registries));
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		pointer.readNBT(tag.getCompound("Pointer"), clientPacket);
		if (tag.contains("Tank"))
			tank.deserializeNBT(registries, tag.getCompound("Tank"));
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<GasValveBlockEntity> type) {
		// The two ends, open or shut. A closed valve still has to look like part of the run - it just
		// hands out no tanks, so nothing gets through it.
		event.registerBlockEntity(Capabilities.CHEMICAL.block(), type,
			(be, context) -> context == null || be.isAlongPipe(context) ? be : null);
	}
}
