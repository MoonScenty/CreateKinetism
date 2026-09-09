package me.moonscenty.createkinetism.content.oil;

import java.util.List;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.common.capabilities.Capabilities;

import me.moonscenty.createkinetism.foundation.CKLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import org.jetbrains.annotations.Nullable;

/**
 * A gas pipe segment that only passes one chemical.
 *
 * <p>It transports the way a plain {@link GasPipeBlockEntity} does - a small tank that spills
 * downhill - with two differences: it joins only the two ends of its own axis, and it refuses to
 * <em>accept</em> anything its filter does not name. Refusing at the inlet rather than the outlet is
 * what makes it useful in a branch: gas that does not match simply goes the other way instead of
 * piling up here.</p>
 *
 * <p>The filter is set the way every Create filter is, by putting an item in the slot on its face -
 * but the item has to be something holding a chemical, because a chemical has no bucket to name it
 * by. Mekanism's Gauge Dropper is the cheap way to do that; any of its chemical tanks work too. An
 * empty filter passes everything, which is what a fresh one does.</p>
 */
public class SmartGasPipeBlockEntity extends SmartFluidPipeBlockEntity
	implements IHaveGoggleInformation, IMekanismChemicalHandler {

	/** The same as a plain segment, so putting one into a run does not throttle it. */
	public static final long CAPACITY = GasPipeBlockEntity.CAPACITY;

	/** The filter is enforced on the way in, not on the way out - see the class comment. */
	public final IChemicalTank tank = BasicChemicalTank.createModern(CAPACITY, stack -> true,
		this::passes, stack -> true, this);

	private final List<IChemicalTank> tanks = List.of(tank);

	public SmartGasPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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

	@Nullable
	private static Chemical chemicalOf(ItemStack stack) {
		IChemicalHandler handler = stack.getCapability(Capabilities.CHEMICAL.item());
		if (handler == null)
			return null;
		for (int tank = 0; tank < handler.getChemicalTanks(); tank++) {
			ChemicalStack held = handler.getChemicalInTank(tank);
			if (!held.isEmpty())
				return held.getChemical();
		}
		return null;
	}

	/** The chemical this segment is set to, or null while the filter is empty. */
	@Nullable
	public Chemical getFilter() {
		FilteringBehaviour filtering = getBehaviour(FilteringBehaviour.TYPE);
		return filtering == null ? null : chemicalOf(filtering.getFilter());
	}

	/** An empty filter passes everything; otherwise only its own chemical gets in. */
	public boolean passes(ChemicalStack stack) {
		Chemical filter = getFilter();
		return filter == null || stack.is(filter);
	}

	/** Whether that face is one of the two ends of this segment. */
	public boolean isOpen(Direction side) {
		return side.getAxis() == pipeAxis();
	}

	/** Create's own derivation, inlined because its own copy is not visible from here. */
	private Axis pipeAxis() {
		BlockState state = getBlockState();
		return state.getValue(SmartFluidPipeBlock.FACE) == AttachFace.WALL ? Axis.Y
			: state.getValue(SmartFluidPipeBlock.FACING)
				.getAxis();
	}

	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		if (side == null || isOpen(side))
			return tanks;
		return List.of();
	}

	@Override
	public void onContentsChanged() {
		setChanged();
	}

	/** Identical to a plain segment's, but only along its own axis. */
	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide || tank.isEmpty())
			return;

		Axis axis = pipeAxis();
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

	/** The tank of a neighbouring pipe segment, plain or smart, or null for anything else. */
	@Nullable
	private IChemicalTank neighbourTank(BlockPos pos) {
		BlockEntity be = level.getBlockEntity(pos);
		if (be instanceof GasPipeBlockEntity pipe)
			return pipe.tank;
		if (be instanceof SmartGasPipeBlockEntity pipe)
			return pipe.tank;
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
		Chemical filter = getFilter();
		if (filter != null) {
			CKLang.translate("gui.smart_gas_pipe.filter")
				.style(ChatFormatting.GRAY)
				.forGoggles(tooltip);
			CKLang.builder()
				.add(Component.translatable(filter.getTranslationKey()))
				.style(ChatFormatting.AQUA)
				.forGoggles(tooltip, 1);
		}

		ChemicalStack held = tank.getStack();
		if (held.isEmpty())
			return filter != null;
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
		BlockEntityType<SmartGasPipeBlockEntity> type) {
		// Only the two ends. Offering a handler sideways is what would make a neighbouring pipe grow an
		// arm towards a face this segment will never trade through.
		event.registerBlockEntity(Capabilities.CHEMICAL.block(), type,
			(be, context) -> context == null || be.isOpen(context) ? be : null);
	}

}
