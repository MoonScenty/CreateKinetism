package me.moonscenty.createkinetism.content.boiler;

import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.fluid.SmartFluidTank;

import me.moonscenty.createkinetism.foundation.MekanismFluids;

import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * Burns superheated sodium into sodium at a flat 1 mB/tick. There is no basin and no GUI - a Thermal
 * Boiler Tank stacked above one sees it as a heat source purely through
 * {@link BoilerHeater#REGISTRY} (see {@link #registerHeatSource}), the same mechanism a vanilla Blaze
 * Burner uses.
 *
 * <p>The shaft is optional: fuel alone reports Create's HEATED (1). Spinning the shaft (from either
 * end, direction does not matter) bumps that past Create's own SUPERHEATED to 3 - a tier Create has
 * no name for, which only a {@code minimum_tier: 3} Thermal Boiling recipe ever asks for (see
 * {@link ThermalBoilingRecipe}). It still satisfies an ordinary {@code "superheated"} recipe too,
 * same as any hotter-than-required source would.</p>
 */
public class SodiumBurnerBlockEntity extends KineticBlockEntity {

	private static final int CAPACITY = 1000;
	private static final int BURN_RATE = 1;

	private final SmartFluidTank inputTank = new SmartFluidTank(CAPACITY, this::onFluidStackChanged) {
		@Override
		public boolean isFluidValid(FluidStack stack) {
			return stack.getFluid() == MekanismFluids.SUPERHEATED_SODIUM.get();
		}
	};
	private final SmartFluidTank outputTank = new SmartFluidTank(CAPACITY, this::onFluidStackChanged);

	public SodiumBurnerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	private void onFluidStackChanged(FluidStack ignored) {
		setChanged();
		sendData();
	}

	/** The two faces across from each other, perpendicular to the shaft - fuel in one, ash out the other. */
	private Couple<Direction> sides() {
		Axis axis = getBlockState().hasProperty(SodiumBurnerBlock.HORIZONTAL_AXIS)
			? getBlockState().getValue(SodiumBurnerBlock.HORIZONTAL_AXIS)
			: Axis.Z;
		return axis == Axis.Z ? Couple.create(Direction.WEST, Direction.EAST)
			: Couple.create(Direction.NORTH, Direction.SOUTH);
	}

	public Direction inputSide() {
		return sides().getFirst();
	}

	public Direction outputSide() {
		return sides().getSecond();
	}

	public boolean isBurning() {
		return !inputTank.getFluid()
			.isEmpty() && outputTank.getSpace() > 0;
	}

	/** Fuel and a turning shaft together - the tier 3 state, and what swaps the block to burn.png. */
	public boolean isSuperheated() {
		return isBurning() && getSpeed() != 0;
	}

	/** What a Thermal Boiler Tank sees: Create's HEATED unpowered, tier 3 once the shaft is spinning. */
	public float heatLevel() {
		if (!isBurning())
			return BoilerHeater.NO_HEAT;
		return isSuperheated() ? 3f : 1f;
	}

	@Override
	public void tick() {
		super.tick();
		if (level.isClientSide)
			return;
		burn();
		updateLitState();
	}

	/** Swaps the block to burn.png exactly when superheated - see {@link SodiumBurnerBlock#LIT}. */
	private void updateLitState() {
		boolean lit = isSuperheated();
		BlockState state = getBlockState();
		if (state.getValue(SodiumBurnerBlock.LIT) != lit)
			level.setBlockAndUpdate(worldPosition, state.setValue(SodiumBurnerBlock.LIT, lit));
	}

	private void burn() {
		FluidStack held = inputTank.getFluid();
		if (held.isEmpty())
			return;
		int consumed = Math.min(BURN_RATE, Math.min(held.getAmount(), outputTank.getSpace()));
		if (consumed <= 0)
			return;
		inputTank.drain(consumed, FluidAction.EXECUTE);
		outputTank.fill(new FluidStack(MekanismFluids.SODIUM.get(), consumed), FluidAction.EXECUTE);
	}

	@Override
	public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.put("InputTank", inputTank.writeToNBT(registries, new CompoundTag()));
		tag.put("OutputTank", outputTank.writeToNBT(registries, new CompoundTag()));
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		inputTank.readFromNBT(registries, tag.getCompound("InputTank"));
		outputTank.readFromNBT(registries, tag.getCompound("OutputTank"));
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<SodiumBurnerBlockEntity> type) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (be, side) -> {
			if (side == be.inputSide())
				return be.inputTank;
			if (side == be.outputSide())
				return be.outputTank;
			return null;
		});
	}

	/** Wires this block into Create's own heat-source registry - see the class doc. */
	public static void registerHeatSource(Block block) {
		BoilerHeater.REGISTRY.register(block, (level, pos, state) -> {
			if (!(level.getBlockEntity(pos) instanceof SodiumBurnerBlockEntity burner))
				return BoilerHeater.NO_HEAT;
			return burner.heatLevel();
		});
	}
}
