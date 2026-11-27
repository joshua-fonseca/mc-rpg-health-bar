package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.config.AirPosition;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Mount health with Air Bubbles/Mount HP on Below, once it has more than one row: vanilla's hearts,
 * with the first ten on top and each further row under the last. Vanilla only stacks them upward,
 * so its overlay is hidden while this one draws.
 */
public class MountHealthLayer implements IGuiOverlay {
    /** Where vanilla's mount hearts are in its icon sheet. */
    private static final int HEART_V = 9;
    private static final int CONTAINER_U = 52;
    private static final int FULL_U = 88;
    private static final int HALF_U = 97;
    private static final int HEARTS_PER_ROW = 10;
    private static final int HEART_SPACING = 8;
    private static final int HEART_SIZE = 9;

    /** Whether this layer takes the place of vanilla's mount health. */
    public static boolean replacesVanilla() {
        return ClientConfig.ENABLED.get() && ClientConfig.AIR_POSITION.get() == AirPosition.BELOW
                && HudPositionMover.mountRows(Minecraft.getInstance()) > 1;
    }

    /**
     * Draws {@code hearts} hearts of mount health in rows of ten, each row under the last, filled to
     * {@code health} points, from the first row's right end and top.
     */
    public static void drawHearts(GuiGraphics graphics, int right, int top, int hearts, int health) {
        RenderSystem.enableBlend();
        for (int heart = 0; heart < hearts; heart++) {
            int x = right - heart % HEARTS_PER_ROW * HEART_SPACING - HEART_SIZE;
            int y = top + heart / HEARTS_PER_ROW * HudPositionMover.ROW_HEIGHT;
            graphics.blit(XpBarOverlay.ICONS, x, y, CONTAINER_U, HEART_V, HEART_SIZE, HEART_SIZE);
            if (heart * 2 + 1 < health) {
                graphics.blit(XpBarOverlay.ICONS, x, y, FULL_U, HEART_V, HEART_SIZE, HEART_SIZE);
            } else if (heart * 2 + 1 == health) {
                graphics.blit(XpBarOverlay.ICONS, x, y, HALF_U, HEART_V, HEART_SIZE, HEART_SIZE);
            }
        }
        RenderSystem.disableBlend();
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = gui.getMinecraft();
        // Shown when vanilla's would be, unless the HUD Editor's normal view leaves it out.
        if (minecraft.options.hideGui || !gui.shouldDrawSurvivalElements() || !(minecraft.getCameraEntity() instanceof Player player) || !replacesVanilla()
                || Boolean.FALSE.equals(HudPosition.previewMounted())) {
            return;
        }
        LivingEntity mount = HudPositionMover.mountWithHealth(player);
        if (mount == null) {
            return;
        }
        HudLayer.Frame frame = HudLayer.Frame.current(graphics.guiWidth(), graphics.guiHeight());
        int rows = HudPositionMover.vehicleRows(mount);
        HudPosition.Bounds area = HudPositionMover.mountArea(frame, graphics.guiHeight(), rows);
        int hearts = HudPositionMover.vehicleHearts(mount);
        int health = Mth.ceil(mount.getHealth());

        gui.setupOverlayRenderState(true, false);
        graphics.pose().pushPose();
        HudPosition.moveTo(graphics.pose(), HudPosition.Offset.ZERO);
        drawHearts(graphics, area.right(), area.top(), hearts, health);
        graphics.pose().popPose();
        HudPositionMover.addDrawnRows(area);
    }
}
