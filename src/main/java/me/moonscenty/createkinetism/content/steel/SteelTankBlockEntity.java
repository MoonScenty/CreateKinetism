package me.moonscenty.createkinetism.content.steel;

import static java.lang.Math.abs;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;

import me.moonscenty.createkinetism.content.oil.DistillationControllerBlockEntity;
import me.moonscenty.createkinetism.foundation.CKLang;

import net.createmod.catnip.data.Iterate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>A Create fluid tank with a second job. On its own it is exactly Create's tank multiblock, but
 * put a Distillation Controller against it and the whole stack switches into
 * <em>column mode</em>: the windows are welded shut, the tank stops behaving as one big vessel, and
 * each pair of layers becomes a numbered fractionation stage that a Distillation Output can tap.</p>
 *
 * <p>Column mode is also where the heat comes in - the controller reads {@link #heat}, summed from
 * whatever is burning under the column's footprint.</p>
 */
public class SteelTankBlockEntity extends FluidTankBlockEntity
	implements IHaveGoggleInformation, IMultiBlockEntityContainer.Fluid {

	public boolean isDistillingColumn = false;
	public boolean[] occludedDirections = { true, true, true, true };
	public float heat;
	public BlockPos distillationController;

	public SteelTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		setLazyTickRate(10);
	}

	@Override
	protected void updateConnectivity() {
		updateConnectivity = false;
		if (level.isClientSide)
			return;
		if (!isController())
			return;
		refreshCapability();
		ConnectivityHandler.formMulti(this);
	}

	public void updateConnectivityExternally() {
		updateConnectivity();
	}

	@Nullable
	public DistillationControllerBlockEntity getDistillationControllerBE() {
		SteelTankBlockEntity controller = getControllerBE();
		if (controller == null || controller.distillationController == null)
			return null;
		BlockEntity be = level.getBlockEntity(controller.distillationController);
		return be instanceof DistillationControllerBlockEntity found ? found : null;
	}

	public void setDistillationMode(boolean active) {
		isDistillingColumn = active;
		setWindows(!active);
	}

	@Override
	public void lazyTick() {
		if (!isController())
			return;
		if (!isDistillingColumn)
			return;

		// Which sides are blocked off, so the renderer knows where not to draw pipework.
		for (Direction d : Iterate.horizontalDirections) {
			AABB aabb = new AABB(getBlockPos()).move(width / 2f - .5f, 0, width / 2f - .5f)
				.deflate(5f / 8);
			aabb = aabb.move(d.getStepX() * (width / 2f + 1 / 4f), 0, d.getStepZ() * (width / 2f + 1 / 4f));
			aabb = aabb.inflate(Math.abs(d.getStepZ()) / 2f, 0.25f, Math.abs(d.getStepX()) / 2f);
			occludedDirections[d.get2DDataValue()] = !getLevel().noCollision(aabb);
		}

		// Heat is summed over the whole footprint, so a wider column wants more burners under it.
		heat = 0;
		for (int xOffset = 0; xOffset < width; xOffset++)
			for (int zOffset = 0; zOffset < width; zOffset++) {
				BlockPos pos = worldPosition.offset(xOffset, -1, zOffset);
				heat += Math.max(BoilerHeater.findHeat(level, pos, level.getBlockState(pos)), 0);
			}
	}

	@Override
	public SteelTankBlockEntity getControllerBE() {
		if (isController())
			return this;
		BlockEntity be = level.getBlockEntity(controller);
		return be instanceof SteelTankBlockEntity found ? found : null;
	}

	@Override
	public void removeController(boolean keepFluids) {
		if (level.isClientSide)
			return;
		updateConnectivity = true;
		if (!keepFluids)
			applyFluidTankSize(1);
		controller = null;
		width = 1;
		height = 1;
		onFluidStackChanged(tankInventory.getFluid());

		BlockState state = getBlockState();
		if (SteelTankBlock.isTank(state)) {
			state = state.setValue(SteelTankBlock.BOTTOM, true);
			state = state.setValue(SteelTankBlock.TOP, true);
			state = state.setValue(SteelTankBlock.SHAPE,
				window ? FluidTankBlock.Shape.WINDOW : FluidTankBlock.Shape.PLAIN);
			getLevel().setBlock(worldPosition, state, 22);
		}

		refreshCapability();
		setChanged();
		sendData();
	}

	@Override
	public void toggleWindows() {
		SteelTankBlockEntity controller = getControllerBE();
		if (controller == null || controller.isDistillingColumn)
			return;
		controller.setWindows(!controller.window);
	}

	/**
	 * Which fractionation stage this block is, counting from 1 at the bottom. Every two layers of
	 * tank make one stage, so a taller column separates crude into more cuts.
	 */
	public int getOutputNumber() {
		SteelTankBlockEntity controller = getControllerBE();
		if (controller == null || !controller.isDistillingColumn)
			return -1;
		return (worldPosition.subtract(controller.worldPosition)
			.getY() + 1) / 2 + 1;
	}

	public boolean hasWindows() {
		return window;
	}

	public int getLuminosity() {
		return luminosity;
	}

	@Override
	public void setWindows(boolean window) {
		this.window = window;
		for (int yOffset = 0; yOffset < height; yOffset++)
			for (int xOffset = 0; xOffset < width; xOffset++)
				for (int zOffset = 0; zOffset < width; zOffset++) {
					BlockPos pos = worldPosition.offset(xOffset, yOffset, zOffset);
					BlockState blockState = level.getBlockState(pos);
					if (!SteelTankBlock.isTank(blockState))
						continue;

					FluidTankBlock.Shape shape = FluidTankBlock.Shape.PLAIN;
					if (window) {
						if (width == 1)
							shape = FluidTankBlock.Shape.WINDOW;
						if (width == 2)
							shape = xOffset == 0
								? zOffset == 0 ? FluidTankBlock.Shape.WINDOW_NW : FluidTankBlock.Shape.WINDOW_SW
								: zOffset == 0 ? FluidTankBlock.Shape.WINDOW_NE : FluidTankBlock.Shape.WINDOW_SE;
						if (width == 3 && abs(abs(xOffset) - abs(zOffset)) == 1)
							shape = FluidTankBlock.Shape.WINDOW;
					}

					level.setBlock(pos, blockState.setValue(SteelTankBlock.SHAPE, shape), 22);
					level.getChunkSource()
						.getLightEngine()
						.checkBlock(pos);
				}
	}

	/** Steel tanks are not boilers; the heat they gather feeds the distillation column instead. */
	@Override
	public void updateBoilerState() {
	}

	@Override
	public void setController(BlockPos controller) {
		if (level.isClientSide && !isVirtual())
			return;
		if (controller.equals(this.controller))
			return;
		this.controller = controller;
		refreshCapability();
		setChanged();
		sendData();
	}

	public void refreshCapability() {
		fluidCapability = handlerForCapability();
		invalidateCapabilities();
	}

	private IFluidHandler handlerForCapability() {
		if (isController())
			return tankInventory;
		SteelTankBlockEntity controller = getControllerBE();
		return controller != null ? controller.handlerForCapability() : tankInventory;
	}

	@Nullable
	public SteelTankBlockEntity getOtherSteelTankBlockEntity(Direction direction) {
		BlockEntity other = level.getBlockEntity(worldPosition.relative(direction));
		return other instanceof SteelTankBlockEntity found ? found : null;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		SteelTankBlockEntity controller = getControllerBE();
		if (controller == null)
			return false;

		if (controller.isDistillingColumn) {
			CKLang.translate("gui.distil_layer")
				.text("#" + getOutputNumber())
				.forGoggles(tooltip);
			CKLang.translate("gui.distil_heat")
				.text(" " + String.format("%.1f", controller.heat))
				.style(ChatFormatting.GRAY)
				.forGoggles(tooltip);
			return true;
		}

		return containedFluidTooltip(tooltip, isPlayerSneaking,
			level.getCapability(Capabilities.FluidHandler.BLOCK, controller.getBlockPos(), null));
	}

	@Override
	public void notifyMultiUpdated() {
		BlockState state = getBlockState();
		if (SteelTankBlock.isTank(state)) {
			state = state.setValue(SteelTankBlock.BOTTOM, getController().getY() == getBlockPos().getY());
			state = state.setValue(SteelTankBlock.TOP,
				getController().getY() + height - 1 == getBlockPos().getY());
			level.setBlock(getBlockPos(), state, 6);
		}
		if (isController())
			setWindows(window);
		onFluidStackChanged(tankInventory.getFluid());
		setChanged();
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<SteelTankBlockEntity> type) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (be, context) -> {
			SteelTankBlockEntity controller = be.getControllerBE();
			if (controller == null)
				return null;
			// In column mode the stack is not one vessel any more, so it exposes nothing directly -
			// the fractions come out of the Distillation Output blocks instead.
			if (controller.isDistillingColumn)
				return null;
			if (be.fluidCapability == null)
				be.refreshCapability();
			return be.fluidCapability;
		});
	}
}
