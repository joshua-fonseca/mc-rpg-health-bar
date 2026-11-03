![The HUD with vanilla armor and hunger icons](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/hud-vanilla-icons.png)

A highly customizable GUI mod. Replaces the vanilla hearts with an RPG-style health bar, and adds a similar mana bar when Iron's Spells 'n Spellbooks is installed.

***

## Features

- **Health bar** the width of the hotbar. It goes beyond the vanilla max of 20 for mods that add hearts to the player (`32/40`).
- **Absorption** fills the bar in gold, and the text shows it separately (`15.5+4/20`) or added in (`19.5/20`).
- **Mana bar support:** an overhaul of the mana bar from Iron's Spells 'n Spellbooks, in the same style as the health bar, replacing Iron's own.
- **Status colors:** like the vanilla hearts, the health bar changes when poisoned, withered or frozen. The hunger bar turns green with the Hunger effect. On 1.21.1, heart colors added by other mods are followed too.
- **Armor and hunger** as the vanilla icons (default), thin bars, or hidden.
- **AppleSkin support:** saturation is outlined in yellow on the hunger bar, and held food's value flashes on it, like AppleSkin's preview on the icons.

#### Customization:

- **Half-size bars:** health and mana can each be full or half width. With both set at half, they will sit side by side (But can be moved individually)
- **Move the HUD** by dragging it: can either move the whole set, including the XP bar and level, or each piece (armor, hunger, health, mana, XP bar) on its own. It stays in place when you change the window size or GUI scale.
- **Item name height:** set how high the held item's name sits, with a preview that moves as you change it. It starts at vanilla's height, which leaves room for mods that add hotbars.
- **Air bubbles and mount health** can sit above hunger, like vanilla, or below it, for layouts with health or mana above hunger.
- **Text position:** the numbers on the health and mana bars can sit on the left, right or center, or be turned off.
- **On/off switch:** turn the whole mod off for a fully vanilla HUD, and back on, without losing your settings.
- **Live preview:** the config screen can show the HUD beside the options, drawn as your settings would, so you see each change without leaving the screen.
- **Everything in game:** changes show on the HUD as you make them, no restart. Save Settings keeps them, and leaving without saving asks first.

#### Armor and hunger as bars, with absorption

![The HUD with armor and hunger as bars, and absorption](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/hud-bars.png)

#### Status colors: poison and the Hunger effect

![Poisoned health bar and green hunger bar from the Hunger effect](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/status-colors.png)

#### Half-size health and mana bars

![Half-size health and mana bars side by side above the hotbar](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/half-bars.png)

#### AppleSkin: saturation and food preview

![With AppleSkin: saturation outlined in yellow on the hunger bar, and the hunger and saturation the held bread would restore flashing after it, then filling when it's eaten](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/appleskin.gif)

## Compatibility

| | 1.21.1 (NeoForge) | 1.20.1 (Forge) |
|---|---|---|
| **Mod loader** | NeoForge 21.1.1+ | Forge 47.0.0+ |
| **Iron's Spells 'n Spellbooks** | 3.4.0+ | 1.1.5+ (spell cost preview: 3.1.4+) |
| **AppleSkin** | 3.0.4+ | 2.5.0+ |
| **Double Hotbar** | Any ([Double Hotbar NeoForge](https://www.curseforge.com/projects/1563792), unofficial port by \_retrospective\_) | 1.3.2.1+ ([Double Hotbar](https://www.curseforge.com/minecraft/mc-mods/double-hotbar) by sidezbros) |

- **Client-side only:** servers don't need it, and it works on servers that don't have it.
- **[Iron's Spells 'n Spellbooks](https://www.curseforge.com/projects/855414) by Iron431 and Lab3:** optional. When it's installed you get the mana bar and the spell cost preview, and Iron's own mana bar is hidden.
- **[AppleSkin](https://www.curseforge.com/minecraft/mc-mods/appleskin) by squeek502:** optional. When it's installed, saturation is outlined in yellow on the hunger bar and held food's value flashes on it; with the vanilla hunger icons, AppleSkin's own overlays move with them.
- **Double Hotbar:** compatible with [Double Hotbar](https://www.curseforge.com/minecraft/mc-mods/double-hotbar) by sidezbros on 1.20.1, and its unofficial NeoForge port, [Double Hotbar NeoForge](https://www.curseforge.com/projects/1563792) by \_retrospective\_, on 1.21.1. Open **Move HUD...** and drag the HUD up above the second hotbar once; the bars, XP bar and level move together and stay there, and the item name stays above them.
- Works with other mods that draw near the hotbar the same way: drag the HUD, or just the pieces in the way, clear of them.
- Air bubbles and mount health stay vanilla and stack above the bars.
- **Texture packs:** armor and hunger on Vanilla use your pack's icons, and the hotbar, XP bar, air bubbles and mount health stay vanilla, so packs restyle them. The health and mana bars, and armor and hunger as bars, are drawn by the mod and don't change with a pack.

#### Iron's Spells: what spell cost preview looks like

![Choosing a spell in Iron's spell wheel shows the mana it costs on the mana bar, then casting it uses that mana](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/spell-cast.gif)

## Settings

Open them from **Mods → RPG Health Bar → Config** (the Mods button is also in the pause menu). Hover over any option to see what each value does. Changes show on the HUD right away: **Save Settings** keeps them. **Back to Mods** (or Esc) asks whether to save them if you changed anything.

Defaults are in bold:

<table>
<tr><th>Category</th><th>Option</th><th>Values</th></tr>
<tr><td valign="top" style="vertical-align: top"></td><td>Mod State</td><td><b>Enabled</b>, Disabled (a fully vanilla HUD; your settings are kept)</td></tr>
<tr><td rowspan="3" valign="top" style="vertical-align: top"><b>Armor + Hunger</b></td><td>Armor</td><td><b>Vanilla</b>, Bar, Hidden</td></tr>
<tr><td>Hunger</td><td><b>Vanilla</b>, Bar, Hidden</td></tr>
<tr><td>Air Bubbles/Mount HP</td><td><b>Above</b> hunger, Below it</td></tr>
<tr><td rowspan="4" valign="top" style="vertical-align: top"><b>Health</b></td><td>Health Units</td><td><b>HP</b> (20 for a full vanilla bar), Hearts (10)</td></tr>
<tr><td>Absorption</td><td><b>Separate</b> (<code>16+4/20</code>), Combined (<code>20/20</code>)</td></tr>
<tr><td>HP Text</td><td><b>Left</b>, Right, Center, Off</td></tr>
<tr><td>Health Bar Width</td><td><b>Full</b>, Half</td></tr>
<tr><td rowspan="2" valign="top" style="vertical-align: top"><b>Mana</b> (only with Iron's Spells installed)</td><td>Mana Text</td><td><b>Left</b>, Right, Center, Off</td></tr>
<tr><td>Mana Bar Width</td><td><b>Full</b>, Half</td></tr>
<tr><td rowspan="2" valign="top" style="vertical-align: top"><b>Positioning</b></td><td>Item Name...</td><td>Height of the held item's name: <b>Vanilla</b> (0), or pixels up (+) or down (-), down to right above the hotbar</td></tr>
<tr><td>Move HUD...</td><td>Drag the whole HUD (<b>Moving: All</b>) or a single piece (<b>Moving: One</b>); the arrow keys nudge it. <b>Above the hotbar</b>, Reset puts it back</td></tr>
</table>

#### The config screen

![The config screen: options in sections on the right, and the preview of the HUD on the left](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/config.png)

#### Moving the whole HUD

![Moving the whole HUD](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/move-hud.png)

#### Moving a single piece

![Moving one piece: the health bar](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/move-pieces.png)

#### Setting the item name height

![The Item Name page: a slider sets the height, and the held item's name is shown where it will go, here right above the hotbar](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/item-name.png)

#### Example layout: health and mana in the top-left

![Another layout: health and mana in the top-left corner, armor and hunger just above the XP bar](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/layout-top-left.png)