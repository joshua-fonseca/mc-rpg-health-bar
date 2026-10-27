# MC RPG Health Bar — Plan

A client-side NeoForge mod for Minecraft **1.21.1** that replaces the vanilla heart/armor/hunger HUD with RPG-style bars, including a mana bar for **Iron's Spells 'n Spellbooks**.

Reference mockup: `Screenshot 2026-10-26 140202.png`

```
 [ armor (white) ......... ][ hunger (orange) ........ ]   <- half width each, side by side, thin, no text
 [ 20+4/20  health (red, yellow absorption overlay) .. ]   <- full hotbar width
 [ 6/10     mana (blue) .............................. ]   <- full hotbar width (only if Iron's Spells is installed)
 [ xp bar (vanilla, green) ........ 39 ............... ]   <- vanilla look, but moves with the group
 [ hotbar ]                                                <- stays put
```

Everything above the hotbar, including the XP bar, is one **movable set**.

---

## 1. Features

### 1.1 Health bar (red)
- Replaces vanilla hearts. Spans the full width of the hotbar (182 px).
- Max is read live from `player.getMaxHealth()` every frame, so attribute modifiers (Apothic Attributes, effects, gear, etc.) are reflected automatically. Not fixed at 20.
- Red fill = `health / maxHealth`, clamped 0–1.
- **Absorption**: drawn as a yellow/gold overlay on the HP bar, starting from the left. Width = `absorption / maxHealth`, clamped to the full bar.
- **Text** (left side of the bar):
  - No absorption: `current/max` → `20/20`
  - With absorption, **Separate** (default): `current+absorption/max` → `16+4/20`; **Combined**: `sum/max` → `20/20`
  - **Health Units**: HP (default, `20/20`) or Hearts (HP / 2, `10/10`)
  - Rounded to the nearest 0.25 (after converting units): `16.75/20`, `16.5/20`. No trailing zeros, so whole numbers have no `.0`. (0.25 rather than 0.5 so a sliver of health still shows as `0.25` instead of `0`.)

### 1.2 Mana bar (blue)
- Directly below the health bar, same width.
- Text on the left: `current/max`, formatted the same way as HP.
- Reads data from Iron's Spells:
  - current mana: `ClientMagicData.getPlayerMana()` (client-synced value)
  - max mana: the player's `irons_spellbooks:max_mana` attribute value
- **No toggle.** It shows automatically when Iron's Spells is installed and is hidden when it isn't. Iron's Spells is an *optional* dependency, so the mod never requires it.
- **Iron's own mana bar is hidden** automatically by this mod (cancel its GUI layer), since this mod replaces it.

### 1.3 Armor bar (white), half width
- Above the health bar, left half. No text.
- Fill = `player.getArmorValue() / 20`, using the vanilla HUD max of 20. Values over 20 show as a full bar.
- Display mode: Vanilla icons (default), Bar, or Hidden.

### 1.4 Hunger bar (orange), half width
- Above the health bar, right half. No text.
- Fill = `foodData.getFoodLevel() / 20`. **Shows hunger, not saturation.**
- Display mode: Vanilla icons (default), Bar, or Hidden.

### 1.5 Vanilla elements left as-is
- **Air bubbles** (underwater) and **mount health** (horses, etc.) stay vanilla.
- The hotbar stays vanilla and doesn't move.
- The **XP bar, XP level number and horse jump meter** keep their vanilla look but **move with the group** (see 1.7). The level number is moved down onto the XP bar, centered on it horizontally and vertically (like the mockup).
- Air bubbles and mount hearts stack above the bars (vanilla's `Gui.leftHeight`/`rightHeight` are pushed above our group) and move with it. Verified in milestone 7, including multi-row horse health and the horse jump meter.
- Known limit: if the HUD is placed against the very top of the screen, extra vanilla rows that only appear sometimes (air bubbles, horse hearts) can go off the top edge. The HUD isn't shifted to make room for them, so it doesn't jump around when you mount or dive.

### 1.6 Armor / hunger display modes (in-game, no restart)
- `armorMode` and `hungerMode`, each one of:
  - `VANILLA` (default): the vanilla icons, in the same row as the half bars (the row grows to the icons' 9px height and the bar is centered vertically in it)
  - `BAR`: our half-width bar
  - `HIDDEN`: not shown
- Stored in a NeoForge **client** config (`config/rpghealthbar-client.toml`).
- The renderer reads config values every frame, so changes apply right away.
- When only one of the two is a bar, it stays in its own half (it doesn't stretch). If neither is a bar, the row is removed and the group shrinks.

### 1.7 Config screen + "Move HUD" editor
The mod provides its own config screen, opened from Mods menu → RPG Health Bar → Config, through `IConfigScreenFactory`:
- **Armor: Vanilla / Bar / Hidden**: each click cycles to the next mode
- **Hunger: Vanilla / Bar / Hidden**: each click cycles to the next mode
- **Health Units: HP (20) / Hearts (10)**
- **Absorption: Separate / Combined** (16+4/20 vs 20/20)
- **HP Text: On / Off** and **Mana Text: On / Off** (default On): whether the numbers are written on the bar
- The buttons are laid out in two columns so the screen fits small windows. Each option reads "Name: [value]": the name is plain text and the button shows only the value.
- **Move HUD**: opens the position editor
- **Reset Position**: puts the HUD back in the default spot above the hotbar
- **Done**
- Hovering over a button shows a tooltip listing its possible values and what each does: each value on its own paragraph, its name in gold, the default marked "(default)" in grey.

**Position editor** (`HudEditScreen`):
- Shows the whole bar group as a preview (with placeholder values if needed), with no outline. What a click would grab is lightened faintly on hover, and more while dragging, exactly over each piece's pixels.
- A **Moving: All / One** button (1.2.0). **All** (default) drags the whole set. **One** drags a single piece: armor, hunger, health, mana, or XP (which carries the level and jump meter; air bubbles and mount health go with hunger, as vanilla stacks them on that side). Each piece's shift is saved on top of the set's position (`<piece>OffsetX/Y` in the config), so moving the whole set keeps the arrangement. The set's bounds, used for clamping and the anchor, are the union of the moved pieces. Thin pieces can be grabbed up to 2px outside their edges.
- The set includes the **vanilla XP bar, XP level number and horse jump meter**, so they stay under the bars wherever the group goes. Without this, moving the bars up to make room for Double Hotbar would leave the XP bar behind under the second hotbar.
- The group is kept fully on screen while dragging.
- Arrow keys nudge the set (or, in One mode, the last grabbed piece) by 1 pixel (10 with Shift).
- **Save** button: saves the position and goes back to the config screen.
- **Reset** button: moves the set back to the default spot and every piece back into place (still needs Save).
- **Cancel** button / ESC: discards the change.
- The title, hint and buttons sit in whichever half of the screen the HUD isn't in, so they never cover it.
- In a world, the real HUD moves live while dragging. From the title screen (or in creative, where the bars aren't drawn), a preview with placeholder values is shown instead.
- There's no need to type X/Y numbers. On Save, the position is stored relative to the **nearest of 9 screen spots** (corners, edge midpoints, center), as `anchor` + `offsetX`/`offsetY` in the config file (not shown as editable fields). Left near the hotbar, it's measured from the bottom-middle, so it keeps its distance from the hotbar; put in a corner, it stays in that corner. Either way it holds up when the window size or GUI scale changes.
- The bars scale with Minecraft's GUI Scale setting, like the hotbar. There's no separate size setting.

The point of this is to let players move the bars away from **Double Hotbar**, **Armor Status HUD**, or any other mod drawing near the hotbar.

**No mod-specific handling:** the default layout always assumes the single vanilla hotbar. The mod doesn't detect or adjust for other HUD mods; the player moves the whole set if something overlaps it.

**Item name and action bar** (1.2.0, `HudTextPlacer`): placed just above whatever pieces are stacked directly over the hotbar (a gap too small for the text counts as stacked), plus any rows above them (air, mount health), or right above the hotbar when nothing is. In creative only the jump meter counts. If another mod moves the item name up (e.g. Double Hotbar makes room for its second row), that move is taken as space the mod uses above the hotbar: the stack is measured from above it and the text is placed vertically from scratch, so the moves don't add up.

**The set is placed only by this mod:** some mods (e.g. Double Hotbar) shift vanilla layers like the XP bar or hunger icons themselves. For layers in the set, the mod replaces any such horizontal/vertical shift with its own position, so those mods can't pull the set apart; the player drags the whole set instead.

---

## 2. Technical approach

### 2.1 Project setup
- Template: NeoForge MDK for 1.21.1 (`neoforged/MDK-1.21.1-ModDevGradle`).
- Java 21, Gradle with ModDevGradle.
- Mod ID: `rpghealthbar` (placeholder).
- Client-only mod, so servers don't need it installed.
- Iron's Spells added as a **compile-only** dependency (with the matching version 3.16.2 + Iron's Lib for compiling), and declared `type = "optional"` in `neoforge.mods.toml`.

### 2.2 Rendering (NeoForge GUI layers)
- Register one custom layer with `RegisterGuiLayersEvent`, placed above `VanillaGuiLayers.PLAYER_HEALTH`.
- Hide vanilla layers through `RenderGuiLayerEvent.Pre` by cancelling:
  - `VanillaGuiLayers.PLAYER_HEALTH` (hearts)
  - `VanillaGuiLayers.ARMOR_LEVEL` (armor icons)
  - `VanillaGuiLayers.FOOD_LEVEL` (hunger shanks)
  - Iron's Spells mana layer (by its layer ID, only when the mod is loaded)
- Draw bars with `GuiGraphics.fill(...)` (batched with `drawManaged`), in the style of the vanilla XP bar's sprites but without its cells: the empty part uses the XP background's colors (dark border, lighter top/bottom rows, darker middle); the fill has a very dark tinted border, a bright middle row, a darker bottom row and a fixed per-pixel noise texture. No texture files: each full bar is painted once per color into a small `DynamicTexture`, and a frame blits the needed columns (only the fill's brighter end is drawn pixel by pixel). This cut the HUD's draw time about 3x versus filling every pixel each frame.
- Draw text with `GuiGraphics.drawString(font, text, x, y, color, shadow)`.
- Move vanilla layers with the group by shifting them by the group's offset with `pose().pushPose()` + `translate` in `RenderGuiLayerEvent.Pre` and `popPose()` in `Post`, for `EXPERIENCE_BAR`, `EXPERIENCE_LEVEL` and `JUMP_METER`. The Pre listener runs at the lowest priority, so it only pushes if no other mod cancelled the layer, which keeps every push matched by a pop.
- Don't draw the HUD in spectator mode, in creative, or when the HUD is hidden (F1), the same as vanilla.

### 2.3 Classes (draft)
| Class | Responsibility |
|---|---|
| `RpgHealthBarMod` | Entry point, registers config, events, config screen factory |
| `config/ClientConfig` | `ModConfigSpec`: `armorMode`, `hungerMode`, `offsetX`, `offsetY` |
| `client/HudLayout` | Computes the group's position/size from config + screen size (used by both the HUD and the editor) |
| `client/HudRenderer` | Draws the whole group at a given position (used by both the HUD layer and the editor preview) |
| `client/BarRenderer` | Draws one bar (background, fill, overlay, border, optional label) |
| `client/VanillaHudHider` | Cancels vanilla + Iron's mana layers |
| `client/screen/RpgConfigScreen` | Toggles, Move HUD, Reset, Done |
| `client/screen/HudEditScreen` | Drag the group around, Save/Cancel |
| `compat/IronsSpellsCompat` | The only class that touches Iron's Spells code; only used if `ModList.get().isLoaded("irons_spellbooks")` |

### 2.4 Layout (default, GUI-scaled pixels)
- Group width: 182 (hotbar width), centered horizontally.
- Health / mana bars: full width, 7 px tall (just fits the text, vertically centered), with a 1 px border. Corners are rounded like the XP bar (corner pixels left out).
- Health / mana fills: the last 6 px brighten gradually (up to 18%), ending in a near-white tip column that keeps the rows' shading.
- Losing health or mana: the lost section stays as a darker red/blue for 0.75 s, then shrinks smoothly to the new value (gone in well under a second). Gains show at once. Only on the real HUD, not the editor's placeholder preview.
- Status colors: the health bar follows the vanilla heart type (`Gui.HeartType` rules plus NeoForge's `PlayerHeartTypeEvent`, so other mods' heart types apply): poisoned green-yellow, withered near-black, frozen light blue; heart types from other mods use their full-heart sprite's average color. The hunger bar turns green with the Hunger effect. The lost-section color is a darker shade of the current color.
- Health animations (stand-ins for the vanilla hearts' movement): at 4 HP or less (health + absorption, vanilla's shake threshold) the fill pulses toward white (0.9s period, alpha up to 0.35); with Regeneration a 24px soft light band sweeps the fill every 1.5s. Both skip the white tip and don't run in the editor preview. Tried and dropped: a white flash on damage (the dark trail already shows it), a glow on healing, and a hunger pulse (on empty saturation, then at 0 food).
- Spell cost preview: while Iron's spell wheel is open, the hovered spell's mana cost is shown at the right end of the mana fill, pulsing between dark and normal blue (0.8 s period). Read from Iron's private `SpellWheelOverlay.wheelSelection` by reflection, behind its own safety net (if it fails, only the preview turns off).
- Armor / hunger bars: `(182 - gap) / 2` wide each, 3 px tall, no border, rounded corners.
- 2 px between the armor/hunger, health and mana rows, the same as between the XP bar and the hotbar.
- Default position: the bars sit 2 px above the XP bar; the level number sits on the XP bar.
- Without Iron's Spells, the mana row is removed and the group shrinks.

### 2.5 Colors (initial)
| Bar | Color |
|---|---|
| Health | red `#E03030` |
| Absorption overlay | gold `#F0C030` |
| Mana | blue `#30A0F0` |
| Armor | white `#E0E0E0` |
| Hunger | orange `#E08020` |
| Background | dark gray, semi-transparent |

---

## 3. Milestones
1. **Scaffold**: MDK project builds, mod loads in a dev client.
2. **Health bar**: vanilla hearts hidden, red bar + text, works with max HP above 20, decimals, and the absorption overlay and text.
3. **Armor + hunger bars**: half-width bars side by side, vanilla armor/hunger icons hidden.
4. **Config + config screen**: toggles work live from the in-game config screen.
5. **Iron's Spells compat**: mana bar, Iron's own bar hidden, no crash or mana bar without Iron's.
6. **Move HUD editor**: drag the group (including the XP bar, level number and jump meter), Save/Cancel, Reset; position persists.
7. **Vanilla overlap check**: air bubbles and mount health don't collide with the bars, and get shifted if needed.
8. **Styling + modpack test**: slim bars with shaded fills, thin armor/hunger lines, level number centered on the XP bar (done); test in the full Antarchy Craft modpack (Double Hotbar, Armor Status HUD, Configured, Jade, Xaero's). Modpack test done: confirmed working in the Antarchy Craft modpack on NeoForge 21.1.235 (2026-10-26).
9. **Release**: build jar, README. Done: version 1.0.0 with a pixel-art icon (`rpghealthbar.png`), mod description and homepage link, README with screenshots (`docs/images/`), CHANGELOG, tag `v1.0.0`. 1.1.0: status colors, low-health pulse and Regeneration sweep; tag `v1.1.0`. 1.2.0: moving single pieces, hover highlight, item name placement; tag `v1.2.0`.

## 4. Testing checklist
- [ ] Max HP 20, 40, and odd values (e.g. 27) display correctly
- [ ] Fractional HP shows one decimal (`17.5/20`), whole HP shows none (`20/20`)
- [ ] Absorption: overlay shows, text reads `20+4/20`, and the overlay clamps when absorption > max HP
- [ ] Armor > 20 shows a full bar, no crash
- [ ] Hunger bar tracks food level (not saturation)
- [ ] Switching armor/hunger between Bar / Vanilla / Hidden in-game updates without a restart
- [ ] Move HUD: drag, Save keeps the new position, Cancel reverts it, Reset goes back to default
- [ ] Position survives a relog, a window resize, and a GUI scale change
- [ ] Without Iron's Spells installed: no crash, no mana bar
- [ ] With Iron's Spells installed: only one mana bar (Iron's own is hidden)
- [x] Air bubbles and mount health are still visible and not covered
- [ ] Creative / spectator / F1 hide the HUD correctly
- [ ] Can be moved clear of Double Hotbar + Armor Status HUD
- [x] XP bar, level number and horse jump meter move with the group

---

## 5. Open questions / to verify during development
- Iron's Spells' exact mana layer ID and the location of `ClientMagicData` in version 3.16.2 have to be confirmed from the jar or source.
- **Configured** may show its own generic config screen instead of ours. Our screen is registered through NeoForge's `IConfigScreenFactory`, which Configured should respect, but this needs to be checked in the modpack.
