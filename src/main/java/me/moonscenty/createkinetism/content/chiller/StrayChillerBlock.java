package me.moonscenty.createkinetism.content.chiller;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;
import me.moonscenty.createkinetism.registry.CKBlocks;
import me.moonscenty.createkinetism.registry.CKItems;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The Stray Chiller: for now, a Blaze Burner in every way but its textures.
 *
 * <p>Subclassed rather than copied, because nearly everything that makes a Blaze Burner useful lives
 * outside the burner and asks about the block, not the class. Basins, boilers, fans and train
 * conductors all read {@link BlazeBurnerBlock#HEAT_LEVEL} off the state, and that property is only
 * the same object - and so only recognised - if this block inherits it. A copied block with its own
 * "blaze" property would look identical and heat nothing.</p>
 *
 * <p>What is overridden is only what Create pins to its own registry entries: the block entity type,
 * the items this block drops and is picked as, and how its own item places it. Flint and steel is
 * refused rather than inherited - on a Blaze Burner it swaps the block for Create's Lit Blaze Burner,
 * which here would turn a chiller into a different mod's block.</p>
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class StrayChillerBlock extends BlazeBurnerBlock {

	public static final MapCodec<StrayChillerBlock> CODEC = simpleCodec(StrayChillerBlock::new);

	public StrayChillerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntityType<? extends BlazeBurnerBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.STRAY_CHILLER.get();
	}

	/** Captured or empty is decided by which of our two items placed it - see StrayChillerBlockItem. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		boolean captured = context.getItemInHand()
			.getItem() instanceof StrayChillerBlockItem item && item.hasCapturedBlaze();
		return defaultBlockState().setValue(HEAT_LEVEL, captured ? HeatLevel.SMOULDERING : HeatLevel.NONE)
			.setValue(FACING, context.getHorizontalDirection()
				.getOpposite());
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
		Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (state.getValue(HEAT_LEVEL) == HeatLevel.NONE && stack.getItem() instanceof FlintAndSteelItem)
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
		Player player) {
		return stackFor(state);
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state, @Nullable BlockEntity blockEntity) {
		return new ItemRequirement(ItemUseType.CONSUME, stackFor(state));
	}

	private static ItemStack stackFor(BlockState state) {
		return state.getValue(HEAT_LEVEL) == HeatLevel.NONE ? CKItems.EMPTY_STRAY_CHILLER.asStack()
			: CKBlocks.STRAY_CHILLER.asStack();
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}
}
