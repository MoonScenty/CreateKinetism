package me.moonscenty.createkinetism.content.oil;

import java.util.function.Consumer;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.ShaftVisual;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;

import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;

/**
 * The valve's shaft and handwheel under Flywheel.
 *
 * <p>{@link GasValveRenderer} draws the same two things, but it almost never runs:
 * {@code KineticBlockEntityRenderer.renderSafe} returns immediately whenever Flywheel is doing the
 * drawing, which by default it is. Without a visual registered alongside it a kinetic block simply
 * has no moving parts at all - which is what left the valve with no shaft and no pointer.</p>
 *
 * <p>Create's own {@code FluidValveVisual}, on our partial model. The shaft comes from
 * {@link ShaftVisual}; everything here is the pointer's quarter turn.</p>
 */
public class GasValveVisual extends ShaftVisual<GasValveBlockEntity> implements SimpleDynamicVisual {

	private final TransformedInstance pointer;
	private final float xRot;
	private final float yRot;
	private final int pointerRotationOffset;

	private boolean settled;

	public GasValveVisual(VisualizationContext context, GasValveBlockEntity blockEntity, float partialTick) {
		super(context, blockEntity, partialTick);

		Direction facing = blockState.getValue(com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock.FACING);
		yRot = AngleHelper.horizontalAngle(facing);
		xRot = facing == Direction.UP ? 0 : facing == Direction.DOWN ? 180 : 90;

		Axis pipeAxis = com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock.getPipeAxis(blockState);
		Axis shaftAxis = KineticBlockEntityRenderer.getRotationAxisOf(blockEntity);
		pointerRotationOffset = pipeAxis.isHorizontal() && shaftAxis == Axis.X || pipeAxis.isVertical() ? 90 : 0;

		pointer = instancerProvider()
			.instancer(InstanceTypes.TRANSFORMED, Models.partial(CKPartialModels.GAS_VALVE_POINTER))
			.createInstance();
		transformPointer(partialTick);
	}

	@Override
	public void beginFrame(DynamicVisual.Context ctx) {
		if (blockEntity.pointer.settled() && settled)
			return;
		transformPointer(ctx.partialTick());
	}

	private void transformPointer(float partialTick) {
		float value = blockEntity.pointer.getValue(partialTick);
		settled = (value == 0 || value == 1) && blockEntity.pointer.settled();

		pointer.setIdentityTransform()
			.translate(getVisualPosition())
			.center()
			.rotateYDegrees(yRot)
			.rotateXDegrees(xRot)
			.rotateYDegrees(pointerRotationOffset + Mth.lerp(value, 0, -90))
			.uncenter()
			.setChanged();
	}

	@Override
	public void updateLight(float partialTick) {
		super.updateLight(partialTick);
		relight(pointer);
	}

	@Override
	protected void _delete() {
		super._delete();
		pointer.delete();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		super.collectCrumblingInstances(consumer);
		consumer.accept(pointer);
	}
}
