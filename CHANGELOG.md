# Changelog

## 2.2.0 (2026-11-27)

- **Thirst Was Taken 2 support** (1.1.0+ on 1.21.1, 1.5.0+ on 1.20.1), with a new **Thirst** option:
  - **Default**: its droplet icons, in a row above hunger that moves with the HUD.
  - **Bar**: a blue half-width bar above the hunger bar.
  - **Vertical**: an upright blue bar to the right of the hotbar, one and a half times as tall as it.
  - **Hidden**.
  - Like its own bar, the bar recolors with Parched and Upset Stomach, outlines quenched with AppleSkin, and isn't shown while riding. Thirst can be moved on its own in the HUD Editor.
- New **Vertical** choice for **Hunger**: an upright bar to the right of the hotbar, with AppleSkin's saturation outline and food preview. With thirst vertical too, hunger is nearest the hotbar.
- In **Advanced** edit mode, vertical hunger and thirst bars can be made taller or shorter by dragging their top in the HUD Editor.
- **Air bubbles** and **mount health** now have places of their own and can each be dragged in the HUD Editor; they no longer move with hunger. A HUD you've already moved keeps them where they were.
  - The new **Mount View** button shows the HUD as it is while riding, so mount health can be dragged.
  - **Air Bubbles/Mount HP** sets where the bubbles start (above or under hunger) until you move them, and whether mount health's extra rows are added above or below. With **Below**, the first ten hearts are now the top row.
- The config screen's preview shows a hotbar under the HUD, and is centered in the space beside the options, which stay put as the preview changes width.
- With the config screen's preview on, an option's description shows under the preview, by hovering anywhere on its row, instead of in a tooltip. With the preview off, tooltips show as before.
- **XP Bar** has a new **Level Only** choice: just the level number, where it would be on the bar. **Level Text** still places it, and it moves with the XP bar in the HUD Editor.
- New **XP Bar Width** option in **Simple** edit mode: **Full** or **Half** (nine cells). The horse's jump bar follows it.
- The config screen's XP section is now titled **XP Bar/Mount Jump Bar**.
- **Separate v2** absorption now writes the amount in parentheses: `(+4)`.
- Fixed: with Confluence, its hunger icons also showed while riding a mount with health.
- Fixed: a horse's jump bar resized shorter lost its orange end, and a longer one had blue cells after it.
- Fixed: with **XP Bar** on **Hidden**, the HUD Editor had an invisible XP bar that could be grabbed; the horse's jump bar is now moved and resized in **Mount View**, or while riding.
- Fixed: resizing the bars or the window kept every size's bar image in memory until the game closed.
- Fixed: if a future version of Iron's Spells or Ars Nouveau can't be read, its own mana bar now shows again instead of no mana bar at all.
- Fixed: **Reset** in the HUD Editor didn't fully reset a piece that wasn't shown at the time, which could then move away from its default spot when the window size changed.
- Fixed: on 1.20.1, **Reset to Defaults**, or answering **No** to keeping changes, could leave a setting unchanged.
- Fixed: on 1.20.1, closing the game right after saving positions in the HUD Editor could lose some of them.

## 2.1.0 (2026-11-25)

- **Ars Nouveau support** (5.8.2+ on 1.21.1, 4.2.4+ on 1.20.1): its mana gets its own purple bar, like Ars's own.
  - Like Ars's own bar, the bar flashes a spell's cost in red when it fails for lack of mana and shows mana reserved by Ars add-ons too.
- A few new Compatibility notes
  - On 1.21.1, one note says how Iron's or Ars can share Confluence's mana bar (convertIronsSpellMana, convertArsNouveauMana), shown only while their bars are still separate. With the option on, only Confluence's bar shows.
  - On 1.21.1, a note flags Confluence's max mana going negative with Ars installed (a Confluence bug).
  - One note lists the mana mods that don't have options for unified mana (e.g. Iron's and Ars), a line each; compatibility mods that merge them aren't accounted for.
  - The notes are evenly spaced whether the preview is on or off, and mod names in the notes and tooltips use their mana bar's color.

## 2.0.1 (2026-11-23)

- New **Separate v2** option for **Absorption**: the health text shows just your health (`16/20`), and the absorption shows as `+4` at the right end of the health bar.
- Reverted how Air Bubbles/Mount HP gets drawn when setting it to "Below". In 2.0.0 would displace the hunger and armor bar which was not the intended use case. The intended use case was for users who place hunger and armor below mana and HP.

## 2.0.0 (2026-11-22)

- New **Edit mode** button at the top right of the config screen: **Simple** (default) gives you the standard full and half width configurations like before, but wow the new **Advanced** option lets you set the health, mana and XP bars to any width by dragging their sides.
- There is now options for the XP bar: disable the XP bar and level number, and change the level placement on the bar.
- **Move HUD...** is now renamed as **HUD Editor...**.
- **Confluence: Otherworld support** (Confluence 1.1.4+ on 1.21.1, 1.2.9+ on 1.20.1): its mana gets its own darker blue bar.
  - With Iron's Spells too, Confluence's bar sits below Iron's. In the HUD Editor each mana bar can be moved on its own. On 1.21.1, if the server has Confluence's convertIronsSpellMana on, Iron's spells use Confluence's mana, so only Confluence's bar shows, with the spell cost preview on it.
  - Confluence's own mana display is hidden, except in soul mode, where it shows soul. Its own health display is hidden too, and its armor and hunger displays are hidden unless Armor or Hunger is on Default.
  - Confluence's bar has its own **Confluence Mana Text** and **Confluence Mana Bar Width** options, listed only with Confluence; Iron's are now **Iron's Mana Text** and **Iron's Mana Bar Width**.
- New **Compatibility notes** which flags if a mod is at a version that's too old, or one that couldn't be read, or say that all is well. 
- New **Reset to Defaults** button beside Save Settings.
- Absorption above your max health now is displayed as multiple bars similar to fighting games. Before, anything over max health ran off the end of the bar.
- Other small QoL things, either to match vanilla behavior, or make things visually/flow better.

## 1.4.0 (2026-11-03)

- The config screen is redesigned. Options are grouped into sections (Armor + Hunger, Health, Mana, Positioning), and the list scrolls when the window is small.
  - Notes beside the headers say whether AppleSkin and Iron's Spells are installed. Without Iron's, the mana options are greyed out.
  - Changes still show right away. **Save Settings** keeps them.
  - The Back button is now **Back to Mods**. With unsaved changes, it (and Esc) asks whether to save them: Yes keeps them, No undoes them, and Esc on the question returns to the config.
- **Live preview:** the eye button beside the title shows the HUD on the left, drawn as your settings would at its default spot, so you can see each change without leaving the screen. It's off by default, the choice is remembered, and it's left out when the window is too narrow.
- New **Mod State** switch: Disabled gives a fully vanilla HUD, including Iron's own mana bar, and keeps your settings and positions.
- New **Air Bubbles/Mount HP** option: **Above** hunger, like vanilla, or **Below** it, for layouts with health or mana above hunger.
- **HP Text** and **Mana Text** can now be **Left** (default), **Right**, **Center** or **Off**. Text you had turned off stays off.
- The Health Bar and Mana Bar options are now called **Health Bar Width** and **Mana Bar Width**.
- Esc in **Move HUD** now saves, like on the Item Name page. Cancel still leaves the old position.
- The tips on the Item Name page are white, for easier reading.
- The Reset Position button left the config screen. **Reset** in Move HUD does the same.
- **Update check:** when a newer version is out, the Mods button and the mod's entry in the Mods list are marked, with a summary and a link to download it.

## 1.3.2 (2026-10-27)

- New **Item Name...** page in the config: sets how high the held item's name sits, from 50 pixels lower to 100 higher than vanilla. A preview shows the name where it will go and moves as you change it. Drag the slider, or use the arrow keys (Shift: 10px). Tips give the values for vanilla's height and for right above the hotbar.
- The default stays at vanilla's height, which leaves room for mods that add hotbars. The name never goes onto the HUD or the hotbar.
- The held item's name and the action bar now stay above rows that other mods reserve above the hotbar, like extra hotbars.
- The HUD's layout is worked out once per frame instead of once per piece, and the item name's position reads the game state once.
- Changing a setting writes the config file once instead of twice.

## 1.3.1 (2026-10-27)

- Absorption no longer appears at once: when you gain it, the gold fill grows in from the left over 0.5 seconds. When you lose it, the lost part turns darker and fades away like health does when you take damage, but quicker (it stays 0.5 seconds instead of 0.75).

## 1.3.0 (2026-10-27)

- New **Health Bar** and **Mana Bar** options: **Full** or **Half** width. Half bars are as wide as the armor and hunger bars. With both set to Half, health and mana sit side by side above the hotbar; with only one, it keeps its own row, e.g. a big health bar with a small mana bar underneath.
- **AppleSkin** support (optional, 3.0.4 or newer):
  - Saturation is outlined in yellow on the hunger bar.
  - While holding food, the hunger and saturation it would restore flash on the hunger bar, like AppleSkin's preview on the icons.
  - With the vanilla hunger icons, AppleSkin's outlines, food preview and exhaustion move with the hunger icons. They're hidden when hunger is a bar or hidden, and AppleSkin's heart preview is hidden since the hearts are replaced.
  - Follows AppleSkin's own settings for these overlays.
- The Regeneration sweep is drawn in one go instead of column by column, making it cheaper.

## 1.2.1 (2026-10-27)

- Each piece now keeps its own place when the window size or GUI scale changes: a piece in a corner stays in that corner, and pieces left above the hotbar stay above the hotbar. Pieces that touch stay together. Before, a layout with pieces spread across the screen drifted as a whole.
- In Move HUD, the title and buttons move to the middle of the screen when pieces are at both the top and the bottom, so they don't cover any.
- Positions saved by earlier versions load as before, and switch to the new format the next time you save in Move HUD.

## 1.2.0 (2026-10-27)

- Move HUD has a new **Moving: All / One** button. With One, you drag a single piece on its own: armor, hunger, health, mana, or the XP bar (with the level and jump meter). Air bubbles and mount health move with hunger. With All, the whole HUD moves and the pieces keep their arrangement.
- In Move HUD, what you'd grab is faintly highlighted when you hover over it, and a bit more while you drag it.
- The held item's name and the action bar message now sit just above whatever is over the hotbar, or right above the hotbar when nothing is. Mods that raise the item name to make room (like Double Hotbar) are taken into account.
- Reset in Move HUD, and Reset Position in the config, also put every piece back.
- The mod's description is shorter, and its author is shown as itskkura.

## 1.1.0 (2026-10-26)

- The health bar changes color with the heart type, like the vanilla hearts: green-yellow when poisoned, black when withered, light blue when frozen. Heart types added or changed by other mods are followed, using their heart texture's color.
- The hunger bar turns green with the Hunger effect, like the vanilla hunger icons.
- At 4 HP or less (health plus absorption), the health bar pulses as a warning, like the vanilla hearts shaking.
- With Regeneration, a light band sweeps along the health bar, like the vanilla hearts' wave.

## 1.0.0 (2026-10-26)

First release, for Minecraft 1.21.1 on NeoForge.

- Health bar replacing the vanilla hearts, with current/max health on it.
- Absorption shown in gold on the health bar, written separately or added in.
- Mana bar for Iron's Spells 'n Spellbooks (optional), replacing Iron's own.
- Spell cost preview on the mana bar while Iron's spell wheel is open.
- Damage trail: lost health or mana stays as a darker section, then shrinks away.
- Armor and hunger as vanilla icons, thin bars, or hidden.
- Health shown in HP or hearts; HP and mana text can each be turned off.
- Drag the whole HUD, including the XP bar and level, anywhere on screen; the position holds across window sizes and GUI scales.
- All settings in game from the Config button, with tooltips; changes apply immediately.
