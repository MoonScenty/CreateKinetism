package me.moonscenty.createkinetism.content.chamber;

import java.util.function.Consumer;

import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.ShaftVisual;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.math.AngleHelper;

import org.joml.Quaternionf;

/**
 * The enricher's shaft and head under Flywheel, mirroring Create's {@code PressVisual}.
 *
 * <p>This is the half that actually draws in a normal game. {@code KineticBlockEntityRenderer}
 * stands down as soon as Flywheel is available - which it is by default - so a kinetic block with no
 * visual registered shows no moving parts at all, however correct its renderer is.</p>
 *
 * <p>{@link ShaftVisual} supplies the rotating shaft on the block's axis. The head is added on top,
 * oriented by the block's horizontal facing and driven down each frame by the pressing behaviour -
 * the same source the renderer reads, so the two paths agree frame for frame.</p>
 */
public class MechanicalEnricherVisual extends ShaftVisual<MechanicalEnricherBlockEntity>
	implements SimpleDynamicVisual {

	private final OrientedInstance head;

	public MechanicalEnricherVisual(VisualizationContext context, MechanicalEnricherBlockEntity blockEntity,
		float partialTick) {
		super(context, blockEntity, partialTick);

		head = instancerProvider()
			.instancer(InstanceTypes.ORIENTED, Models.partial(CKPartialModels.MECHANICAL_ENRICHER_HEAD))
			.createInstance();

		head.rotation(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(
			blockState.getValue(MechanicalEnricherBlock.HORIZONTAL_FACING))));

		moveHead(partialTick);
	}

	@Override
	public void beginFrame(DynamicVisual.Context ctx) {
		moveHead(ctx.partialTick());
	}

	private void moveHead(float partialTick) {
		EnrichingBehaviour pressing = blockEntity.getPressingBehaviour();
		head.position(getVisualPosition())
			.translatePosition(0, -pressing.getRenderedHeadOffset(partialTick) * pressing.mode.headOffset, 0)
			.setChanged();
	}

	@Override
	public void updateLight(float partialTick) {
		super.updateLight(partialTick);
		relight(head);
	}

	@Override
	protected void _delete() {
		super._delete();
		head.delete();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		super.collectCrumblingInstances(consumer);
		consumer.accept(head);
	}
}
