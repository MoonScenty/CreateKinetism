package me.moonscenty.createkinetism.content.chemical;

import java.util.List;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;

import me.moonscenty.createkinetism.content.recipe.ConvertingRecipe;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Holds a solid infusion source, dissolves it, and feeds the machine below.
 *
 * <p>There is deliberately no shaft and no processing time. In Mekanism this conversion is not a
 * machine step at all - it happens inside the infuser the moment you drop redstone into its slot -
 * so charging the tank costs nothing but the item. What it does cost is a block of space directly
 * above whatever it feeds, since the fluid only ever moves downwards.</p>
 */
public class ChemicalTankBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

	/** Ten Enriched items' worth, at Mekanism's 80-per-infusion scale. */
	public static final int CAPACITY = 800;

	/**
	 * Per tick. The infuser draws 80mB per operation and cannot run one in under 20 ticks, so 4mB/t is
	 * all it can actually consume - this leaves comfortable headroom without emptying the tank at once.
	 */
	private static final int PUSH_RATE = 10;

	public SmartFluidTankBehaviour tank;

	private final ItemStackHandler inventory = new ItemStackHandler(1) {
		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return findRecipe(stack) != null;
		}
	};

	public ChemicalTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<ChemicalTankBlockEntity> type) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, context) -> be.inventory);
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type,
			(be, context) -> be.tank == null ? null : be.tank.getCapability());
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		tank = SmartFluidTankBehaviour.single(this, CAPACITY);
		behaviours.add(tank);
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		dissolveOne();
		pushDown();
	}

	/** One item per tick at most, and only when the whole yield fits - no half-dissolved items. */
	private void dissolveOne() {
		ItemStack stack = inventory.getStackInSlot(0);
		if (stack.isEmpty())
			return;

		ConvertingRecipe recipe = findRecipe(stack);
		if (recipe == null)
			return;

		List<FluidStack> results = recipe.getFluidResults();
		if (results.isEmpty())
			return;

		FluidStack yield = results.get(0)
			.copy();
		if (tank.getPrimaryHandler()
			.fill(yield, IFluidHandler.FluidAction.SIMULATE) != yield.getAmount())
			return;

		tank.getPrimaryHandler()
			.fill(yield, IFluidHandler.FluidAction.EXECUTE);
		stack.shrink(1);
		notifyUpdate();
	}

	/**
	 * Downwards only. Anything that accepts fluid works - the Metallurgic Infuser is the point, but a
	 * pipe or a tank underneath is a legitimate way to route the infusion somewhere else.
	 */
	private void pushDown() {
		FluidStack held = tank.getPrimaryHandler()
			.getFluid();
		if (held.isEmpty())
			return;

		IFluidHandler below =
			level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.below(), Direction.UP);
		if (below == null)
			return;

		FluidStack offer = held.copy();
		offer.setAmount(Math.min(PUSH_RATE, held.getAmount()));
		int moved = below.fill(offer, IFluidHandler.FluidAction.EXECUTE);
		if (moved <= 0)
			return;

		tank.getPrimaryHandler()
			.drain(moved, IFluidHandler.FluidAction.EXECUTE);
		notifyUpdate();
	}

	private ConvertingRecipe findRecipe(ItemStack stack) {
		if (level == null || stack.isEmpty())
			return null;
		SingleRecipeInput input = new SingleRecipeInput(stack);
		for (RecipeHolder<ConvertingRecipe> holder : level.getRecipeManager()
			.getAllRecipesFor(CKRecipeTypes.CONVERTING.<RecipeInput, ConvertingRecipe>getType())) {
			if (holder.value()
				.matches(input, level))
				return holder.value();
		}
		return null;
	}

	public ItemStackHandler getInventory() {
		return inventory;
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(compound, registries, clientPacket);
		compound.put("Inventory", inventory.serializeNBT(registries));
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		if (compound.contains("Inventory"))
			inventory.deserializeNBT(registries, compound.getCompound("Inventory"));
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		return containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability());
	}
}
