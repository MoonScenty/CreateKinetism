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

import me.moonscenty.createkinetism.foundation.CKLang;
import me.moonscenty.createkinetism.foundation.SidedChemicalAccess;
import me.moonscenty.createkinetism.registry.CKChemicals;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import org.jetbrains.annotations.Nullable;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>Halves whatever is in its tanks every half second and throws flame at the sky while it does.
 * The more it is burning, the bigger the flare - which makes it a rough but readable gauge of how
 * much of your refinery output you are throwing away.</p>
 *
 * <p>Two tanks, one of each kind. A real flare burns gas, and most of what a refinery cannot use
 * here is gas - but the gases in this mod are Mekanism chemicals, which a fluid tank cannot hold.
 * So the block offers both handlers on the same face: a fluid pipe goes where a fluid pipe went,
 * and a pressurized tube (or a Gas Pump) goes in the same place for the rest.</p>
 */
public class FlarestackBlockEntity extends SmartBlockEntity
	implements IHaveGoggleInformation, IMekanismChemicalHandler {

	private static final int CAPACITY = 4000;

	public SmartFluidTankBehaviour tank;

	/**
	 * The gas half. Same size as the fluid tank, and burnt on the same schedule.
	 *
	 * <p>Only {@link CKChemicals#TYPE_OIL_GAS} goes in. A stack that took anything would take the
	 * polonium somebody misrouted into it and burn that too, and nothing brings it back - so the
	 * refusal happens at the inlet, where a tube can still see it fail.</p>
	 */
	public final IChemicalTank chemicalTank =
		BasicChemicalTank.input(CAPACITY, chemical -> chemical.is(CKChemicals.TYPE_OIL_GAS), this);

	private final List<IChemicalTank> tanks = List.of(chemicalTank);

	public FlarestackBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		tank = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.TYPE, this, 1, CAPACITY, true);
		behaviours.add(tank);
	}

	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		return tanks;
	}

	@Override
	public void onContentsChanged() {
		setChanged();
		sendData();
	}

	/** Everything waiting to be burnt, of either kind. Drives the size of the flare. */
	private int burning() {
		return tank.getPrimaryHandler()
			.getFluidAmount() + (int) chemicalTank.getStored();
	}

	@Override
	public void tick() {
		super.tick();
		if (!level.isClientSide)
			return;

		int amount = burning();
		if (amount == 0)
			return;

		Vec3 offset = new Vec3(0, 0.024f, 0);
		Vec3 particlePos = Vec3.atCenterOf(worldPosition.above());
		Vec3 velocity = VecHelper.offsetRandomly(offset, level.random, 0.03f)
			.multiply(1, 5f, 1);

		for (int i = 0; i < (amount / 20) + 1; i++)
			level.addParticle(ParticleTypes.FLAME, particlePos.x + offset.x, particlePos.y + offset.y,
				particlePos.z + offset.z, velocity.x, velocity.y, velocity.z);

		if (level.random.nextInt(4) == 1)
			level.addParticle(ParticleTypes.LARGE_SMOKE, particlePos.x + offset.x, particlePos.y + offset.y,
				particlePos.z + offset.z, velocity.x / 5, velocity.y, velocity.z / 5);

		if (level.random.nextInt(5) == 1)
			level.playLocalSound(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
				SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.AMBIENT, 0.5f, 0.5f, false);
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (level.isClientSide)
			return;
		int fluid = tank.getPrimaryHandler()
			.getFluidAmount();
		tank.getPrimaryHandler()
			.drain(Mth.ceil(fluid / 2f), FluidAction.EXECUTE);
		long gas = chemicalTank.getStored();
		if (gas > 0)
			chemicalTank.extract((gas + 1) / 2, Action.EXECUTE, AutomationType.INTERNAL);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean added = containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability());

		ChemicalStack gas = chemicalTank.getStack();
		if (gas.isEmpty())
			return added;
		CKLang.builder()
			.add(Component.translatable(gas.getChemical()
				.getTranslationKey()))
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip);
		CKLang.builder()
			.text(gas.getAmount() + " / " + CAPACITY + "mB")
			.style(ChatFormatting.GOLD)
			.forGoggles(tooltip, 1);
		return true;
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<FlarestackBlockEntity> type) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type,
			(be, context) -> context == null || context == Direction.DOWN ? be.tank.getCapability() : null);
		// The same face. A tube goes where a pipe would have.
		event.registerBlockEntity(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), type,
			(be, context) -> context == null || context == Direction.DOWN
				? new SidedChemicalAccess(be, context)
				: null);
	}
}
