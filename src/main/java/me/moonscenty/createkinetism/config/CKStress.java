package me.moonscenty.createkinetism.config;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import me.moonscenty.createkinetism.CreateKinetism;

import net.createmod.catnip.config.ConfigBase;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import net.neoforged.neoforge.common.ModConfigSpec;

import org.jetbrains.annotations.Nullable;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md, which in turn mirrors Create's
 * own {@code CStress}. Create refuses to let other mods write into its stress config, so an addon
 * that wants configurable kinetic stats has to keep its own.
 *
 * <p>Every impact and capacity in this mod is registered here rather than as a constant, so a pack
 * can rebalance any of it. The RPM ceiling on the engines is here too, since that is what decides
 * how much capacity you can trade speed for.</p>
 *
 * <p>Values are keyed by {@link ResourceLocation} rather than by {@code Block} because the config
 * spec is built before block registration has run.</p>
 */
public class CKStress extends ConfigBase {

	/** Bump to discard previously configured values. */
	private static final int VERSION = 1;

	private static final Object2DoubleMap<ResourceLocation> DEFAULT_IMPACTS = new Object2DoubleOpenHashMap<>();
	private static final Object2DoubleMap<ResourceLocation> DEFAULT_CAPACITIES = new Object2DoubleOpenHashMap<>();
	private static final Object2IntMap<ResourceLocation> DEFAULT_RPM = new Object2IntOpenHashMap<>();

	protected final Map<ResourceLocation, ModConfigSpec.ConfigValue<Double>> impacts = new HashMap<>();
	protected final Map<ResourceLocation, ModConfigSpec.ConfigValue<Double>> capacities = new HashMap<>();
	protected final Map<ResourceLocation, ModConfigSpec.ConfigValue<Integer>> rpm = new HashMap<>();

	@Override
	public void registerAll(ModConfigSpec.Builder builder) {
		builder.comment(".", Comments.su, Comments.impact)
			.push("impact");
		DEFAULT_IMPACTS.forEach((id, value) -> this.impacts.put(id, builder.define(id.getPath(), value)));
		builder.pop();

		builder.comment(".", Comments.su, Comments.capacity)
			.push("capacity");
		DEFAULT_CAPACITIES.forEach((id, value) -> this.capacities.put(id, builder.define(id.getPath(), value)));
		builder.pop();

		builder.comment(".", Comments.rpm)
			.push("rpm");
		DEFAULT_RPM.forEach((id, value) -> this.rpm.put(id, builder.defineInRange(id.getPath(), (int) value, 1, 4096)));
		builder.pop();
	}

	@Override
	public String getName() {
		return "stressValues.v" + VERSION;
	}

	@Nullable
	public DoubleSupplier getImpact(Block block) {
		ModConfigSpec.ConfigValue<Double> value = this.impacts.get(RegisteredObjectsHelper.getKeyOrThrow(block));
		return value == null ? null : value::get;
	}

	@Nullable
	public DoubleSupplier getCapacity(Block block) {
		ModConfigSpec.ConfigValue<Double> value = this.capacities.get(RegisteredObjectsHelper.getKeyOrThrow(block));
		return value == null ? null : value::get;
	}

	/** The highest RPM an engine's scroll value will go to, in either direction. */
	public static int getMaxRpm(Block block) {
		ResourceLocation id = RegisteredObjectsHelper.getKeyOrThrow(block);
		CKStress stress = CKConfigs.stress();
		if (stress != null) {
			ModConfigSpec.ConfigValue<Integer> value = stress.rpm.get(id);
			if (value != null)
				return value.get();
		}
		return DEFAULT_RPM.getOrDefault(id, 256);
	}

	/** The compile-time default, for the goggle tooltip - Create's RPM registry takes a plain value. */
	public static int getDefaultRpm(ResourceLocation id) {
		return DEFAULT_RPM.getOrDefault(id, 256);
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> setNoImpact() {
		return setImpact(0);
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> setImpact(double value) {
		return builder -> {
			DEFAULT_IMPACTS.put(CreateKinetism.asResource(builder.getName()), value);
			return builder;
		};
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> setCapacity(double value) {
		return builder -> {
			DEFAULT_CAPACITIES.put(CreateKinetism.asResource(builder.getName()), value);
			return builder;
		};
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> setMaxRpm(int value) {
		return builder -> {
			DEFAULT_RPM.put(CreateKinetism.asResource(builder.getName()), value);
			return builder;
		};
	}

	private static class Comments {
		static String su = "[in Stress Units]";
		static String impact =
			"Configure the individual stress impact of mechanical blocks. Note that this cost is doubled for every speed increase it receives.";
		static String capacity = "Configure how much stress a source can accommodate for.";
		static String rpm =
			"Configure the highest RPM an engine can be scrolled to. Engines trade speed for capacity, so raising this lets an engine run faster but supply proportionally less stress.";

		private Comments() {
		}
	}
}
