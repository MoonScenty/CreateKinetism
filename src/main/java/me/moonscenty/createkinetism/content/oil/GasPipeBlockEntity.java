package me.moonscenty.createkinetism.content.oil;

import java.util.List;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
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

import net.createmod.catnip.data.Iterate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import org.jetbrains.annotations.Nullable;

/**
 * One segment of gas pipe: a small tank that spills into whatever is next to it.
 *
 * <p>There is no network here and no route-finding. A segment pushes what it holds into its
 * neighbours and that is all, which is enough because of the rule it pushes by:</p>
 *
 * <ul>
 * <li><b>Into another pipe</b>, only when that pipe holds less than this one, and only enough to
 * even the two out. Gas therefore runs downhill in fill level, away from wherever it is being pumped
 * in, and two pipes never trade the same gas back and forth.</li>
 * <li><b>Into anything else</b> - a machine, a Flare Stack, a Mekanism tube - as much as it will
 * take. That is what makes the far end of a run a destination rather than a dead end.</li>
 * </ul>
 *
 * <p>Nothing is ever pulled. A pipe run that is not being pumped into stays empty, exactly like
 * Create's fluid pipes; {@link GasPumpBlockEntity} is what puts gas into one.</p>
 */
public class GasPipeBlockEntity extends FluidPipeBlockEntity
	implements IHaveGoggleInformation, IMekanismChemicalHandler {

	/**
	 * How much one segment holds.
	 *
	 * <p>Sets the pace of a run as well as its contents: a segment can hand over at most half its
	 * gap to the next one each tick, so this is what caps throughput. Big enough not to throttle a
	 * pump, small enough that a long run is not a hidden tank.</p>
	 */
	public static final long CAPACITY = 500;

	public final IChemicalTank tank = BasicChemicalTank.createAllValid(CAPACITY, this);

	private final List<IChemicalTank> tanks = List.of(tank);

	public GasPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
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


	/** Only through faces the blockstate says are joined up. */
	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		if (side == null || isConnected(side))
			return tanks;
		return List.of();
	}

	private boolean isConnected(Direction side) {
		BlockState state = getBlockState();
		return state.hasProperty(PipeBlock.PROPERTY_BY_DIRECTION.get(side))
			&& state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side));
	}

	@Override
	public void onContentsChanged() {
		setChanged();
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide || tank.isEmpty())
			return;

		for (Direction side : Iterate.directions) {
			if (tank.isEmpty())
				return;
			if (!isConnected(side))
				continue;

			BlockPos other = worldPosition.relative(side);
			long allowance = level.getBlockEntity(other) instanceof GasPipeBlockEntity pipe
				? evenOut(pipe)
				: tank.getStored();
			if (allowance <= 0)
				continue;

			IChemicalHandler target = level.getCapability(Capabilities.CHEMICAL.block(), other,
				side.getOpposite());
			if (target == null)
				continue;
			offer(target, allowance);
		}
	}

	/**
	 * Half the difference between two pipes, so they meet in the middle rather than overshooting and
	 * sloshing back next tick.
	 */
	private long evenOut(GasPipeBlockEntity neighbour) {
		ChemicalStack held = tank.getStack();
		ChemicalStack theirs = neighbour.tank.getStack();
		// A pipe already carrying something else is not a route for this - let it clear first.
		if (!theirs.isEmpty() && !theirs.is(held.getChemical()))
			return 0;
		return (tank.getStored() - neighbour.tank.getStored()) / 2;
	}

	private void offer(IChemicalHandler target, long allowance) {
		ChemicalStack offered = tank.extract(allowance, Action.SIMULATE, AutomationType.INTERNAL);
		if (offered.isEmpty())
			return;
		ChemicalStack rejected = target.insertChemical(offered, Action.EXECUTE);
		long moved = offered.getAmount() - rejected.getAmount();
		if (moved > 0)
			tank.extract(moved, Action.EXECUTE, AutomationType.INTERNAL);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		ChemicalStack held = tank.getStack();
		if (held.isEmpty())
			return false;
		CKLang.builder()
			.add(Component.translatable(held.getChemical()
				.getTranslationKey()))
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip);
		CKLang.builder()
			.text(held.getAmount() + " / " + CAPACITY + "mB")
			.style(ChatFormatting.GOLD)
			.forGoggles(tooltip, 1);
		return true;
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.put("Tank", tank.serializeNBT(registries));
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		if (tag.contains("Tank"))
			tank.deserializeNBT(registries, tag.getCompound("Tank"));
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<GasPipeBlockEntity> type) {
		event.registerBlockEntity(Capabilities.CHEMICAL.block(), type, (be, context) -> be);
	}
}
