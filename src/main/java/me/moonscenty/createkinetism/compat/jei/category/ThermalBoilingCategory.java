package me.moonscenty.createkinetism.compat.jei.category;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedBlazeBurner;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.CreateLang;

import me.moonscenty.createkinetism.content.boiler.ThermalBoilingRecipe;
import me.moonscenty.createkinetism.registry.CKPartialModels;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Thermal Boiling 레시피. Boiler Controller가 만드는 모양(1x1, 3층 스택 + 밑에 열원)을 그린다.
 * 실제 블록스테이트 대신 {@code jei_top/middle/bottom} 전용 모델을 쓰는데, 이유는 CT의 "connected" 텍스쳐가
 * 실제 월드에서 이웃 블록을 확인해야 적용되고 JEI의 가짜 렌더 컨텍스트에는 그게 없기 때문. 버너는 Create가
 * {@code MixingCategory}에서 가열된 basin에 쓰는 것과 똑같은 {@code AnimatedBlazeBurner}를 그대로 사용.
 */
@ParametersAreNonnullByDefault
public class ThermalBoilingCategory extends BasinRecipeCategory<ThermalBoilingRecipe> {

	/** {@link AnimatedBlazeBurner}와 스케일을 맞춰야 따로 그리는 두 장면의 블록 크기가 어색하지 않다. */
	private static final int SCALE = 23;

	private final AnimatedBlazeBurner heater = new AnimatedBlazeBurner();

	public ThermalBoilingCategory(Info<ThermalBoilingRecipe> info) {
		super(info);
	}

	@Override
	protected int machineAnchor() {
		return 72;
	}

	/** 비워둠 - 어떤 레시피인지 알아야 그릴 수 있어서 실제로는 {@link #draw}에서 그림. */
	@Override
	protected void drawMachine(GuiGraphics graphics, int centerX, int anchorY) {
	}

	/**
	 * 부모의 화살표/그림자 그리기를 {@code super} 호출 대신 직접 다시 구현함 - 그림자 위치도 장면에 맞춰
	 * 옮겨야 하는데 부모 클래스는 그걸 고정된 위치에 그려버림.
	 */
	@Override
	protected void draw(ThermalBoilingRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics graphics, double mouseX,
		double mouseY) {
		int vRows = (1 + recipe.getFluidResults()
			.size()
			+ recipe.getRollableResults()
				.size()) / 2;
		if (vRows <= 2)
			AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, -19 * (vRows - 1) + 32);
		AllGuiTextures.JEI_SHADOW.render(graphics, 81, 78);

		HeatCondition heat = recipe.getRequiredHeat();
		int centerX = getBackground().getWidth() / 2 + 3;
		int anchorY = machineAnchor();

		drawTanks(graphics, centerX, anchorY);
		// AnimatedBlazeBurner는 자기 앵커 기준 local Y=+1.65에 버너 블록을 그림(translate/rotate는
		// 아래 drawTanks와 동일한 방식). Create의 MixingCategory는 히터 앵커를 기계 앵커보다 21px 아래로
		// 잡아서 서로 맞닿게 함. 우리 탱크 바닥은 local Y=-0.5라 mixer의 +1.65보다 위쪽이므로, 그 차이인
		// 2.15 local unit(이 스케일에서 약 48px)만큼 앵커를 더 위로 당겨줘야 같은 결과가 나옴.
		heater.withHeat(heat.visualizeAsBlazeBurner())
			.draw(graphics, centerX, anchorY - 27);

		graphics.drawString(Minecraft.getInstance().font, CreateLang.translateDirect(heat.getTranslationKey()), 4, 92,
			heat.getColor(), false);
	}

	private void drawTanks(GuiGraphics graphics, int xOffset, int yOffset) {
		PoseStack ms = graphics.pose();
		ms.pushPose();
		ms.translate(xOffset, yOffset, 200);
		ms.mulPose(Axis.XP.rotationDegrees(-15.5f));
		ms.mulPose(Axis.YP.rotationDegrees(22.5f));

		// 실제 블록스테이트가 아니라 JEI 전용 모델 - 실제 블록의 connected 텍스쳐는 이웃을 확인할 실제
		// 월드가 있어야 적용되는데 JEI의 가짜 렌더에는 그게 없음.
		AnimatedKinetics.defaultBlockElement(CKPartialModels.THERMAL_BOILER_TANK_JEI_TOP)
			.atLocal(0, -2.5, 0)
			.scale(SCALE)
			.render(graphics);
		AnimatedKinetics.defaultBlockElement(CKPartialModels.THERMAL_BOILER_TANK_JEI_MIDDLE)
			.atLocal(0, -1.5, 0)
			.scale(SCALE)
			.render(graphics);
		AnimatedKinetics.defaultBlockElement(CKPartialModels.THERMAL_BOILER_TANK_JEI_BOTTOM)
			.atLocal(0, -0.5, 0)
			.scale(SCALE)
			.render(graphics);

		// The feed floor's own gauge, same partial the real ThermalBoilerTankRenderer mounts on every
		// side. The gauge partial's own geometry already sits flush on a block's +X face, so it needs
		// the SAME atLocal corner as the tank it's mounted on - rotateBlock() then spins it in place
		// around that block's true centre to land on whichever face should show.
		AnimatedKinetics.defaultBlockElement(CKPartialModels.DISTILLATION_GAUGE)
			.atLocal(0, -0.5, 0)
			.rotateBlock(0, 180, 0)
			.scale(SCALE)
			.render(graphics);
		AnimatedKinetics.defaultBlockElement(CKPartialModels.DISTILLATION_GAUGE)
			.atLocal(0, -0.5, 0)
			.rotateBlock(0, 270, 0)
			.scale(SCALE)
			.render(graphics);

		ms.popPose();
	}
}
