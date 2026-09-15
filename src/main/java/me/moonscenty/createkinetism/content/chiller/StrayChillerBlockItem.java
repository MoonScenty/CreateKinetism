package me.moonscenty.createkinetism.content.chiller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import me.moonscenty.createkinetism.CreateKinetism;
import me.moonscenty.createkinetism.registry.CKBlocks;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Create's {@code BlazeBurnerBlockItem}, pointed at this mod's chiller.
 *
 * <p>Copied rather than extended: its constructor is private, and its empty form is hard-wired to
 * Create's Blaze Burner, so an empty chiller built from it would place a Blaze Burner. The behaviour
 * is Create's, with the catch swapped: the empty item catches whatever is in {@link #CAPTURABLE} (a
 * stray, as shipped) instead of a blaze, from a spawner or by hand, and becomes a filled chiller.</p>
 *
 * <p>Both forms place the same block; {@link StrayChillerBlock#getStateForPlacement} reads which
 * one it was. Only the filled one claims the block as its item, so picking and drops map back to it.</p>
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class StrayChillerBlockItem extends BlockItem {

	/** What an empty chiller can catch - {@code createkinetism:stray_chiller_capturable}. */
	public static final TagKey<EntityType<?>> CAPTURABLE =
		TagKey.create(Registries.ENTITY_TYPE, CreateKinetism.asResource("stray_chiller_capturable"));

	private final boolean capturedBlaze;

	public static StrayChillerBlockItem empty(Properties properties) {
		return new StrayChillerBlockItem(CKBlocks.STRAY_CHILLER.get(), properties, false);
	}

	public static StrayChillerBlockItem withBlaze(Block block, Properties properties) {
		return new StrayChillerBlockItem(block, properties, true);
	}

	private StrayChillerBlockItem(Block block, Properties properties, boolean capturedBlaze) {
		super(block, properties);
		this.capturedBlaze = capturedBlaze;
	}

	@Override
	public void registerBlocks(Map<Block, Item> blockToItemMap, Item item) {
		if (!hasCapturedBlaze())
			return;
		super.registerBlocks(blockToItemMap, item);
	}

	/** The empty one is an item in its own right, not the block's, so it takes an item key. */
	@Override
	public String getDescriptionId() {
		return hasCapturedBlaze() ? super.getDescriptionId()
			: Util.makeDescriptionId("item", BuiltInRegistries.ITEM.getKey(this));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (hasCapturedBlaze())
			return super.useOn(context);

		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockEntity be = world.getBlockEntity(pos);
		Player player = context.getPlayer();

		if (!(be instanceof SpawnerBlockEntity spawnerBE))
			return super.useOn(context);

		BaseSpawner spawner = spawnerBE.getSpawner();

		List<SpawnData> possibleSpawns = spawner.spawnPotentials.unwrap()
			.stream()
			.map(Wrapper::data)
			.toList();

		if (possibleSpawns.isEmpty()) {
			possibleSpawns = new ArrayList<>();
			possibleSpawns.add(spawner.nextSpawnData);
		}

		for (SpawnData e : possibleSpawns) {
			Optional<EntityType<?>> optionalEntity = EntityType.by(e.entityToSpawn());
			if (optionalEntity.isEmpty() || !optionalEntity.get().is(CAPTURABLE))
				continue;

			spawnCaptureEffects(world, VecHelper.getCenterOf(pos));
			if (world.isClientSide || player == null)
				return InteractionResult.SUCCESS;

			giveFilledItemTo(player, context.getItemInHand(), context.getHand());
			return InteractionResult.SUCCESS;
		}

		return super.useOn(context);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack heldItem, Player player, LivingEntity entity,
		InteractionHand hand) {
		if (hasCapturedBlaze())
			return InteractionResult.PASS;
		if (!entity.getType().is(CAPTURABLE))
			return InteractionResult.PASS;

		Level world = player.level();
		spawnCaptureEffects(world, entity.position());
		if (world.isClientSide)
			return InteractionResult.FAIL;

		giveFilledItemTo(player, heldItem, hand);
		entity.discard();
		return InteractionResult.FAIL;
	}

	protected void giveFilledItemTo(Player player, ItemStack heldItem, InteractionHand hand) {
		ItemStack filled = CKBlocks.STRAY_CHILLER.asStack();
		if (!player.isCreative())
			heldItem.shrink(1);
		if (heldItem.isEmpty()) {
			player.setItemInHand(hand, filled);
			return;
		}
		player.getInventory()
			.placeItemBackInInventory(filled);
	}

	private void spawnCaptureEffects(Level world, Vec3 vec) {
		if (world.isClientSide) {
			for (int i = 0; i < 40; i++) {
				Vec3 motion = VecHelper.offsetRandomly(Vec3.ZERO, world.random, .125f);
				world.addParticle(ParticleTypes.SNOWFLAKE, vec.x, vec.y, vec.z, motion.x, motion.y, motion.z);
				Vec3 circle = motion.multiply(1, 0, 1)
					.normalize()
					.scale(.5f);
				world.addParticle(ParticleTypes.SMOKE, circle.x, vec.y, circle.z, 0, -0.125, 0);
			}
			return;
		}

		BlockPos soundPos = BlockPos.containing(vec);
		world.playSound(null, soundPos, SoundEvents.STRAY_HURT, SoundSource.HOSTILE, .25f, .75f);
		world.playSound(null, soundPos, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, .5f, .75f);
	}

	public boolean hasCapturedBlaze() {
		return capturedBlaze;
	}
}
