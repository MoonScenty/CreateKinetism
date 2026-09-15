package me.moonscenty.createkinetism.content.chiller;

import java.util.Map;

import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;

import me.moonscenty.createkinetism.content.heat.CKHeatLevels;

import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * A Blaze Burner's block entity that chills instead of heating.
 *
 * <p>Goggles, the stock keeper and egg feeding are inherited unchanged. Fuel is not: a chiller takes
 * only snow and ice ({@link #FUEL_SECONDS}), and anything in it makes the block
 * {@link CKHeatLevels#CHILLED}, where a Blaze Burner would be Kindled or Seething; with none it idles at
 * Smouldering, as a burner does.</p>
 *
 * <p>The head is turned here rather than by Create, for two reasons. With Flywheel on, Create hands
 * the animation to {@code BlazeBurnerVisual}, which this block does not have. And Create only counts a
 * burner as working from Fading up, which Chilled ranks below - so {@code BlazeBurnerBlockEntityMixin}
 * switches Create's animation off for this block entirely.</p>
 */
public class StrayChillerBlockEntity extends BlazeBurnerBlockEntity {

	/** How long each fuel keeps a chiller going, in seconds. Nothing else goes in - no coal, no Blaze Cake. */
	public static final Map<Item, Integer> FUEL_SECONDS = Map.of(
		Items.SNOWBALL, 10,
		Items.SNOW_BLOCK, 40,
		Items.ICE, 360,
		Items.PACKED_ICE, 3240,
		Items.BLUE_ICE, 3240);

	/** The most a chiller can be topped up to by hand - one Packed Ice's worth, in ticks. */
	private static final int MAX_CHILL_TIME = 3240 * 20;

	public StrayChillerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void tick() {
		super.tick();
		if (level != null && level.isClientSide)
			animateHead();
	}

	@Override
	protected HeatLevel getHeatLevel() {
		return activeFuel == FuelType.NONE ? HeatLevel.SMOULDERING : CKHeatLevels.CHILLED;
	}

	/**
	 * Create's {@code tryUpdateFuel} with the fuel list swapped for {@link #FUEL_SECONDS}. Every fuel is
	 * the same kind, so there is no Blaze Cake case; the top-up rule is Create's - an automated insert
	 * only lands once the chiller is nearly out, a player's hand tops it up to {@link #MAX_CHILL_TIME} -
	 * except that a full chiller refuses the item rather than eating it.
	 */
	@Override
	protected boolean tryUpdateFuel(ItemStack itemStack, boolean forceOverflow, boolean simulate) {
		if (isCreative)
			return false;

		Integer seconds = FUEL_SECONDS.get(itemStack.getItem());
		if (seconds == null)
			return false;
		int newBurnTime = seconds * 20;

		if (activeFuel != FuelType.NONE) {
			if (remainingBurnTime <= INSERTION_THRESHOLD)
				newBurnTime += remainingBurnTime;
			else if (forceOverflow && remainingBurnTime < MAX_CHILL_TIME)
				newBurnTime = Math.min(remainingBurnTime + newBurnTime, MAX_CHILL_TIME);
			else
				return false;
		}

		if (simulate)
			return true;

		activeFuel = FuelType.NORMAL;
		remainingBurnTime = newBurnTime;

		if (level.isClientSide) {
			spawnParticleBurst(true);
			return true;
		}

		playSound();
		updateBlockState();
		return true;
	}

	/** A Creative Blaze Cake toggles between idle and chilling - the only two working states there are. */
	@Override
	protected void applyCreativeFuel() {
		activeFuel = FuelType.NONE;
		remainingBurnTime = 0;
		isCreative = true;

		HeatLevel next = getHeatLevelFromBlock() == CKHeatLevels.CHILLED ? HeatLevel.SMOULDERING : CKHeatLevels.CHILLED;

		if (level.isClientSide) {
			spawnParticleBurst(true);
			return;
		}

		playSound();
		setBlockHeat(next);
	}

	/**
	 * Create's {@code spawnParticles} with snowflakes where a burner puts out large smoke. The flame on
	 * top is still Create's: blue soul fire while chilling, as for a Seething burner.
	 */
	@Override
	protected void spawnParticles(HeatLevel heatLevel, double burstMult) {
		if (level == null)
			return;
		if (heatLevel == HeatLevel.NONE)
			return;
		heatLevel = StrayChillerRenderer.visualLevel(heatLevel);

		RandomSource r = level.getRandom();

		Vec3 c = VecHelper.getCenterOf(worldPosition);
		Vec3 v = c.add(VecHelper.offsetRandomly(Vec3.ZERO, r, .125f)
			.multiply(1, 0, 1));

		if (r.nextInt(4) != 0)
			return;

		boolean empty = level.getBlockState(worldPosition.above())
			.getCollisionShape(level, worldPosition.above())
			.isEmpty();

		if (empty || r.nextInt(8) == 0)
			level.addParticle(ParticleTypes.SNOWFLAKE, v.x, v.y, v.z, 0, 0, 0);

		double yMotion = empty ? .0625f : r.nextDouble() * .0125f;
		Vec3 v2 = c.add(VecHelper.offsetRandomly(Vec3.ZERO, r, .5f)
			.multiply(1, .25f, 1)
			.normalize()
			.scale((empty ? .25f : .5) + r.nextDouble() * .125f))
			.add(0, .5, 0);

		if (heatLevel.isAtLeast(HeatLevel.SEETHING)) {
			level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, v2.x, v2.y, v2.z, 0, yMotion, 0);
		} else if (heatLevel.isAtLeast(HeatLevel.FADING)) {
			level.addParticle(ParticleTypes.FLAME, v2.x, v2.y, v2.z, 0, yMotion, 0);
		}
	}

	/** The head's heading for the renderer - the field itself is protected. */
	public float getHeadAngle(float partialTicks) {
		return headAngle.getValue(partialTicks);
	}

	/** Heated like a Blaze Burner, so a boiler counts it the way it counts one. */
	public static void registerHeatSource(Block block) {
		BoilerHeater.REGISTRY.register(block, BoilerHeater.BLAZE_BURNER);
	}

	/** {@code BlazeBurnerBlockEntity.tickAnimation}, with Chilled as the working state. */
	@OnlyIn(Dist.CLIENT)
	private void animateHead() {
		boolean active = getHeatLevelFromBlock() == CKHeatLevels.CHILLED && isValidBlockAbove();

		if (!active) {
			float target = 0;
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null && !player.isInvisible()) {
				double x;
				double z;
				if (isVirtual()) {
					x = -4;
					z = -10;
				} else {
					x = player.getX();
					z = player.getZ();
				}
				double dx = x - (getBlockPos().getX() + 0.5);
				double dz = z - (getBlockPos().getZ() + 0.5);
				target = AngleHelper.deg(-Mth.atan2(dz, dx)) - 90;
			}
			target = headAngle.getValue() + AngleHelper.getShortestAngleDiff(headAngle.getValue(), target);
			headAngle.chase(target, .25f, Chaser.exp(5));
			headAngle.tickChaser();
		} else {
			headAngle.chase((AngleHelper.horizontalAngle(getBlockState().getOptionalValue(BlazeBurnerBlock.FACING)
				.orElse(Direction.SOUTH)) + 180) % 360, .125f, Chaser.EXP);
			headAngle.tickChaser();
		}

		headAnimation.chase(active ? 1 : 0, .25f, Chaser.exp(.25f));
		headAnimation.tickChaser();
	}
}
