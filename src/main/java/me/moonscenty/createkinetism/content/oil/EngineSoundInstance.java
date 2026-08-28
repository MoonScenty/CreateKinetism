package me.moonscenty.createkinetism.content.oil;

import java.lang.ref.WeakReference;

import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Ported from Petrochem (MIT, hadron13) - see LICENSE-THIRD-PARTY.md.
 *
 * <p>A looping engine note that follows load and RPM. Pitch and volume are chased rather than set,
 * so an engine spinning up or being throttled slides instead of jumping, and shutting one down fades
 * it out before the loop stops.</p>
 */
public class EngineSoundInstance extends AbstractTickableSoundInstance {

	private final WeakReference<BlockEntity> blockEntity;
	private final LerpedFloat lerpedPitch = LerpedFloat.linear();
	private final LerpedFloat lerpedVolume = LerpedFloat.linear();

	public EngineSoundInstance(SoundEvent soundEvent, BlockEntity be) {
		super(soundEvent, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
		this.looping = true;
		this.attenuation = Attenuation.LINEAR;
		this.volume = 0.2f;
		this.pitch = 1.0f;

		lerpedPitch.chase(1.0, 1 / 20f, LerpedFloat.Chaser.EXP);
		lerpedVolume.chase(0.2, 1 / 5f, LerpedFloat.Chaser.EXP);

		this.blockEntity = new WeakReference<>(be);
		BlockPos pos = be.getBlockPos();
		this.x = pos.getX();
		this.y = pos.getY();
		this.z = pos.getZ();
	}

	public void setVolume(float volume) {
		lerpedVolume.updateChaseTarget(volume);
	}

	public void setPitch(float pitch) {
		lerpedPitch.updateChaseTarget(pitch);
	}

	/** Fade out and stop once silent. */
	public void cease() {
		lerpedVolume.updateChaseTarget(0);
	}

	@Override
	public void tick() {
		ClientLevel level = Minecraft.getInstance().level;
		BlockEntity be = blockEntity.get();
		if (level == null || be == null || be.isRemoved())
			cease();

		lerpedPitch.tickChaser();
		lerpedVolume.tickChaser();
		pitch = lerpedPitch.getValue();
		volume = lerpedVolume.getValue();

		if (volume == 0)
			stop();
	}
}
