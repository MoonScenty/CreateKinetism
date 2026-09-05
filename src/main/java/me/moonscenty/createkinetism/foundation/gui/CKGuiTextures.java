package me.moonscenty.createkinetism.foundation.gui;

import me.moonscenty.createkinetism.CreateKinetism;

import net.createmod.catnip.gui.TextureSheetSegment;
import net.createmod.catnip.gui.element.ScreenElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * This mod's own GUI sprites, in the shape of Create's {@code AllGuiTextures}.
 *
 * <p>There is only one so far, and it exists because Create's arrows are all longer than the gaps in
 * the Kinetite Compressor's panel - that machine puts its slots around the picture rather than in a
 * line, so a 42-wide arrow has nowhere to sit.</p>
 *
 * <p>Each entry is one whole file rather than a region of a sheet. Create packs its sprites into
 * sheets because it has hundreds; adding a file at a time is easier to draw against, and the enum
 * can grow a sheet-based constructor later without disturbing these.</p>
 */
public enum CKGuiTextures implements ScreenElement, TextureSheetSegment {

	SHORT_RIGHT_ARROW("jei/short_right_arrow", 24, 9),
	;

	public final ResourceLocation location;
	private final int width;
	private final int height;
	private final int startX;
	private final int startY;

	CKGuiTextures(String file, int width, int height) {
		this(file, 0, 0, width, height);
	}

	CKGuiTextures(String file, int startX, int startY, int width, int height) {
		this.location = CreateKinetism.asResource("textures/gui/" + file + ".png");
		this.startX = startX;
		this.startY = startY;
		this.width = width;
		this.height = height;
	}

	@Override
	public ResourceLocation getLocation() {
		return location;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void render(GuiGraphics graphics, int x, int y) {
		graphics.blit(location, x, y, startX, startY, width, height, width, height);
	}

	@Override
	public int getStartX() {
		return startX;
	}

	@Override
	public int getStartY() {
		return startY;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}
}
