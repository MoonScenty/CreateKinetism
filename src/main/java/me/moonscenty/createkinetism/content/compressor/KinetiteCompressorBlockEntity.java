package me.moonscenty.createkinetism.content.compressor;

import java.util.List;
import java.util.Optional;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import me.moonscenty.createkinetism.content.recipe.KinetiteCompressingRecipe;
import me.moonscenty.createkinetism.registry.CKRecipeTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

import org.jetbrains.annotations.Nullable;

/**
 * Holds the two halves of a compressing recipe and drives the ram.
 *
 * <p>One cycle is out and back: the ram leaves its rest position, travels ten pixels into the
 * spinning head, and returns. The output appears at the moment of contact, but the machine will not
 * look at the next recipe until the ram is home again - a press that is still on its way back is not
 * ready, and letting it start over mid-travel would make the animation lie about what happened.</p>
 *
 * <p>Progress runs on both sides rather than being synced every tick, the way Create's own press
 * animates: speed is already synced, so both sides advance the same curve from the same start.</p>
 *
 * <p>Two shafts drive this machine, on separate networks. This half's own speed spins the head that
 * holds the target; the cradle behind it drives the ram, and so the ram's travel is timed off
 * <em>its</em> speed - see {@link #getRamSpeed}. Both have to be turning for anything to happen,
 * which is the point of building a machine that needs two drives.</p>
 */
public class KinetiteCompressorBlockEntity extends KineticBlockEntity {

	// Ordered so each half of the machine owns a contiguous run of slots: the front holds what goes
	// in and what comes out, the cradle behind holds the Kinetite. That is what lets a hopper on the
	// front and a hopper on the back feed different things without a filter.
	public static final int TARGET_SLOT = 0;
	public static final int OUTPUT_SLOT = 1;
	public static final int KINETITE_SLOT = 2;

	/** How far the ram travels, in Blockbench pixels - the figure the model was drawn to. */
	public static final float TRAVEL_PIXELS = 10;

	/** Ticks for a whole out-and-back at 64 RPM. Faster shafts finish sooner, down to a floor. */
	private static final float CYCLE_TICKS_AT_64 = 60;

	public final ItemStackHandler inventory = new ItemStackHandler(3) {
		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return slot != OUTPUT_SLOT;
		}

		@Override
		protected void onContentsChanged(int slot) {
			setChanged();
			sendData();
		}
	};

	/** 0 at rest, 1 at contact, 2 home again. Idle sits at 0. */
	public float progress;
	public float prevProgress;
	private boolean running;
	/** So the output lands once per cycle rather than once per tick at the peak. */
	private boolean pressed;

	public KinetiteCompressorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event,
		BlockEntityType<KinetiteCompressorBlockEntity> type) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> be.frontHandler());
	}

	/** What the front half offers: put the target in, take the result out. */
	public IItemHandler frontHandler() {
		return new RangedWrapper(inventory, TARGET_SLOT, OUTPUT_SLOT + 1);
	}

	/** What the cradle offers on this machine's behalf: the Kinetite holder, and nothing else. */
	public IItemHandler kinetiteHandler() {
		return new RangedWrapper(inventory, KINETITE_SLOT, KINETITE_SLOT + 1);
	}

	/** Where the ram sits this frame, in block space. */
	public float getRamOffset(float partialTicks) {
		float p = Mth.lerp(partialTicks, prevProgress, progress);
		float reach = p <= 1 ? p : 2 - p;
		return reach * TRAVEL_PIXELS / 16f;
	}

	/**
	 * How fast the ram is being driven, which is the cradle's business rather than ours.
	 *
	 * @return zero when the cradle is missing or standing still, in which case nothing presses
	 */
	public float getRamSpeed() {
		if (level == null)
			return 0;
		BlockPos pos = KinetiteCompressorBlock.cradlePos(worldPosition, getBlockState());
		return level.getBlockEntity(pos) instanceof KinetiteCompressorCradleBlockEntity cradle
			? cradle.getSpeed()
			: 0;
	}

	@Override
	public void tick() {
		super.tick();
		prevProgress = progress;

		// The head must be spinning and the ram must have something driving it.
		float rpm = Math.abs(getRamSpeed());
		if (rpm == 0 || getSpeed() == 0)
			return;

		if (!running) {
			if (level != null && !level.isClientSide && canStart())
				start();
			return;
		}

		progress += 2 / (CYCLE_TICKS_AT_64 * Mth.clamp(64 / rpm, .25f, 4f));

		// Contact. The output is made here, once.
		if (progress >= 1 && !pressed) {
			pressed = true;
			if (level != null && !level.isClientSide)
				press();
		}

		// Home. Only now is the machine free to look at the next recipe.
		if (progress >= 2) {
			progress = 0;
			prevProgress = 0;
			running = false;
			pressed = false;
			if (level != null && !level.isClientSide)
				sendData();
		}
	}

	private boolean canStart() {
		return findRecipe().map(this::outputFits)
			.orElse(false);
	}

	private void start() {
		running = true;
		pressed = false;
		progress = 0;
		prevProgress = 0;
		sendData();
	}

	/** Take the two inputs, leave the result. Re-checked here because a cycle takes real time. */
	private void press() {
		Optional<RecipeHolder<KinetiteCompressingRecipe>> found = findRecipe();
		if (found.isEmpty())
			return;
		KinetiteCompressingRecipe recipe = found.get()
			.value();
		if (!outputFits(found.get()))
			return;

		int[] consumed = recipe.resolve(inputView());
		if (consumed == null)
			return;
		// inputView() lists the target first and the Kinetite second, so map those two back onto the
		// real slots rather than assuming they sit next to each other.
		int[] realSlot = { TARGET_SLOT, KINETITE_SLOT };
		for (int i = 0; i < consumed.length && i < realSlot.length; i++)
			inventory.extractItem(realSlot[i], consumed[i], false);

		ItemStack result = recipe.getResultItem(level.registryAccess())
			.copy();
		ItemStack held = inventory.getStackInSlot(OUTPUT_SLOT);
		if (held.isEmpty())
			inventory.setStackInSlot(OUTPUT_SLOT, result);
		else
			held.grow(result.getCount());
		setChanged();
		sendData();
	}

	private boolean outputFits(RecipeHolder<KinetiteCompressingRecipe> holder) {
		ItemStack result = holder.value()
			.getResultItem(level.registryAccess());
		ItemStack held = inventory.getStackInSlot(OUTPUT_SLOT);
		if (held.isEmpty())
			return true;
		return ItemStack.isSameItemSameComponents(held, result)
			&& held.getCount() + result.getCount() <= held.getMaxStackSize();
	}

	/** Only the two input holders; the finished item must not be read back as an ingredient. */
	private RecipeInput inputView() {
		ItemStackHandler inputs = new ItemStackHandler(2);
		inputs.setStackInSlot(0, inventory.getStackInSlot(TARGET_SLOT));
		inputs.setStackInSlot(1, inventory.getStackInSlot(KINETITE_SLOT));
		return new RecipeWrapper(inputs);
	}

	private Optional<RecipeHolder<KinetiteCompressingRecipe>> findRecipe() {
		if (level == null)
			return Optional.empty();
		if (inventory.getStackInSlot(TARGET_SLOT)
			.isEmpty()
			|| inventory.getStackInSlot(KINETITE_SLOT)
				.isEmpty())
			return Optional.empty();
		return level.getRecipeManager()
			.getRecipeFor(CKRecipeTypes.KINETITE_COMPRESSING.<RecipeInput, KinetiteCompressingRecipe>getType(), inputView(), level);
	}

	/** What the spinning head is holding up, and what the ram is bringing down onto it. */
	public ItemStack getTargetItem() {
		return inventory.getStackInSlot(TARGET_SLOT);
	}

	public ItemStack getKinetiteItem() {
		return inventory.getStackInSlot(KINETITE_SLOT);
	}

	@Override
	protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.put("Inventory", inventory.serializeNBT(registries));
		compound.putBoolean("Running", running);
		compound.putBoolean("Pressed", pressed);
		compound.putFloat("Progress", progress);
		super.write(compound, registries, clientPacket);
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		if (compound.contains("Inventory"))
			inventory.deserializeNBT(registries, compound.getCompound("Inventory"));
		running = compound.getBoolean("Running");
		pressed = compound.getBoolean("Pressed");
		progress = compound.getFloat("Progress");
		prevProgress = progress;
	}
}
