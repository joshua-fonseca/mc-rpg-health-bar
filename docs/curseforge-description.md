![The HUD with vanilla armor and hunger icons](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/hud-vanilla-icons.png)

My original plan was to only replace the vanilla hearts with an RPG-style health bar and add a similar mana bar when Iron's Spells 'n Spellbooks is installed, but now this has turned into a full-blown GUI customization mod, supporting BOTH Iron's and Confluence, with the goal of looking not too far from Vanilla.

***

## Features

*   **Health bar** that goes beyond the vanilla max of 20 for mods that increase max health (`32/40`).
*   **Absorption** fills the health bar in yellow, and the text shows it separately (`15.5+4/20`) or added in (`19.5/20`).
*   **Mana bar support:** Supports mana bars from Iron's Spells 'n Spellbooks and/or Confluence: Otherworld, in the same style as the health bar.
*   **Status colors:** the health bar changes when poisoned, withered or frozen. The hunger bar changes with the Hunger effect. On 1.21.1, heart colors added by other mods are followed too.
*   **Armor and hunger** can be the vanilla icons (default), thin bars, or hidden.
*   **AppleSkin support:** saturation is outlined in yellow on the hunger bar, and held food's value flashes on it, like AppleSkin's preview on the icons.
*   **Heavy customization:** move individual pieces of the HUD wherever you like, and more, explained below.

#### Armor and hunger as bars, with absorption

![The HUD with armor and hunger as bars, and absorption](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/hud-bars.png)

#### Status colors: poison and the Hunger effect

![Poisoned health bar and green hunger bar from the Hunger effect](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/status-colors.png)

#### Confluence: Otherworld's mana bar

![Confluence's mana in its own darker blue bar under the health bar, with armor and hunger as bars](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/confluence-mana.png)

#### No mana mod? No mana bar

![Without a mana mod: just the health bar, with the vanilla armor and hunger icons](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/no-mana.png)

#### AppleSkin: saturation and food preview

![With AppleSkin: saturation outlined in yellow on the hunger bar, and the hunger and saturation the held bread would restore flashing after it, then filling when it's eaten](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/appleskin.gif)

#### Customization:

*   **Move the HUD** by dragging it: can either move the whole set, including the XP bar and level, or each piece (armor, hunger, health, each mana bar, XP bar) on its own. It stays in place when you change the window size or GUI scale.
*   **Half-size bars:** health and mana can each be full or half width, and moved individually.
*   **Advanced edit mode:** For users who want even more customization, you can set the health, mana and XP bars to any width by dragging their ends in the HUD Editor.
*   **XP bar:** put the level number in the center or at either end, or hide the XP bar entirely.
*   **Item name height:** set how high the held item's name sits, with a preview that moves as you change it. It starts at vanilla's height, which leaves room for mods that add hotbars.
*   **Air bubbles and mount health** can sit above hunger, like vanilla, or below it, for layouts with health or mana above hunger.
*   **Text position:** the numbers on the health and mana bars can sit on the left, right or center, or be turned off.
*   **On/off switch:** turn the whole mod off for a fully vanilla HUD, and back on, without losing your settings.
*   **Live preview:** the config screen can show the HUD beside the options, drawn as your settings would, so you see each change without leaving the screen.
*   **Everything in game:** changes show on the HUD as you make them, no restart. Save Settings keeps them, and leaving without saving asks first.

## Compatibility

|                                              | 1.21.1 (NeoForge)                                                                                                                                                | 1.20.1 (Forge)                                                                                                                                |
| -------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- |
| <strong>Mod loader</strong>                  | NeoForge 21.1.1+                                                                                                                                                 | Forge 47.0.0+                                                                                                                                 |
| <strong>Iron's Spells 'n Spellbooks</strong> | 3.4.0+                                                                                                                                                           | 1.1.5+ (spell cost preview: 3.1.4+)                                                                                                           |
| <strong>AppleSkin</strong>                   | 3.0.4+                                                                                                                                                           | 2.5.0+                                                                                                                                        |
| <strong>Confluence: Otherworld</strong>      | 1.1.4+                                                                                                                                                           | 1.2.9+                                                                                                                                        |
| <strong>Double Hotbar</strong>               | Any (<a href="https://www.curseforge.com/projects/1563792" target="_blank" rel="nofollow">Double Hotbar NeoForge</a>, unofficial port by <em>retrospective</em>) | 1.3.2.1+ (<a href="https://www.curseforge.com/minecraft/mc-mods/double-hotbar" target="_blank" rel="nofollow">Double Hotbar</a> by sidezbros) |

*   **Client-side only:** servers don't need it, and it works on servers that don't have it.
*   **[Iron's Spells 'n Spellbooks](https://www.curseforge.com/projects/855414) by Iron431 and Lab3:** optional. When it's installed you get the mana bar and the spell cost preview, and Iron's own mana bar is hidden.
*   **[AppleSkin](https://www.curseforge.com/minecraft/mc-mods/appleskin) by squeek502:** optional. When it's installed, saturation is outlined in yellow on the hunger bar and held food's value flashes on it; with the vanilla hunger icons, AppleSkin's own overlays move with them.
*   **[Confluence: Otherworld](https://www.curseforge.com/minecraft/mc-mods/confluence):** optional. When it's installed its mana gets its own darker blue bar, below Iron's when both are installed; with Confluence's convertIronsSpellMana on (1.21.1 only: Confluence's Forge build doesn't use it), Iron's spells use Confluence's mana, so only that bar shows, with the spell cost preview. Confluence's own health and mana displays are hidden while this mod is on (in soul mode its soul display stays); with Armor or Hunger on Default, Confluence's own armor and hunger displays show, if they're on in its config. They move with the HUD, like vanilla's. On 1.20.1, Confluence needs a PortLib version that matches it; see Confluence's page.
*   **Double Hotbar:** compatible with [Double Hotbar](https://www.curseforge.com/minecraft/mc-mods/double-hotbar) by sidezbros on 1.20.1, and its unofficial NeoForge port, [Double Hotbar NeoForge](https://www.curseforge.com/projects/1563792) by \_retrospective\_, on 1.21.1. Open the **HUD Editor…** and drag the HUD up above the second hotbar once; the bars, XP bar and level move together and stay there, and the item name stays above them.
*   Works with other mods that draw near the hotbar the same way: drag the HUD, or just the pieces in the way, clear of them.
*   Air bubbles and mount health stay vanilla, stacked above hunger or below it.
*   **Texture packs:** armor and hunger on Default use your pack's icons, and the hotbar, XP bar, air bubbles and mount health stay vanilla, so packs restyle them. The health and mana bars, and armor and hunger as bars, are drawn by the mod and don't change with a pack.

#### Iron's Spells: what spell cost preview looks like

![Choosing a spell in Iron's spell wheel shows the mana it costs on the mana bar, then casting it uses that mana](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/spell-cast.gif)

## Settings

#### The config screen

![The config screen: options in sections on the right, and the preview of the HUD on the left](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/config.png)

#### Advanced mode: the width options open the HUD Editor

![The config screen in Advanced mode: the width options are Edit in HUD Editor buttons, and the preview shows the resized bars](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/advanced-settings.png)

#### Advanced mode: resizing bars in the HUD Editor

![The HUD Editor in Advanced mode: the health bar shortened from its right end, the mana bar from its left, and the XP bar made longer](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/resize-bars.png)

#### Moving the whole HUD

![Moving the whole HUD](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/move-hud.png)

#### Moving a single piece

![Moving one piece: the health bar](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/move-pieces.png)

#### Setting the item name height

![The Item Name page: a slider sets the height, and the held item's name is shown where it will go, here right above the hotbar](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/item-name.png)

#### Example layout: health and mana in the top-left

![Another layout: health and mana in the top-left corner, armor and hunger just above the XP bar](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/layout-top-left.png)

#### I will not judge you for your layouts even if it looks like this:

![A chaotic layout: the health, mana and XP bars stretched and scattered across the screen at odd widths](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/layout-chaos.png)
