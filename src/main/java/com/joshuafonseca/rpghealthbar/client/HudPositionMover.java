package com.joshuafonseca.rpghealthbar.client;

import com.joshuafonseca.rpghealthbar.compat.ConfluenceCompat;
import com.joshuafonseca.rpghealthbar.compat.ThirstCompat;
import com.joshuafonseca.rpghealthbar.config.AirPosition;
import com.joshuafonseca.rpghealthbar.config.ClientConfig;
import com.joshuafonseca.rpghealthbar.config.HudPiece;
import com.joshuafonseca.rpghealthbar.config.ThirstMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

/**
 * Places the vanilla HUD elements that belong to the movable set at the same offset as our bars,
 * replacing any shift other mods gave them so the set always stays together.
 */
public final class HudPositionMover {
    /**
     * The vanilla overlays that move, and the piece each moves with. AppleSkin draws on the food
     * icons in the food overlay's Pre and Post, so it moves with them.
     */
    private static final Map<ResourceLocation, HudPiece> MOVED_OVERLAYS = Map.ofEntries(
            Map.entry(VanillaGuiOverlay.JUMP_BAR.id(), HudPiece.XP),
            Map.entry(VanillaGuiOverlay.ARMOR_LEVEL.id(), HudPiece.ARMOR),
            // Vanilla stacks air and mount health (which replaces food while riding) on the food side.
            Map.entry(VanillaGuiOverlay.FOOD_LEVEL.id(), HudPiece.HUNGER),
            Map.entry(VanillaGuiOverlay.AIR_LEVEL.id(), HudPiece.AIR),
            Map.entry(VanillaGuiOverlay.MOUNT_HEALTH.id(), HudPiece.MOUNT),
            // Confluence's own armor and hunger icons, shown with Armor or Hunger on Default, sit
            // where vanilla's would.
            Map.entry(ConfluenceCompat.ARMOR_LAYER, HudPiece.ARMOR),
            Map.entry(ConfluenceCompat.FOOD_LAYER, HudPiece.HUNGER));

    /** Whether the overlay currently rendering was translated, so Post only pops what Pre pushed. */
    private static boolean pushed;

    /** Vanilla's rows are 9px icons plus a 1px gap. */
    public static final int ROW_HEIGHT = 10;
    /** Where this frame's air and mount health rows were drawn, for those that took no room above the bars. */
    private static final List<HudPosition.Bounds> drawnRows = new ArrayList<>();
    /** ForgeGui.rightHeight before an air or mount health overlay, or -1; see {@link #onPost}. */
    private static int rightHeightBefore = -1;
    /** Where that overlay's rows are drawn, and whether they're where rows above the bars normally stack. */
    private static HudPosition.Bounds rowsArea;
    private static boolean rowsStacked;

    /** ForgeGui.rightHeight before Thirst Was Taken 2's overlay on Default, or -1; see {@link #keepThirstRow}. */
    private static int rightHeightBeforeThirst = -1;

    private HudPositionMover() {
    }

    /**
     * The air and mount health rows drawn this frame anywhere but stacked above the bars, which the
     * held item's name also has to stay clear of.
     */
    public static List<HudPosition.Bounds> drawnRows() {
        return drawnRows;
    }

    /** Counts rows drawn away from the stack above the bars, for {@link #drawnRows}. */
    static void addDrawnRows(HudPosition.Bounds area) {
        drawnRows.add(area);
    }

    /** Called as the GUI starts drawing. */
    public static void newFrame() {
        drawnRows.clear();
    }

    /** Registered at high priority, after replaced overlays are cancelled and before other mods draw on them. */
    public static void onPre(RenderGuiOverlayEvent.Pre event) {
        ResourceLocation id = event.getOverlay().id();
        if (id.equals(ThirstCompat.LAYER) && ThirstCompat.isAvailable() && ClientConfig.ENABLED.get()) {
            if (Minecraft.getInstance().gui instanceof ForgeGui gui) {
                moveThirstIcons(event.getGuiGraphics(), gui);
            }
            return;
        }
        HudPiece piece = MOVED_OVERLAYS.get(id);
        if (piece == null || !ClientConfig.ENABLED.get()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        HudLayer.Frame frame = HudLayer.Frame.current(graphics.guiWidth(), graphics.guiHeight());
        graphics.pose().pushPose();
        HudPosition.moveTo(graphics.pose(), frame.pieceOffset(piece));
        if ((piece == HudPiece.AIR || piece == HudPiece.MOUNT) && Minecraft.getInstance().gui instanceof ForgeGui gui) {
            placeRows(graphics, gui, frame, piece);
        }
        pushed = true;
    }

    /**
     * Thirst Was Taken 2's droplets (Default): from where it stacks them above hunger to the thirst
     * piece. Its overlay only runs this far on Default; otherwise it's hidden.
     */
    private static void moveThirstIcons(GuiGraphics graphics, ForgeGui gui) {
        HudLayer.Frame frame = HudLayer.Frame.current(graphics.guiWidth(), graphics.guiHeight());
        if (frame.thirst() != ThirstMode.VANILLA) {
            return;
        }
        rightHeightBeforeThirst = gui.rightHeight;
        int stackedTop = graphics.guiHeight() - rightHeightBeforeThirst;
        graphics.pose().pushPose();
        HudPosition.moveTo(graphics.pose(), frame.pieceOffset(HudPiece.THIRST)
                .plus(new HudPosition.Offset(0, frame.layout().thirstY() - stackedTop)));
        pushed = true;
    }

    /**
     * With a thirst row in the layout, starts the rows stacked above hunger (air bubbles, other
     * mods') above it, however far {@code rightHeight} had got before Thirst Was Taken 2's overlay.
     * Without a thirst row (thirst not shown, as while riding a living entity), or with the bars
     * hidden (creative, spectator), the rows are left where they are.
     */
    public static void keepThirstRow(GuiGraphics graphics, ForgeGui gui, int rightHeight) {
        HudLayer.Frame frame = HudLayer.Frame.current(graphics.guiWidth(), graphics.guiHeight());
        if (frame.pieceBounds().containsKey(HudPiece.THIRST) && HudLayer.isVisible(Minecraft.getInstance())) {
            gui.rightHeight = Math.max(rightHeight, HudLayer.vanillaRowsStart(graphics.guiHeight(), frame));
        }
    }

    /**
     * Moves the air bubbles' or mount health's rows from where vanilla stacks them above hunger to
     * their piece. Vanilla draws the first row lowest and any more above it.
     */
    private static void placeRows(GuiGraphics graphics, ForgeGui gui, HudLayer.Frame frame, HudPiece piece) {
        int mountRows = mountRows(Minecraft.getInstance());
        HudPosition.Bounds area;
        if (piece == HudPiece.MOUNT) {
            area = mountArea(frame, graphics.guiHeight(), Math.max(1, mountRows));
        } else {
            // Mount health is only in the way if it's drawn, which the HUD Editor's normal view doesn't.
            boolean mountDrawn = mountRows > 0 && !Boolean.FALSE.equals(HudPosition.previewMounted());
            area = airArea(frame, graphics.guiHeight(), mountDrawn ? mountArea(frame, graphics.guiHeight(), mountRows) : null);
        }
        int vanillaFirstRow = graphics.guiHeight() - gui.rightHeight;
        int firstRow = area.bottom() - HudLayout.ICON_ROW_HEIGHT;
        // The pose is already shifted by the piece's offset.
        graphics.pose().translate(0, firstRow - frame.pieceOffset(piece).y() - vanillaFirstRow, 0);
        rightHeightBefore = gui.rightHeight;
        rowsArea = area;
        // At their default spot above the bars, other mods' rows stack on top of them.
        rowsStacked = ClientConfig.AIR_POSITION.get() == AirPosition.ABOVE && frame.pieceOffset(piece).equals(HudPosition.Offset.ZERO);
    }

    /**
     * Where {@code rows} rows of mount health are drawn: from the mount piece upward, or downward with
     * Air Bubbles/Mount HP on Below, and moved back on screen if they'd reach past its edge.
     */
    public static HudPosition.Bounds mountArea(HudLayer.Frame frame, int screenHeight, int rows) {
        HudPosition.Bounds first = frame.placed(HudPiece.MOUNT);
        int extra = (rows - 1) * ROW_HEIGHT;
        int top = ClientConfig.AIR_POSITION.get() == AirPosition.BELOW ? first.top() : first.top() - extra;
        int height = first.height() + extra;
        top = Math.max(0, Math.min(top, screenHeight - height));
        return new HudPosition.Bounds(first.left(), top, first.right(), top + height);
    }

    /**
     * Where the air bubbles are drawn: at their piece, or past mount health's rows if those are in the
     * way, above them or, with Air Bubbles/Mount HP on Below, under them.
     *
     * @param mount where mount health's rows are drawn, or null if they aren't
     */
    public static HudPosition.Bounds airArea(HudLayer.Frame frame, int screenHeight, @Nullable HudPosition.Bounds mount) {
        HudPosition.Bounds air = frame.placed(HudPiece.AIR);
        if (mount == null || !air.intersects(mount)) {
            return air;
        }
        int gap = ROW_HEIGHT - air.height();
        int above = mount.top() - ROW_HEIGHT;
        int below = mount.bottom() + gap;
        boolean down = ClientConfig.AIR_POSITION.get() == AirPosition.BELOW;
        // The other way if there's no room.
        int top = down ? (below + air.height() <= screenHeight ? below : above) : (above >= 0 ? above : below);
        return new HudPosition.Bounds(air.left(), top, air.right(), top + air.height());
    }

    /** How many rows of mount health the player's mount has; 0 without a mount whose health is shown. */
    public static int mountRows(Minecraft minecraft) {
        LivingEntity mount = minecraft.player == null ? null : mountWithHealth(minecraft.player);
        return mount == null ? 0 : vehicleRows(mount);
    }

    /** Whether the game is drawing air bubbles right now. */
    public static boolean airShown(Minecraft minecraft) {
        return HudLayer.isVisible(minecraft) && minecraft.getCameraEntity() instanceof Player player
                && (player.isEyeInFluid(FluidTags.WATER) || player.getAirSupply() < player.getMaxAirSupply());
    }

    /** Whether the game is drawing mount health right now. */
    public static boolean mountShown(Minecraft minecraft) {
        return HudLayer.isVisible(minecraft) && minecraft.getCameraEntity() instanceof Player player && mountWithHealth(player) != null;
    }

    /**
     * The mount the player rides, if vanilla shows its health in hunger's place (the food overlay
     * checks showVehicleHealth); null otherwise.
     */
    @Nullable
    public static LivingEntity mountWithHealth(Player player) {
        return player.getVehicle() instanceof LivingEntity vehicle && vehicle.showVehicleHealth() ? vehicle : null;
    }

    /** Hearts of mount health vanilla draws, up to 30 (as in ForgeGui.renderHealthMount). */
    static int vehicleHearts(LivingEntity mount) {
        return Math.min((int) (mount.getMaxHealth() + 0.5F) / 2, 30);
    }

    /** Rows of mount health vanilla draws: 10 hearts a row. */
    static int vehicleRows(LivingEntity mount) {
        return Math.max(1, (vehicleHearts(mount) + 9) / 10);
    }

    /**
     * Places the item name and action bar. Registered at the lowest priority, so it sees every move
     * other mods give the text in Pre, and every row they reserve above the hotbar for it.
     */
    public static void onTextPre(RenderGuiOverlayEvent.Pre event) {
        ResourceLocation id = event.getOverlay().id();
        boolean itemName = id.equals(VanillaGuiOverlay.ITEM_NAME.id());
        if (!itemName && !id.equals(VanillaGuiOverlay.RECORD_OVERLAY.id()) || !ClientConfig.ENABLED.get()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        HudLayer.Frame frame = HudLayer.Frame.current(graphics.guiWidth(), graphics.guiHeight());
        // Placed vertically from scratch: an upward move another mod gave the text is applied from
        // vanilla's spot, and replaced. Horizontal moves are kept.
        float foreignY = graphics.pose().last().pose().m31();
        int raise = Math.max(0, Math.round(-foreignY));
        int shift = itemName
                ? HudTextPlacer.itemNameShift(graphics.guiWidth(), graphics.guiHeight(), frame, raise)
                : HudTextPlacer.overlayShift(graphics.guiWidth(), graphics.guiHeight(), frame, raise);
        graphics.pose().pushPose();
        graphics.pose().translate(0, shift - foreignY, 0);
        pushed = true;
    }

    /** A mod cancelled the overlay after we shifted it; Forge won't send Post, so undo it here. */
    public static void onCancelled(RenderGuiOverlayEvent.Pre event) {
        if (event.isCanceled() && pushed) {
            event.getGuiGraphics().pose().popPose();
            pushed = false;
            rightHeightBefore = -1;
            rightHeightBeforeThirst = -1;
        }
    }

    /** Registered at the lowest priority, so mods drawing on the overlay in Post are shifted too. */
    public static void onPost(RenderGuiOverlayEvent.Post event) {
        if (rightHeightBefore >= 0 && Minecraft.getInstance().gui instanceof ForgeGui gui) {
            boolean drawn = gui.rightHeight != rightHeightBefore;
            if (drawn && rowsStacked) {
                int screenHeight = event.getGuiGraphics().guiHeight();
                gui.rightHeight = Math.max(rightHeightBefore, screenHeight - rowsArea.top() + ROW_HEIGHT);
            } else {
                // Rows drawn elsewhere take no room above the bars, so rows stacked there (other
                // mods') and the held item's name don't leave a gap for them.
                if (drawn) {
                    drawnRows.add(rowsArea);
                }
                gui.rightHeight = rightHeightBefore;
            }
        }
        rightHeightBefore = -1;
        if (rightHeightBeforeThirst >= 0 && Minecraft.getInstance().gui instanceof ForgeGui gui) {
            keepThirstRow(event.getGuiGraphics(), gui, rightHeightBeforeThirst);
        }
        rightHeightBeforeThirst = -1;
        if (pushed) {
            event.getGuiGraphics().pose().popPose();
            pushed = false;
        }
    }
}
