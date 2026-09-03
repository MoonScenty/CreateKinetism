package me.moonscenty.createkinetism.content.vat;

import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.foundation.item.ItemHelper;

import me.moonscenty.createkinetism.content.recipe.CombinerRecipe;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import org.jetbrains.annotations.Nullable;

/**
 * A vat that also holds one item of its own.
 *
 * <p>Mekanism's Combiner has an input slot and a separate infusion slot. The basin under this block
 * is the first; this inventory is the second. The machine carries the dust and presses it into the
 * cobblestone below, rather than everything being tipped into the basin together.</p>
 *
 * <p>That split is why the Combiner is not just another vat registration. Create's basin matching
 * knows only about the basin, so the infusion condition has to be added on either side of it: the
 * recipe cannot start unless this slot satisfies it, and one item leaves the slot each time a batch
 * completes.</p>
 */
public class CombinerBlockEntity extends VatBlockEntity {

	/** One slot, one stack. Filled by hand or by any funnel, hopper or belt pointed at the block. */
	public ItemStackHandler infusionInv = new ItemStackHandler(1) {
		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
			sendData();
			basinChecker.scheduleUpdate();
		}
	};

	public CombinerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public ItemStack getHeldInfusion() {
		return infusionInv.getStackInSlot(0);
	}

	/**
	 * The top face only. The infusion item is dropped in from above, the way the machine then presses
	 * it downwards - and it keeps the sides clear, which matters because a vat sits one block over a
	 * basin and the gap between them is where hoppers and funnels for the bulk material run.
	 *
	 * <p>A null side is the caller asking without a face at all, and still gets the inventory.</p>
	 */
	@Nullable
	public IItemHandler getItemHandler(@Nullable Direction side) {
		return side == null || side == Direction.UP ? infusionInv : null;
	}

	/**
	 * The basin half is Create's; the infusion half is ours. Both have to hold, or the head never
	 * comes down - which is the behaviour you want when someone has filled the basin but not the
	 * machine.
	 */
	@Override
	protected <I extends RecipeInput> boolean matchBasinRecipe(Recipe<I> recipe) {
		if (!(recipe instanceof CombinerRecipe combiner))
			return false;
		if (!combiner.matchesInfusion(getHeldInfusion()))
			return false;
		return getBasin().map(basin -> BasinRecipe.match(basin, recipe))
			.orElse(false);
	}

	/** One infusion item per batch, consumed as the basin half is applied. */
	@Override
	protected void applyBasinRecipe() {
		super.applyBasinRecipe();
		if (!getHeldInfusion().isEmpty())
			infusionInv.extractItem(0, 1, false);
	}

	@Override
	protected boolean matchStaticFilters(RecipeHolder<? extends Recipe<?>> recipe) {
		return recipe.value() instanceof CombinerRecipe;
	}

	@Override
	public void destroy() {
		super.destroy();
		ItemHelper.dropContents(level, worldPosition, infusionInv);
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.put("Infusion", infusionInv.serializeNBT(registries));
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		infusionInv.deserializeNBT(registries, compound.getCompound("Infusion"));
		super.read(compound, registries, clientPacket);
	}
}
