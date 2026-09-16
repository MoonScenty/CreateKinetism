package me.moonscenty.createkinetism.content.machine;


import com.simibubi.create.AllBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * A machine that carries its own Create Basin instead of standing over one.
 *
 * <p>Most machines in this mod work on a real Basin placed below them, which is what buys them
 * piping and hoppers for free. These cannot: the basin has to move with the head, and a block in the
 * world cannot be moved a pixel. So the basin is installed <em>into</em> the machine - held as an
 * item, drawn by the renderer, moved along with everything else - while the inventory and tanks come
 * from {@link ProcessingMachineBlockEntity}.</p>
 *
 * <p>With no basin installed there is no inventory at all. That is deliberate: an empty cradle should
 * refuse items rather than swallow them.</p>
 */
public abstract class BasinCarryingBlockEntity extends ProcessingMachineBlockEntity {

	private ItemStack installedBasin = ItemStack.EMPTY;

	public BasinCarryingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** Nothing works, and nothing may be inserted, until a basin has been fitted. */
	@Override
	protected boolean canOperate() {
		return hasBasin();
	}

	public boolean hasBasin() {
		return !installedBasin.isEmpty();
	}

	public ItemStack getInstalledBasin() {
		return installedBasin;
	}

	public boolean installBasin(ItemStack stack) {
		if (hasBasin() || !stack.is(AllBlocks.BASIN.asItem()))
			return false;
		installedBasin = stack.copyWithCount(1);
		contentsChanged = true;
		basinChanged();
		return true;
	}

	/** Takes the basin back out. Whatever it was holding comes with it. */
	public ItemStack removeBasin() {
		if (!hasBasin())
			return ItemStack.EMPTY;
		ItemStack basin = installedBasin;
		installedBasin = ItemStack.EMPTY;
		stopRunning();
		basinChanged();
		return basin;
	}

	/**
	 * Fitting or pulling a basin changes what this machine offers to the world, so say so.
	 *
	 * <p>{@link ProcessingMachineBlockEntity#registerCapabilities} hands out its handlers only while
	 * {@link #canOperate()} holds, and here that is {@link #hasBasin()}. A neighbour caches what it was
	 * given - a null included - and NeoForge only re-asks when the block invalidates. Without this a
	 * pipe that found an empty cradle keeps seeing nothing after the basin goes in, and one that
	 * latched onto a fitted machine keeps holding the handler after the basin comes out.</p>
	 */
	private void basinChanged() {
		setChanged();
		sendData();
		invalidateCapabilities();
	}

	@Override
	protected AABB createRenderBoundingBox() {
		// The basin rides a block above us.
		return new AABB(worldPosition).expandTowards(0, 1.5, 0);
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		if (hasBasin())
			compound.put("Basin", installedBasin.save(registries));
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		installedBasin = compound.contains("Basin")
			? ItemStack.parseOptional(registries, compound.getCompound("Basin"))
			: ItemStack.EMPTY;
		super.read(compound, registries, clientPacket);
	}
}
