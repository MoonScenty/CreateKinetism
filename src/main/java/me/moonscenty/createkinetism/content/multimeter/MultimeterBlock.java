package me.moonscenty.createkinetism.content.multimeter;

import com.simibubi.create.content.kinetics.gauge.GaugeBlock;
import com.simibubi.create.content.kinetics.gauge.GaugeBlockEntity;

import me.moonscenty.createkinetism.registry.CKBlockEntityTypes;

import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * A Speedometer and a Stressometer in one housing, with a needle for each.
 *
 * <p>This is the one place in the mod where extending a Create block is plainly right rather than
 * merely convenient: it <em>is</em> a gauge. Placement against a shaft, the odd bracket-shaped hitbox,
 * the coloured particles that puff off a working dial and the comparator output are all
 * {@link GaugeBlock}'s, and none of them would differ if we wrote them out again. What differs is one
 * thing - a second needle - and that lives in {@link MultimeterBlockEntity}.</p>
 *
 * <p>The {@code Type} handed to the super constructor only ever decides which block entity type
 * {@code GaugeBlock} looks up, and that is overridden below, so it carries no meaning here.</p>
 */
public class MultimeterBlock extends GaugeBlock {

	public MultimeterBlock(Properties properties) {
		super(properties, Type.SPEED);
	}

	@Override
	public BlockEntityType<? extends GaugeBlockEntity> getBlockEntityType() {
		return CKBlockEntityTypes.MULTIMETER.get();
	}
}
