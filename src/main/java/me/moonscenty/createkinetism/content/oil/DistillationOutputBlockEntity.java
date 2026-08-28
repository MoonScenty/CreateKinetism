package me.moonscenty.createkinetism.content.oil;

import java.util.List;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;

import me.moonscenty.createkinetism.content.steel.SteelTankBlockEntity;
import me.moonscenty.createkinetism.foundation.CKLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Holds one fraction coming off the column and registers itself with the controller so the
 * controller knows that stage has somewhere to go. Two taps on the same stage is a mistake, and the
 * second one says so in its goggle tooltip rather than silently eating output.</p>
 */
public class DistillationOutputBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

	public SmartFluidTankBehaviour tankInventory;
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
	public void lazyTick() {
		super.lazyTick();
		if (level.isClientSide)
			return;

		// Powered means "this cut is waste" - throw it away instead of backing the column up.
		if (getBlockState().getValue(DistillationOutputBlock.POWERED)) {
			tankInventory.getPrimaryHandler()
				.drain(500, FluidAction.EXECUTE);
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

		if (getBlockState().getValue(DistillationOutputBlock.POWERED))
			CKLang.translate("gui.distil_discard")
				.style(ChatFormatting.GRAY)
				.forGoggles(tooltip);

		return true;
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		tag.putBoolean("Duplicate", duplicate);
		super.write(tag, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		duplicate = tag.getBoolean("Duplicate");
		super.read(tag, registries, clientPacket);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<DistillationOutputBlockEntity> type) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type,
			(be, context) -> context == null || context == DistillationOutputBlock.getFacing(be.getBlockState())
				? be.tankInventory.getCapability()
				: null);
	}
}
