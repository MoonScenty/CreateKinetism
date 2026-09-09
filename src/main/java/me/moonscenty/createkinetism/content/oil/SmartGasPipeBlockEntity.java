package me.moonscenty.createkinetism.content.oil;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
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

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;

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
public class SmartGasPipeBlockEntity extends SmartBlockEntity
	implements IHaveGoggleInformation, IMekanismChemicalHandler, Clearable {

	/** The same as a plain segment, so putting one into a run does not throttle it. */
	public static final long CAPACITY = GasPipeBlockEntity.CAPACITY;

	/** The filter is enforced on the way in, not on the way out - see the class comment. */
	public final IChemicalTank tank = BasicChemicalTank.createModern(CAPACITY, stack -> true,
		this::passes, stack -> true, this);

	private final List<IChemicalTank> tanks = List.of(tank);

	public FilteringBehaviour filtering;

	public SmartGasPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		filtering = new FilteringBehaviour(this, new FilterSlot()).withPredicate(SmartGasPipeBlockEntity::names);
		behaviours.add(filtering);
	}

	@Override
	public void clearContent() {
		filtering.setFilter(ItemStack.EMPTY);
	}

	/** Whether an item is one a chemical can be read off. Empty is allowed: it clears the filter. */
	private static boolean names(ItemStack stack) {
		return stack.isEmpty() || chemicalOf(stack) != null;
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
		return filtering == null ? null : chemicalOf(filtering.getFilter());
	}

	/** An empty filter passes everything; otherwise only its own chemical gets in. */
	public boolean passes(ChemicalStack stack) {
		Chemical filter = getFilter();
		return filter == null || stack.is(filter);
	}

	private boolean isOpen(Direction side) {
		return side.getAxis() == SmartGasPipeBlock.getPipeAxis(getBlockState());
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

		Axis axis = SmartGasPipeBlock.getPipeAxis(getBlockState());
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
		event.registerBlockEntity(Capabilities.CHEMICAL.block(), type, (be, context) -> be);
	}

	/** Create's own placement for the filter card, unchanged - the model it sits on is theirs too. */
	static class FilterSlot extends ValueBoxTransform {

		@Override
		public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
			AttachFace face = state.getValue(SmartGasPipeBlock.FACE);
			float y = face == AttachFace.CEILING ? 0.55f : face == AttachFace.WALL ? 11.4f : 15.45f;
			float z = face == AttachFace.CEILING ? 4.6f : face == AttachFace.WALL ? 0.55f : 4.625f;
			return VecHelper.rotateCentered(VecHelper.voxelSpace(8, y, z), angleY(state), Axis.Y);
		}

		@Override
		public float getScale() {
			return super.getScale() * 1.02f;
		}

		@Override
		public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
			AttachFace face = state.getValue(SmartGasPipeBlock.FACE);
			TransformStack.of(ms)
				.rotateYDegrees(angleY(state))
				.rotateXDegrees(face == AttachFace.CEILING ? -45 : 45);
		}

		private static float angleY(BlockState state) {
			AttachFace face = state.getValue(SmartGasPipeBlock.FACE);
			float horizontalAngle = AngleHelper.horizontalAngle(state.getValue(SmartGasPipeBlock.FACING));
			if (face == AttachFace.WALL)
				horizontalAngle += 180;
			return horizontalAngle;
		}
	}
}
