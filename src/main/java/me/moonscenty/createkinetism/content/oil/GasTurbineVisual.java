package me.moonscenty.createkinetism.content.oil;

import java.util.function.Consumer;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.Direction;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>The Flywheel counterpart to {@link GasTurbineRenderer}. Without it the turbine shows no fan at
 * all on a default install, because the renderer stands down as soon as Flywheel takes over.</p>
 *
 * <p>Petrochem's turbine is an electrical generator and drives the fan off its own lerped speed;
 * ours is a rotational generator, so the angle comes straight off the network speed - the same
 * formula the renderer uses, so the two paths agree frame for frame.</p>
 *
 * <p><b>The facing passed to {@code rotateToFace} here is the opposite of the one
 * {@link GasTurbineRenderer} passes to {@code partialFacing}, and that is not a mistake.</b>
 * Flywheel's {@code Rotate.rotateToFace(Direction)} rotates from NORTH ({@code NORTH -> self()}),
 * while Create's {@code RotatingInstance.rotateToFace(from, to)} - the one its own FanVisual uses -
 * rotates from SOUTH. The two conventions are 180 degrees apart, so reaching the layout Create's
 * encased fan has means naming the front here and the back there. Both end up with the model's -Z
 * pointing out of the intake.</p>
 */
public class GasTurbineVisual extends AbstractBlockEntityVisual<FuelEngineBlockEntity>
	implements SimpleDynamicVisual {

	protected final TransformedInstance shaft;
	protected final TransformedInstance fan1, fan2, fan3;
	protected final Matrix4f baseTransform = new Matrix4f();

	public GasTurbineVisual(VisualizationContext ctx, FuelEngineBlockEntity blockEntity, float partialTick) {
		super(ctx, blockEntity, partialTick);

		shaft = instancerProvider()
			.instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.SHAFT_HALF))
			.createInstance();
		fan1 = instancerProvider()
			.instancer(InstanceTypes.TRANSFORMED, Models.partial(CKPartialModels.TURBINE_PROPELLER))
			.createInstance();
		fan2 = instancerProvider()
			.instancer(InstanceTypes.TRANSFORMED, Models.partial(CKPartialModels.TURBINE_PROPELLER))
			.createInstance();
		fan3 = instancerProvider()
			.instancer(InstanceTypes.TRANSFORMED, Models.partial(CKPartialModels.TURBINE_PROPELLER))
			.createInstance();

		Direction facing = blockEntity.getBlockState()
			.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);

		// The blades are a single quad with only a north face, so an inverted rotation does not show
		// a back-to-front fan - it culls the fan away entirely.
		fan1.translate(getVisualPosition())
			.center()
			.rotateToFace(facing);

		baseTransform.set(fan1.pose);

		animate(0);
	}

	public void animate(float angle) {
		Direction facing = blockEntity.getBlockState()
			.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);

		shaft.setIdentityTransform()
			.translate(getVisualPosition())
			.center()
			.rotateToFace(facing)
			.rotateZDegrees(angle)
			.uncenter()
			.setChanged();

		// Three stages, one counter-rotating, which is what gives a spinning turbine its layered blur.
		fan1.setTransform(baseTransform)
			.rotateZDegrees(angle)
			.uncenter()
			.setChanged();

		fan2.setTransform(baseTransform)
			.rotateZDegrees(-angle + 30)
			.uncenter()
			.translateZ(1 / 16f)
			.setChanged();

		fan3.setTransform(baseTransform)
			.rotateZDegrees(angle + 60)
			.uncenter()
			.translateZ(2 / 16f)
			.setChanged();
	}

	@Override
	public void beginFrame(Context ctx) {
		animate(AnimationTickHolder.getRenderTime(blockEntity.getLevel()) * blockEntity.getSpeed() * 3 / 10f % 360);
	}

	@Override
	public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
		consumer.accept(shaft);
		consumer.accept(fan1);
		consumer.accept(fan2);
		consumer.accept(fan3);
	}

	@Override
	public void updateLight(float partialTick) {
		relight(shaft, fan1, fan2, fan3);
	}

	@Override
	protected void _delete() {
		shaft.delete();
		fan1.delete();
		fan2.delete();
		fan3.delete();
	}
}
