package me.moonscenty.createkinetism.compat.jei.category;

import java.util.List;

import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import me.moonscenty.createkinetism.compat.jei.category.animation.AnimatedKinetiteCompressor;
import me.moonscenty.createkinetism.content.recipe.ChamberRecipe;
import me.moonscenty.createkinetism.foundation.gui.CKGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * A target item and a lump of Kinetite pressed into one.
 *
 * <p>Two input slots rather than the Enricher's one, stacked so they read as the two things the
 * machine holds: the upper is what the spinning head presents, the lower what the ram brings down on
 * it. Which is which is not enforced by the recipe - {@link ChamberRecipe} matches on contents, not
 * on slot - but showing them apart matches what the block looks like.</p>
 */
public class KinetiteCompressingCategory extends CreateRecipeCategory<ChamberRecipe> {

	private final AnimatedKinetiteCompressor compressor = new AnimatedKinetiteCompressor();

	public KinetiteCompressingCategory(Info<ChamberRecipe> info) {
		super(info);
	}

	/** The target comes in from the left, the Kinetite from above, and the result leaves right. */
	private static final int TARGET_X = 6, TARGET_Y = 36;
	private static final int KINETITE_X = 62, KINETITE_Y = 4;
	private static final int OUTPUT_X = 152, OUTPUT_Y = 36;

	// Arrows, in the same order. The two straight ones are ours: Create's is 42 wide, which is longer
	// than either gap here. The one from above is Create's - it already bends the way this one needs
	// to, coming in from the side and turning down into the machine.
	private static final int IN_ARROW_X = 32, IN_ARROW_Y = 40;
	private static final int DOWN_ARROW_X = 83, DOWN_ARROW_Y = 12;
	private static final int OUT_ARROW_X = 118, OUT_ARROW_Y = 40;

	@Override
	protected void setRecipe(IRecipeLayoutBuilder builder, ChamberRecipe recipe, IFocusGroup focuses) {
		List<Ingredient> ingredients = recipe.getIngredients();
		int[][] where = { { TARGET_X, TARGET_Y }, { KINETITE_X, KINETITE_Y } };
		for (int i = 0; i < ingredients.size() && i < where.length; i++)
			builder.addSlot(RecipeIngredientRole.INPUT, where[i][0], where[i][1])
				.setBackground(getRenderedSlot(), -1, -1)
				.addIngredients(ingredients.get(i));

		int i = 0;
		for (ProcessingOutput output : recipe.getRollableResults()) {
			builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X + 19 * i, OUTPUT_Y)
				.setBackground(getRenderedSlot(output), -1, -1)
				.addItemStack(output.getStack())
				.addRichTooltipCallback(addStochasticTooltip(output));
			i++;
		}
	}

	@Override
	protected void draw(ChamberRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX,
		double mouseY) {
		AllGuiTextures.JEI_SHADOW.render(graphics, 61, 55);

		// One arrow per way in or out, rather than a single belt-like run underneath: this machine is
		// fed from two sides, and one long arrow could only ever describe one of them.
		CKGuiTextures.SHORT_RIGHT_ARROW.render(graphics, IN_ARROW_X, IN_ARROW_Y);
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, DOWN_ARROW_X, DOWN_ARROW_Y);
		CKGuiTextures.SHORT_RIGHT_ARROW.render(graphics, OUT_ARROW_X, OUT_ARROW_Y);
		// Measured rather than derived. The anchor is not the machine's visual centre - the drawing
		// lands up and to the right of it, by an amount that scales with the size above - so the anchor
		// sits down and left of the shadow it is meant to stand on, which is centred near (88, 45).
		compressor.draw(graphics, getBackground().getWidth() / 2 - 8, 59);
	}
}
