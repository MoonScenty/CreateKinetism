package me.moonscenty.createkinetism.content.oil;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.pipes.AxisPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;

import me.moonscenty.createkinetism.content.recipe.PumpjackRecipe;
import me.moonscenty.createkinetism.foundation.CKLang;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.crafting.RecipeHolder;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>The wellhead. It only produces if a column of Create fluid pipes runs from directly underneath
 * it all the way down to bedrock, which is what makes siting a well a real decision rather than
 * placing a block. What comes up is decided by the biome, through {@link PumpjackRecipe}.</p>
 *
 * <p>Wells within eight blocks of each other are drawing on the same pocket, so each neighbour costs
 * a quarter of the yield. Spreading a field out beats stacking wells in one spot.</p>
 */
public class PumpjackWellBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

	public static final int TANK_CAPACITY = 2000;
	private static final int INTERFERENCE_RANGE = 8;
	private static final float INTERFERENCE_PENALTY = 0.75f;

	/** Every loaded well on the server, so neighbours can be found without a world scan. */
	public static final List<BlockPos> LOADED_WELLS = new ArrayList<>();

	public SmartFluidTankBehaviour tank;
	public PumpjackRecipe currentRecipe;
	public float efficiency = 1f;
	public boolean isPipingValid = false;

	public PumpjackWellBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		tank = SmartFluidTankBehaviour.single(this, TANK_CAPACITY);
		tank.forbidInsertion();
		behaviours.add(tank);
	}

	@Override
	public void setLevel(Level level) {
		if (!hasLevel() && !level.isClientSide)
			LOADED_WELLS.add(getBlockPos());
		super.setLevel(level);
	}

	@Override
	public void remove() {
		super.remove();
		if (level != null && !level.isClientSide)
			LOADED_WELLS.remove(getBlockPos());
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		if (level != null && !level.isClientSide)
			LOADED_WELLS.remove(getBlockPos());
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		isPipingValid = validatePiping();
	}

	/** A well needs an unbroken run of Create fluid pipe from just below it down to bedrock. */
	public boolean validatePiping() {
		if (level == null)
			return true;
		BlockPos position = getBlockPos().below();
		BlockState state = level.getBlockState(position);
		while (!state.is(Blocks.BEDROCK)) {
			if (!(state.getBlock() instanceof FluidPipeBlock) && !(state.getBlock() instanceof AxisPipeBlock))
				return false;
			position = position.below();
			if (level.isOutsideBuildHeight(position))
				return false;
			state = level.getBlockState(position);
		}
		return true;
	}

	public void updateRecipe() {
		if (level == null)
			return;
		if (currentRecipe != null && currentRecipe.matchesBiome(level.getBiome(getBlockPos())))
			return;

		currentRecipe = null;
		for (RecipeHolder<PumpjackRecipe> holder : level.getRecipeManager()
			.getAllRecipesFor(CKRecipeTypes.PUMPJACK.<net.minecraft.world.item.crafting.RecipeInput, PumpjackRecipe>getType()))
			if (holder.value()
				.matchesBiome(level.getBiome(getBlockPos()))) {
				currentRecipe = holder.value();
				return;
			}
	}

	public void updateEfficiency() {
		BlockPos pos = getBlockPos();
		float updated = 1f;
		for (BlockPos other : LOADED_WELLS) {
			if (pos.equals(other))
				continue;
			if (pos.closerThan(other.atY(pos.getY()), INTERFERENCE_RANGE))
				updated *= INTERFERENCE_PENALTY;
		}
		if (updated != efficiency) {
			efficiency = updated;
			sendData();
		}
	}

	/** Called once per stroke of the walking beam above. */
	public void pump() {
		if (currentRecipe == null || !isPipingValid || isTankFull())
			return;

		updateEfficiency();

		FluidStack result = currentRecipe.getFluidResult()
			.copy();
		result.setAmount((int) (result.getAmount() * efficiency));
		if (result.isEmpty())
			return;

		tank.allowInsertion();
		tank.getPrimaryHandler()
			.fill(result, FluidAction.EXECUTE);
		tank.forbidInsertion();
	}

	public boolean isTankFull() {
		return tank.getPrimaryHandler()
			.getFluidAmount() >= TANK_CAPACITY;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		if (efficiency < 1f) {
			CKLang.translate("gui.pumpjack_well.efficiency")
				.text(" " + (int) (efficiency * 100f) + "%")
				.forGoggles(tooltip);
			CKLang.translate("gui.pumpjack_well.other_wells")
				.style(ChatFormatting.GRAY)
				.forGoggles(tooltip);
		}
		if (!isPipingValid)
			CKLang.translate("gui.pumpjack_well.no_pipes")
				.style(ChatFormatting.RED)
				.forGoggles(tooltip);

		return containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability());
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.putFloat("Efficiency", efficiency);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		efficiency = tag.getFloat("Efficiency");
	}
}
