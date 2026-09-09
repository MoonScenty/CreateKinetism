package me.moonscenty.createkinetism.content.oil;

import java.util.List;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
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

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import org.jetbrains.annotations.Nullable;

/**
 * Moves a chemical along an axis, the way Create's Mechanical Pump moves a fluid.
 *
 * <p>Mekanism's own pressurized tubes will carry a gas on their own, but nothing in this mod pushed
 * one: the distillation column offers its air and its gaseous cuts and then waits for something to
 * take them. This is that something, and being kinetic it costs rotation rather than a config
 * number - see {@code CKStress}.</p>
 *
 * <p>It pulls out of whatever is behind it and pushes into whatever is in front, where front is
 * {@link GasPumpBlock#FACING}. The small buffer in between is what lets a tube attach to either end
 * as well: a tube can fill the buffer from behind or drain it from in front, so the pump works
 * bolted straight onto a machine or dropped into the middle of a tube run.</p>
 *
 * <p>Throughput is {@value #MB_PER_TICK_PER_RPM} mB per tick per RPM, and the pump does not care
 * what it is carrying. Pointing one at a Flare Stack is how a refinery gets rid of a gas it cannot
 * use; pointing one out of a vacuum column is how the column holds its vacuum.</p>
 */
public class GasPumpBlockEntity extends PumpBlockEntity implements IMekanismChemicalHandler {

	/** Millibuckets a tick at one RPM. */
	public static final int MB_PER_TICK_PER_RPM = 4;

	/**
	 * How much sits in the pump itself.
	 *
	 * <p>Small on purpose. It exists so a tube has something to talk to at either end, not as
	 * storage - a pump that buffered a bucket would go on delivering for a while after it stopped
	 * turning.</p>
	 */
	public static final long BUFFER = 500;

	/** Ticks between syncs of the buffer, matching Create's own fluid tank. */
	private static final int SYNC_RATE = 8;

	public final IChemicalTank buffer = BasicChemicalTank.createAllValid(BUFFER, this);

	private final List<IChemicalTank> tanks = List.of(buffer);
	private int syncCooldown;

	public GasPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** The direction gas travels: out of the front, in through the back. */
	public Direction facing() {
		return getBlockState().getValue(PumpBlock.FACING);
	}

	/**
	 * The buffer, but only to the two ends of the run.
	 *
	 * <p>Offering it sideways would let a tube on the flank both fill and empty the pump, which reads
	 * as the pump leaking.</p>
	 */
	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		if (side == null || side.getAxis() == facing().getAxis())
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
		if (syncCooldown == 0)
			syncCooldown = SYNC_RATE;
	}

	/** What the pump can move this tick, each way. Zero when it is not turning. */
	public long getRate() {
		return Mth.floor(Math.abs(getSpeed()) * MB_PER_TICK_PER_RPM);
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;

		if (syncCooldown > 0 && --syncCooldown == 0)
			sendData();

		long rate = getRate();
		if (rate <= 0)
			return;

		Direction front = facing();
		push(neighbour(front), rate);
		pull(neighbour(front.getOpposite()), rate);
	}

	@Nullable
	private IChemicalHandler neighbour(Direction side) {
		return level.getCapability(Capabilities.CHEMICAL.block(), worldPosition.relative(side),
			side.getOpposite());
	}

	/**
	 * Empty the buffer forwards.
	 *
	 * <p>Done before the pull, so a full buffer makes room in the same tick it is refilled and the
	 * pump moves its whole rate rather than half of it.</p>
	 */
	private void push(@Nullable IChemicalHandler target, long rate) {
		if (target == null || buffer.isEmpty())
			return;
		ChemicalStack offered = buffer.extract(rate, Action.SIMULATE, AutomationType.INTERNAL);
		if (offered.isEmpty())
			return;
		ChemicalStack rejected = target.insertChemical(offered, Action.EXECUTE);
		long moved = offered.getAmount() - rejected.getAmount();
		if (moved > 0)
			buffer.extract(moved, Action.EXECUTE, AutomationType.INTERNAL);
	}

	/** Fill the buffer from behind, taking only as much as will fit. */
	private void pull(@Nullable IChemicalHandler source, long rate) {
		if (source == null)
			return;
		long room = buffer.getNeeded();
		if (room <= 0)
			return;
		ChemicalStack available = source.extractChemical(Math.min(rate, room), Action.SIMULATE);
		if (available.isEmpty())
			return;
		ChemicalStack rejected = buffer.insert(available, Action.SIMULATE, AutomationType.INTERNAL);
		long take = available.getAmount() - rejected.getAmount();
		if (take <= 0)
			return;
		buffer.insert(source.extractChemical(available.copyWithAmount(take), Action.EXECUTE),
			Action.EXECUTE, AutomationType.INTERNAL);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);

		CKLang.translate("gui.gas_pump.throughput")
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip);
		LangBuilder millibuckets = CKLang.builder()
			.text(" mB/t");
		CKLang.builder()
			.text(String.valueOf(getRate()))
			.add(millibuckets)
			.style(ChatFormatting.AQUA)
			.forGoggles(tooltip, 1);

		ChemicalStack held = buffer.getStack();
		if (!held.isEmpty()) {
			CKLang.builder()
				.add(Component.translatable(held.getChemical()
					.getTranslationKey()))
				.style(ChatFormatting.GRAY)
				.forGoggles(tooltip);
			CKLang.builder()
				.text(held.getAmount() + " / " + BUFFER + "mB")
				.style(ChatFormatting.GOLD)
				.forGoggles(tooltip, 1);
		}

		return true;
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.put("Buffer", buffer.serializeNBT(registries));
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		if (tag.contains("Buffer"))
			buffer.deserializeNBT(registries, tag.getCompound("Buffer"));
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<GasPumpBlockEntity> type) {
		// The two ends of the run only, matching getChemicalTanks - otherwise a pipe alongside the pump
		// would reach for a face the pump does not trade through.
		event.registerBlockEntity(Capabilities.CHEMICAL.block(), type,
			(be, context) -> context == null || context.getAxis() == be.facing()
				.getAxis() ? be : null);
	}
}
