package me.moonscenty.createkinetism.content.turbine;

import java.util.List;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;

import mekanism.api.MekanismAPI;
import mekanism.api.chemical.BasicChemicalTank;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.api.Action;
import mekanism.api.AutomationType;

import me.moonscenty.createkinetism.foundation.CKLang;
import me.moonscenty.createkinetism.foundation.SidedChemicalAccess;

import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import org.jetbrains.annotations.Nullable;

/**
 * One block of a Turbine: steam in, water out, rotation out through a {@link TurbineBearingBlockEntity}
 * under the middle of the floor.
 *
 * <p>Built the way Create builds a Fluid Tank. Casings placed next to each other join up through
 * Create's own {@link ConnectivityHandler} into the largest square-footed box they can make - up to
 * {@value #MAX_WIDTH} wide and {@value #MAX_HEIGHT} tall - and the lowest north-west block becomes the
 * controller that owns the contents. There is no formed/unformed switch and no GUI.</p>
 *
 * <p>It turns only when the box is a real turbine: an odd width of at least 3, so there is a middle for
 * the rotor to run up; at least 2 tall; and a Turbine Bearing under the middle of the floor.</p>
 *
 * <p>The box is two parts. Every floor but the top one is the rotor; the top floor is the tank the spent
 * steam condenses into - no blades, and the only place the water can be taken from. Every casing in the
 * box, both parts alike, lets {@value #FLOW_PER_CASING} mB of steam a tick through - see
 * {@link #maxFlow}.</p>
 *
 * <ul>
 *   <li>Steam - anything in {@code #mekanism:water_vapor} - comes in by pressurized tube through any face
 *       of the rotor floors, and cannot be pulled back out.</li>
 *   <li>Every mB of steam used condenses into a mB of water in the top floor's tank, which pipes can drain
 *       through the top floor's faces - and only those - and never fill. A full water tank stops the
 *       turbine.</li>
 *   <li>Each mB of steam a tick is worth {@value #SU_PER_MB} SU at {@value #SPEED} RPM, averaged over a
 *       second so the bearing does not reshuffle the network's stress every tick.</li>
 * </ul>
 *
 * <p>Steam is held by the controller alone. When the box is split, the water is shared out the way a
 * Fluid Tank shares its fluid; the steam stays with whichever block was the controller.</p>
 */
public class TurbineCasingBlockEntity extends SmartBlockEntity
	implements IMultiBlockEntityContainer.Fluid, IMekanismChemicalHandler, IHaveGoggleInformation {

	public static final int MAX_WIDTH = 7;
	public static final int MAX_HEIGHT = 16;

	/**
	 * Water held per casing, in mB: whatever a Create Fluid Tank holds per block (8 buckets unless a pack
	 * changes {@code fluidTankCapacity}), so a turbine's water tank is as big as a Fluid Tank of the same
	 * size. A method rather than a constant because it is Create's server config.
	 */
	public static int waterPerBlock() {
		return FluidTankBlockEntity.getCapacityMultiplier();
	}
	/** Steam held per casing, in mB. */
	public static final long STEAM_PER_BLOCK = 1000;

	/** Steam a tick each casing in the box lets through, in mB. */
	public static final int FLOW_PER_CASING = 100;
	/** Stress capacity one mB of steam a tick buys, in SU. */
	public static final float SU_PER_MB = 1024;
	/** The bearing's fixed speed. */
	public static final float SPEED = 64;

	/** What counts as steam: Mekanism's own tag, which holds both water vapour and steam. */
	public static final TagKey<Chemical> STEAM =
		TagKey.create(MekanismAPI.CHEMICAL_REGISTRY_NAME, ResourceLocation.fromNamespaceAndPath("mekanism", "water_vapor"));

	protected BlockPos controller;
	protected BlockPos lastKnownPos;
	protected boolean updateConnectivity;
	protected int width = 1;
	protected int height = 1;
	protected boolean window = true;

	protected final SmartFluidTank waterTank;
	private final IFluidHandler waterCapability;
	private IChemicalTank steamTank;
	private boolean contentsDirty;

	/** Steam used over the last second, per tick. What the bearing turns into stress capacity. */
	private float averageFlow;
	private int flowSum;
	private int flowTicks;

	public TurbineCasingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		waterTank = new SmartFluidTank(waterPerBlock(), $ -> onContentsChanged());
		waterCapability = new DrainOnly(waterTank);
		steamTank = newSteamTank(STEAM_PER_BLOCK);
		updateConnectivity = true;
	}

	private IChemicalTank newSteamTank(long capacity) {
		return BasicChemicalTank.inputModern(capacity, stack -> stack.is(STEAM), this);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	// ------------------------------------------------------------------ connectivity

	protected void updateConnectivity() {
		updateConnectivity = false;
		if (level.isClientSide)
			return;
		if (!isController())
			return;
		ConnectivityHandler.formMulti(this);
	}

	public void updateConnectivityExternally() {
		updateConnectivity();
	}

	@Override
	public BlockPos getController() {
		return isController() ? worldPosition : controller;
	}

	@Override
	public boolean isController() {
		return controller == null || worldPosition.equals(controller);
	}

	@Override
	@SuppressWarnings("unchecked")
	@Nullable
	public TurbineCasingBlockEntity getControllerBE() {
		if (isController())
			return this;
		BlockEntity be = level.getBlockEntity(controller);
		return be instanceof TurbineCasingBlockEntity found ? found : null;
	}

	@Override
	public void setController(BlockPos controller) {
		if (level.isClientSide && !isVirtual())
			return;
		if (controller.equals(this.controller))
			return;
		this.controller = controller;
		invalidateCapabilities();
		setChanged();
		sendData();
	}

	@Override
	public void removeController(boolean keepContents) {
		if (level.isClientSide)
			return;
		updateConnectivity = true;
		if (!keepContents)
			setTankSize(0, 1);
		controller = null;
		width = 1;
		height = 1;
		averageFlow = 0;

		BlockState state = getBlockState();
		if (TurbineCasingBlock.isCasing(state))
			level.setBlock(worldPosition, state.setValue(TurbineCasingBlock.BOTTOM, true)
				.setValue(TurbineCasingBlock.TOP, true)
				.setValue(TurbineCasingBlock.SHAPE, window ? FluidTankBlock.Shape.WINDOW : FluidTankBlock.Shape.PLAIN),
				Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);

		invalidateCapabilities();
		setChanged();
		sendData();
	}

	@Override
	public BlockPos getLastKnownPos() {
		return lastKnownPos;
	}

	@Override
	public void preventConnectivityUpdate() {
		updateConnectivity = false;
	}

	@Override
	public void notifyMultiUpdated() {
		BlockState state = getBlockState();
		if (TurbineCasingBlock.isCasing(state))
			level.setBlock(worldPosition, state
				.setValue(TurbineCasingBlock.BOTTOM, getController().getY() == worldPosition.getY())
				.setValue(TurbineCasingBlock.TOP, getController().getY() + height - 1 == worldPosition.getY()), 6);
		if (isController())
			setWindows(window);
		invalidateCapabilities();
		setChanged();
	}

	@Override
	public Direction.Axis getMainConnectionAxis() {
		return Direction.Axis.Y;
	}

	@Override
	public int getMaxLength(Direction.Axis longAxis, int width) {
		return MAX_HEIGHT;
	}

	@Override
	public int getMaxWidth() {
		return MAX_WIDTH;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void setHeight(int height) {
		this.height = height;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public void setWidth(int width) {
		this.width = width;
	}

	@Override
	public boolean hasTank() {
		return true;
	}

	/** Per block, the way Create's tank answers it. */
	@Override
	public int getTankSize(int tank) {
		return waterPerBlock();
	}

	/** Both tanks follow the size of the box; anything past the new capacity is lost. */
	@Override
	public void setTankSize(int tank, int blocks) {
		waterTank.setCapacity(blocks * waterPerBlock());
		if (waterTank.getSpace() < 0)
			waterTank.drain(-waterTank.getSpace(), FluidAction.EXECUTE);

		ChemicalStack held = steamTank.getStack();
		steamTank = newSteamTank(blocks * STEAM_PER_BLOCK);
		if (!held.isEmpty())
			steamTank.setStack(held.copyWithAmount(Math.min(held.getAmount(), steamTank.getCapacity())));
		invalidateCapabilities();
	}

	@Override
	public IFluidTank getTank(int tank) {
		return waterTank;
	}

	@Override
	public FluidStack getFluid(int tank) {
		return waterTank.getFluid()
			.copy();
	}

	// ------------------------------------------------------------------ windows

	public void toggleWindows() {
		TurbineCasingBlockEntity be = getControllerBE();
		if (be != null)
			be.setWindows(!be.window);
	}

	/**
	 * Windows go on the side faces, leaving the corner columns and the top and bottom floors as the
	 * frame, so each side shows one big pane the rotor can be seen through - see
	 * {@link TurbineCasingModel}. Never on the blocks inside the box. A turbine only 2 tall has no middle
	 * floor and so no windows.
	 */
	public void setWindows(boolean window) {
		this.window = window;
		for (int yOffset = 0; yOffset < height; yOffset++)
			for (int xOffset = 0; xOffset < width; xOffset++)
				for (int zOffset = 0; zOffset < width; zOffset++) {
					BlockPos pos = worldPosition.offset(xOffset, yOffset, zOffset);
					BlockState blockState = level.getBlockState(pos);
					if (!TurbineCasingBlock.isCasing(blockState))
						continue;

					boolean edgeX = xOffset == 0 || xOffset == width - 1;
					boolean edgeZ = zOffset == 0 || zOffset == width - 1;
					boolean middleFloor = yOffset > 0 && yOffset < height - 1;
					FluidTankBlock.Shape shape = window && edgeX != edgeZ && middleFloor ? FluidTankBlock.Shape.WINDOW
						: FluidTankBlock.Shape.PLAIN;

					level.setBlock(pos, blockState.setValue(TurbineCasingBlock.SHAPE, shape), 22);
					level.getChunkSource()
						.getLightEngine()
						.checkBlock(pos);
				}
	}

	// ------------------------------------------------------------------ the turbine

	/** Where the Turbine Bearing has to be: under the middle of the floor. */
	public BlockPos bearingPos() {
		return worldPosition.offset(width / 2, -1, width / 2);
	}

	/** A box the rotor can run in, with its bearing under it. Asked of the controller. */
	public boolean isTurbine() {
		return width >= 3 && width % 2 == 1 && height >= 2
			&& level.getBlockEntity(bearingPos()) instanceof TurbineBearingBlockEntity;
	}

	/** Floors with blades on them: every floor but the top, which is the condensate tank. */
	public int rotorFloors() {
		return height - 1;
	}

	/**
	 * Steam the turbine can pass a tick: every casing in the box x {@value #FLOW_PER_CASING} mB. A 3x3x2
	 * turbine passes 1,800 mB/t; a 7x7x16 one, the largest, 78,400.
	 */
	public int maxFlow() {
		return width * width * height * FLOW_PER_CASING;
	}

	/** Whether this casing is on the turbine's top floor - the condensate tank. */
	public boolean isOnTankFloor() {
		TurbineCasingBlockEntity controllerBE = getControllerBE();
		return controllerBE != null
			&& worldPosition.getY() == controllerBE.getBlockPos().getY() + controllerBE.height - 1;
	}

	public float getAverageFlow() {
		return averageFlow;
	}

	public boolean isRunning() {
		return averageFlow > 0;
	}

	@Override
	public void tick() {
		super.tick();

		if (lastKnownPos == null)
			lastKnownPos = worldPosition;
		else if (!lastKnownPos.equals(worldPosition)) {
			removeController(true);
			lastKnownPos = worldPosition;
			return;
		}

		if (updateConnectivity)
			updateConnectivity();
		if (!isController())
			return;

		if (level.isClientSide) {
			if (isRunning())
				vent();
			return;
		}

		int flow = 0;
		if (isTurbine()) {
			long steam = steamTank.getStored();
			flow = (int) Math.min(Math.min(steam, maxFlow()), waterTank.getSpace());
			if (flow > 0) {
				steamTank.extract(flow, Action.EXECUTE, AutomationType.INTERNAL);
				waterTank.fill(new FluidStack(Fluids.WATER, flow), FluidAction.EXECUTE);
			}
		}

		flowSum += flow;
		if (++flowTicks >= 20) {
			float average = flowSum / 20f;
			flowSum = 0;
			flowTicks = 0;
			if (average != averageFlow) {
				averageFlow = average;
				sendData();
				if (level.getBlockEntity(bearingPos()) instanceof TurbineBearingBlockEntity bearing)
					bearing.updateFromTurbine();
			}
		}
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (contentsDirty && !level.isClientSide) {
			contentsDirty = false;
			sendData();
		}
	}

	/** Vapour puffing out of the top of a running turbine - the vent the steam leaves by. */
	private void vent() {
		RandomSource random = level.random;
		if (random.nextInt(3) != 0)
			return;
		double x = worldPosition.getX() + random.nextDouble() * width;
		double z = worldPosition.getZ() + random.nextDouble() * width;
		double y = worldPosition.getY() + height + 0.05;
		if (!level.getBlockState(BlockPos.containing(x, y, z))
			.isAir())
			return;
		level.addParticle(ParticleTypes.CLOUD, x, y, z, 0, 0.06 + random.nextDouble() * 0.04, 0);
	}

	@Override
	public void onContentsChanged() {
		setChanged();
		contentsDirty = true;
	}

	// ------------------------------------------------------------------ capabilities

	@Override
	public List<IChemicalTank> getChemicalTanks(@Nullable Direction side) {
		TurbineCasingBlockEntity controllerBE = getControllerBE();
		return controllerBE == null ? List.of() : List.of(controllerBE.steamTank);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<TurbineCasingBlockEntity> type) {
		// Steam into the rotor floors; water out of the tank floor alone.
		event.registerBlockEntity(mekanism.common.capabilities.Capabilities.CHEMICAL.block(), type,
			(be, side) -> be.isOnTankFloor() ? null : new SidedChemicalAccess(be, side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (be, side) -> {
			if (!be.isOnTankFloor())
				return null;
			TurbineCasingBlockEntity controllerBE = be.getControllerBE();
			return controllerBE == null ? null : controllerBE.waterCapability;
		});
	}

	/** Pipes may take water out of a turbine, never put any in: the water is what the steam became. */
	private record DrainOnly(IFluidHandler tank) implements IFluidHandler {

		@Override
		public int getTanks() {
			return tank.getTanks();
		}

		@Override
		public FluidStack getFluidInTank(int index) {
			return tank.getFluidInTank(index);
		}

		@Override
		public int getTankCapacity(int index) {
			return tank.getTankCapacity(index);
		}

		@Override
		public boolean isFluidValid(int index, FluidStack stack) {
			return false;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return 0;
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			return tank.drain(resource, action);
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			return tank.drain(maxDrain, action);
		}
	}

	// ------------------------------------------------------------------ persistence, display

	/**
	 * The whole box, for the controller - the rotor and membrane it draws fill it. Padded a little: the
	 * blade model dips half a pixel under its floor.
	 *
	 * <p>Create caches this the first time it is asked, and a controller is usually asked while it is
	 * still a lone block, before the turbine forms around it. Without {@link #initialize} and
	 * {@link #read} throwing the cached box away, the rotor stayed culled to that one block: stand close
	 * to the turbine, look up so the controller's corner leaves the screen, and the inside vanished.</p>
	 */
	@Override
	protected AABB createRenderBoundingBox() {
		if (!isController())
			return super.createRenderBoundingBox();
		return new AABB(worldPosition).expandTowards(width - 1, height - 1, width - 1)
			.inflate(0.1);
	}

	@Override
	public void initialize() {
		super.initialize();
		if (level.isClientSide)
			invalidateRenderBoundingBox();
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		if (updateConnectivity)
			compound.putBoolean("Uninitialized", true);
		if (lastKnownPos != null)
			compound.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
		if (!isController())
			compound.put("Controller", NbtUtils.writeBlockPos(controller));
		if (isController()) {
			compound.putBoolean("Window", window);
			compound.putInt("Size", width);
			compound.putInt("Height", height);
			compound.put("Water", waterTank.writeToNBT(registries, new CompoundTag()));
			compound.put("Steam", steamTank.serializeNBT(registries));
			compound.putFloat("Flow", averageFlow);
		}
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);

		BlockPos controllerBefore = controller;
		int prevWidth = width;
		int prevHeight = height;

		updateConnectivity = compound.contains("Uninitialized");
		lastKnownPos = compound.contains("LastKnownPos") ? NBTHelper.readBlockPos(compound, "LastKnownPos") : null;
		controller = compound.contains("Controller") ? NBTHelper.readBlockPos(compound, "Controller") : null;

		if (isController()) {
			window = compound.getBoolean("Window");
			width = compound.getInt("Size");
			height = compound.getInt("Height");
			int blocks = Math.max(1, width * width * height);
			waterTank.setCapacity(blocks * waterPerBlock());
			waterTank.readFromNBT(registries, compound.getCompound("Water"));
			steamTank = newSteamTank(blocks * STEAM_PER_BLOCK);
			if (compound.contains("Steam"))
				steamTank.deserializeNBT(registries, compound.getCompound("Steam"));
			averageFlow = compound.getFloat("Flow");
		}

		if (!clientPacket)
			return;
		boolean changeOfController =
			controllerBefore == null ? controller != null : !controllerBefore.equals(controller);
		if (changeOfController || prevWidth != width || prevHeight != height) {
			invalidateRenderBoundingBox();
			if (hasLevel())
				level.setBlocksDirty(worldPosition, Blocks.AIR.defaultBlockState(), getBlockState());
		}
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		TurbineCasingBlockEntity c = getControllerBE();
		if (c == null)
			return false;

		CKLang.translate("gui.turbine.title")
			.forGoggles(tooltip);
		if (!c.isTurbine()) {
			String reason = c.width < 3 || c.width % 2 == 0 ? "width" : c.height < 2 ? "height" : "bearing";
			CKLang.translate("gui.turbine.invalid." + reason)
				.style(ChatFormatting.RED)
				.forGoggles(tooltip, 1);
		}

		CKLang.translate("gui.turbine.flow", String.format("%.0f", c.averageFlow), c.maxFlow())
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip, 1);
		CKLang.translate("gui.turbine.output", String.format("%,.0f", c.averageFlow * SU_PER_MB))
			.style(ChatFormatting.GOLD)
			.forGoggles(tooltip, 1);
		CKLang.translate("gui.turbine.steam", c.steamTank.getStored(), c.steamTank.getCapacity())
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip, 1);
		CKLang.translate("gui.turbine.water", c.waterTank.getFluidAmount(), c.waterTank.getCapacity())
			.style(ChatFormatting.GRAY)
			.forGoggles(tooltip, 1);
		return true;
	}
}
