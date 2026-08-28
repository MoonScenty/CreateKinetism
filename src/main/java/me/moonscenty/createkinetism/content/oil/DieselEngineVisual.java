package me.moonscenty.createkinetism.content.oil;

import java.util.Objects;
import java.util.function.Consumer;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;

import me.moonscenty.createkinetism.registry.CKPartialModels;

import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>The Flywheel counterpart to {@link DieselEngineRenderer}. Without it the engine shows no
 * piston at all on a default install, because the renderer stands down as soon as Flywheel takes
 * over.</p>
 */
public class DieselEngineVisual extends AbstractBlockEntityVisual<DieselEngineBlockEntity>
	implements SimpleDynamicVisual {

	protected final TransformedInstance piston;
	protected final TransformedInstance linkage;
	protected final TransformedInstance connector;

	private Float lastAngle = Float.NaN;
	private Direction.Axis lastAxis = null;

	public DieselEngineVisual(VisualizationContext context, DieselEngineBlockEntity blockEntity, float partialTick) {
		super(context, blockEntity, partialTick);

		piston = instancerProvider()
			.instancer(InstanceTypes.TRANSFORMED, Models.partial(CKPartialModels.DIESEL_ENGINE_PISTON))
			.createInstance();
		linkage = instancerProvider()
			.instancer(InstanceTypes.TRANSFORMED, Models.partial(CKPartialModels.DIESEL_ENGINE_LINKAGE))
			.createInstance();
		connector = instancerProvider()
			.instancer(InstanceTypes.TRANSFORMED, Models.partial(CKPartialModels.DIESEL_ENGINE_CONNECTOR))
			.createInstance();

		animate();
	}

	@Override
	public void beginFrame(Context ctx) {
		animate();
	}

	private void animate() {
		Float angle = blockEntity.getTargetAngle();
		Direction.Axis axis = Direction.Axis.Y;

		PoweredShaftBlockEntity shaft = blockEntity.getShaft();
		if (shaft != null)
			axis = KineticBlockEntityRenderer.getRotationAxisOf(shaft);

		if (Objects.equals(angle, lastAngle) && lastAxis == axis)
			return;

		lastAngle = angle;
		lastAxis = axis;

		if (angle == null) {
			piston.setVisible(false);
			linkage.setVisible(false);
			connector.setVisible(false);
			return;
		}

		piston.setVisible(true);
		linkage.setVisible(true);
		connector.setVisible(true);

		Direction facing = SteamEngineBlock.getFacing(blockState);
		Direction.Axis facingAxis = facing.getAxis();

		boolean roll90 =
			facingAxis.isHorizontal() && axis == Direction.Axis.Y || facingAxis.isVertical() && axis == Direction.Axis.Z;

		// Crank-slider, identical to the renderer so the two paths agree frame for frame.
		float stroke = (6 / 16f) * Mth.sin(angle)
			- Mth.sqrt(Mth.square(14 / 16f) - Mth.square(6 / 16f) * Mth.square(Mth.cos(angle)));
		float distance = Mth.sqrt(Mth.square(stroke - 6 / 16f * Mth.sin(angle)));
		float rodAngle = (float) Math.acos(distance / (14 / 16f)) * (Mth.cos(angle) >= 0 ? 1f : -1f);

		transformed(piston, facing, roll90)
			.translate(0, stroke + 20 / 16f, 0)
			.setChanged();

		transformed(linkage, facing, roll90)
			.center()
			.translate(0, 1, 0)
			.uncenter()
			.translate(0, stroke + 20 / 16f, 0)
			.translate(0, 4 / 16f, 8 / 16f)
			.rotateX(rodAngle)
			.translate(0, -4 / 16f, -8 / 16f)
			.setChanged();

		transformed(connector, facing, roll90)
			.translate(0, 2, 0)
			.center()
			.rotateX(-(angle + Mth.HALF_PI))
			.uncenter()
			.setChanged();
	}

	/** Orients a part for whichever face the engine is bolted to. */
	protected TransformedInstance transformed(TransformedInstance instance, Direction facing, boolean roll90) {
		return instance.setIdentityTransform()
			.translate(getVisualPosition())
			.center()
			.rotateYDegrees(AngleHelper.horizontalAngle(facing))
			.rotateXDegrees(AngleHelper.verticalAngle(facing) + 90)
			.rotateYDegrees(roll90 ? -90 : 0)
			.uncenter();
	}

	@Override
	public void updateLight(float partialTick) {
		relight(piston, linkage, connector);
	}

	@Override
	protected void _delete() {
		piston.delete();
		linkage.delete();
		connector.delete();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		consumer.accept(piston);
		consumer.accept(linkage);
		consumer.accept(connector);
	}
}
