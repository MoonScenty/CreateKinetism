package me.moonscenty.createkinetism.content.oil;

import java.util.List;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;

import me.moonscenty.createkinetism.content.steel.SteelTankBlockEntity;
import me.moonscenty.createkinetism.foundation.CKLang;
import me.moonscenty.createkinetism.foundation.SidedChemicalAccess;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import org.jetbrains.annotations.Nullable;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Holds one fraction coming off the column and registers itself with the controller so the
 * controller knows that stage has somewhere to go. Two taps on the same stage is a mistake, and the
 * second one says so in its goggle tooltip rather than silently eating output.</p>
 *
 * <p>Two tanks, because a cut can be either kind. The liquid fractions go in the fluid tank and
 * out through a Create pipe; a gaseous cut goes in the chemical tank and out through a Mekanism
 * tube. A given stage only ever uses one of them - the recipe decides which - so the other simply
 * stays empty rather than needing to be switched off.</p>
 */
public class DistillationOutputBlockEntity extends SmartBlockEntity
	implements IHaveGoggleInformation, IMekanismChemicalHandler {

	/** Matches the fluid tank's own 4000, so neither kind of cut backs the column up sooner. */
	public static final long CHEMICAL_CAPACITY = 4000;

	public SmartFluidTankBehaviour tankInventory;

	/** Filled only by the column, emptied by whatever is pulling on it. */
	public final IChemicalTank chemicalTank = BasicChemicalTank.output(CHEMICAL_CAPACITY, this);

	private final List<IChemicalTank> tanks = List.of(chemicalTank);
	public boolean duplicate = false;

	public DistillationOutputBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		setLazyTickRate(10);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		tankInventory = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, this, 1, 4000, true)
			.whenFluidUpdates(this::sendData)
			.forbidInsertion();
		behaviours.add(tankInventory);
	}

	@Override
	public void onContentsChanged() {
		setChanged();
		sendData();
	}

	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		return tanks;
	}

	/**
	 * Room for one cut of this size, whichever kind it is.
	 *
	 * <p>The column asks before it commits: a stage that cannot take its share stalls the whole
	 * batch rather than letting the rest through and voiding this one.</p>
	 */
	public boolean canAccept(ChemicalStack cut) {
		return chemicalTank.insert(cut, Action.SIMULATE, AutomationType.INTERNAL)
			.isEmpty();
	}

	public void accept(ChemicalStack cut) {
		chemicalTank.insert(cut, Action.EXECUTE, AutomationType.INTERNAL);
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (level.isClientSide)
			return;

		// Powered means "this cut is waste" - throw it away instead of backing the column up.
		if (getBlockState().getValue(DistillationOutputBlock.POWERED)) {
			tankInventory.getPrimaryHandler()
				.drain(500, FluidAction.EXECUTE);
			chemicalTank.extract(500, Action.EXECUTE, AutomationType.INTERNAL);
			sendData();
		}

		DistillationControllerBlockEntity controller = findController();
		if (controller == null)
			return;

		boolean wasDuplicate = duplicate;
		duplicate = controller.addOutput(getOutputNumber(), worldPosition);
		if (wasDuplicate != duplicate)
			sendData();
	}

	@Override
	public void remove() {
		super.remove();
		if (level.isClientSide)
			return;
		DistillationControllerBlockEntity controller = findController();
		if (controller != null)
			controller.removeOutput(getOutputNumber());
	}

	private SteelTankBlockEntity backingTank() {
		BlockEntity be = level.getBlockEntity(
			worldPosition.relative(DistillationOutputBlock.getTankFace(getBlockState())));
		return be instanceof SteelTankBlockEntity tank ? tank : null;
	}

	private DistillationControllerBlockEntity findController() {
		SteelTankBlockEntity tank = backingTank();
		return tank == null ? null : tank.getDistillationControllerBE();
	}

	public int getOutputNumber() {
		SteelTankBlockEntity tank = backingTank();
		return tank == null ? -1 : tank.getOutputNumber();
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		if (duplicate)
			CKLang.translate("gui.distil_duplicate")
				.style(ChatFormatting.DARK_RED)
				.forGoggles(tooltip);

		int output = getOutputNumber();
		if (output != -1)
			CKLang.translate("gui.distil_layer")
				.text("#" + output)
				.forGoggles(tooltip);

		containedFluidTooltip(tooltip, isPlayerSneaking, tankInventory.getCapability());

		ChemicalStack gas = chemicalTank.getStack();
		if (!gas.isEmpty()) {
			CKLang.builder()
				.text("")
				.add(Component.translatable(gas.getChemical()
					.getTranslationKey()))
				.style(ChatFormatting.GRAY)
				.forGoggles(tooltip);
			CKLang.builder()
				.text(gas.getAmount() + " / " + CHEMICAL_CAPACITY + "mB")
				.style(ChatFormatting.GOLD)
				.forGoggles(tooltip, 1);
		}

		if (getBlockState().getValue(DistillationOutputBlock.POWERED))
			CKLang.translate("gui.distil_discard")
				.style(ChatFormatting.GRAY)
				.forGoggles(tooltip);

		return true;
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		tag.putBoolean("Duplicate", duplicate);
		tag.put("ChemicalTank", chemicalTank.serializeNBT(registries));
		super.write(tag, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		duplicate = tag.getBoolean("Duplicate");
		if (tag.contains("ChemicalTank"))
			chemicalTank.deserializeNBT(registries, tag.getCompound("ChemicalTank"));
		super.read(tag, registries, clientPacket);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<DistillationOutputBlockEntity> type) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type,
			(be, context) -> context == null || context == DistillationOutputBlock.getFacing(be.getBlockState())
				? be.tankInventory.getCapability()
				: null);
		// The gas leaves by the same face the liquid would, so a tube goes where a pipe would have.
		event.registerBlockEntity(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), type,
			(be, context) -> context == null || context == DistillationOutputBlock.getFacing(be.getBlockState())
				? new SidedChemicalAccess(be, context)
				: null);
	}
}
