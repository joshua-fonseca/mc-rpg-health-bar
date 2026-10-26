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
- **Text** (left side of the bar), in HP, not hearts:
  - No absorption: `current/max` → `20/20`
  - With absorption: `current+absorption/max` → `20+4/20`
  - Fractions show one decimal: `17.5/20`. Whole numbers have no `.0`.

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
- Display mode: Bar (default), Vanilla icons, or Hidden.

### 1.4 Hunger bar (orange), half width
- Above the health bar, right half. No text.
- Fill = `foodData.getFoodLevel() / 20`. **Shows hunger, not saturation.**
- Display mode: Bar (default), Vanilla icons, or Hidden.

### 1.5 Vanilla elements left as-is
- **Air bubbles** (underwater) and **mount health** (horses, etc.) stay vanilla.
- The hotbar stays vanilla and doesn't move.
- The **XP bar, XP level number and horse jump meter** keep their vanilla look but **move with the group** (see 1.7).
- ⚠ Vanilla draws air bubbles and mount hearts in the same row as hunger, so they may overlap the new bars. They need to be checked during testing (see milestone 7). If they do overlap, they can be shifted up by the group's height with a pose-stack translate around those vanilla layers, without restyling them.

### 1.6 Armor / hunger display modes (in-game, no restart)
- `armorMode` and `hungerMode`, each one of:
  - `BAR` (default): our half-width bar
  - `VANILLA`: the vanilla icons, drawn just above our bars
  - `HIDDEN`: not shown
- Stored in a NeoForge **client** config (`config/rpghealthbar-client.toml`).
- The renderer reads config values every frame, so changes apply right away.
- When only one of the two is a bar, it stays in its own half (it doesn't stretch). If neither is a bar, the row is removed and the group shrinks.

### 1.7 Config screen + "Move HUD" editor
The mod provides its own config screen, opened from Mods menu → RPG Health Bar → Config, through `IConfigScreenFactory`:
- **Armor: Bar / Vanilla / Hidden**: each click cycles to the next mode
- **Hunger: Bar / Vanilla / Hidden**: each click cycles to the next mode
- **Move HUD**: opens the position editor
- **Reset Position**: puts the HUD back in the default spot above the hotbar
- **Done**

**Position editor** (`HudEditScreen`):
- Shows the whole bar group as a preview (with placeholder values if needed) and an outline around it.
- Click and drag to move the **whole set together** (the bars can't be moved one at a time).
- The set includes the **vanilla XP bar, XP level number and horse jump meter**, so they stay under the bars wherever the group goes. Without this, moving the bars up to make room for Double Hotbar would leave the XP bar behind under the second hotbar.
- The group is kept fully on screen while dragging.
- **Save** button: saves the position and goes back to the config screen.
- **Cancel** button / ESC: discards the change.
- There's no need to type X/Y numbers. Internally the position is saved as an offset from the bottom-center of the screen, so it stays put across window resizes and GUI scale changes. These offset values are stored in the config file but aren't shown as editable fields.

The point of this is to let players move the bars away from **Double Hotbar**, **Armor Status HUD**, or any other mod drawing near the hotbar.

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
- Draw bars with `GuiGraphics.fill(...)` (flat colors) at first; textured sprites can come later in polish.
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
- Health / mana bars: full width, tall enough for text (~9 px each).
- Armor / hunger bars: `(182 - gap) / 2` wide each, about half the height of the health bar.
- Default position: bottom of the group sits just above the XP bar (`screenHeight - 32`), where vanilla hearts would be.
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
8. **Polish + modpack test**: textures (optional), test in the full Antarchy Craft modpack (Double Hotbar, Armor Status HUD, Configured, Jade, Xaero's).
9. **Release**: build jar, README.

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
- [ ] Air bubbles and mount health are still visible and not covered
- [ ] Creative / spectator / F1 hide the HUD correctly
- [ ] Can be moved clear of Double Hotbar + Armor Status HUD
- [ ] XP bar, level number and horse jump meter move with the group

---

## 5. Open questions / to verify during development
- Iron's Spells' exact mana layer ID and the location of `ClientMagicData` in version 3.16.2 have to be confirmed from the jar or source.
- **Configured** may show its own generic config screen instead of ours. Our screen is registered through NeoForge's `IConfigScreenFactory`, which Configured should respect, but this needs to be checked in the modpack.
