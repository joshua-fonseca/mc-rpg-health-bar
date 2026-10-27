# RPG Health Bar

Replaces the vanilla hearts with an RPG-style health bar. With **Iron's Spells 'n Spellbooks** installed, a mana bar sits right below it.

#### Default: vanilla armor and hunger icons

![The HUD with vanilla armor and hunger icons](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/hud-vanilla-icons.png)

## Features

- **Health bar** the width of the hotbar, with your current and max health on it (`15.5/20`). It goes beyond the vanilla max of 20 for mods that add hearts to the player (`32/40`), following your real max health.
- **Absorption** fills the bar in gold, and the text shows it separately (`15.5+4/20`) or added in (`19.5/20`).
- **Mana bar support:** an overhaul of the mana bar from Iron's Spells 'n Spellbooks, in the same style as the health bar, replacing Iron's own.
- **Status colors:** like the vanilla hearts, the health bar turns green-yellow when poisoned, black when withered and light blue when frozen, and the hunger bar turns green with the Hunger effect. Heart colors added by other mods are followed too.
- **Armor and hunger** as the vanilla icons (default), thin bars, or hidden.
- **AppleSkin support:** saturation is outlined in yellow on the hunger bar, and held food's value flashes on it, like AppleSkin's preview on the icons.
- **Highly customizable:**
  - **Half-size bars:** health and mana can each be full or half width. With both half, they sit side by side above the hotbar.
  - **Move the HUD** by dragging it: the whole set, including the XP bar and level, or each piece (armor, hunger, health, mana, XP bar) on its own. It stays in place when you change the window size or GUI scale.
  - **Everything in game:** all options apply immediately, no restart.

#### Armor and hunger as bars, with absorption

![The HUD with armor and hunger as bars, and absorption](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/hud-bars.png)

#### Status colors: poison and the Hunger effect

![Poisoned health bar and green hunger bar from the Hunger effect](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/status-colors.png)

#### Half-size health and mana bars

![Half-size health and mana bars side by side above the hotbar](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/half-bars.png)

#### AppleSkin: saturation and food preview

![With AppleSkin: saturation outlined in yellow on the hunger bar, and the hunger and saturation the held bread would restore flashing after it, then filling when it's eaten](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/appleskin.gif)

## Compatibility

- **[Iron's Spells 'n Spellbooks](https://www.curseforge.com/projects/855414) by Iron431 and Lab3:** optional. When it's installed you get the mana bar and the spell cost preview, and Iron's own mana bar is hidden. Works with Iron's 3.9.1 or newer (every version for 1.21.1).
- **[AppleSkin](https://www.curseforge.com/minecraft/mc-mods/appleskin) by squeek502:** optional. When it's installed, saturation is outlined in yellow on the hunger bar and held food's value flashes on it; with the vanilla hunger icons, AppleSkin's own overlays move with them. Needs AppleSkin 3.0.4 or newer (3.0.2 and 3.0.3 don't run on 1.21.1).
- **[Double Hotbar](https://www.curseforge.com/projects/1563792) by sidezbros:** compatible. Open **Move HUD...** and drag the HUD up above the second hotbar once; the bars, XP bar and level move together and stay there, and the item name stays above them.
- Works with other mods that draw near the hotbar the same way: drag the HUD, or just the pieces in the way, clear of them.
- Air bubbles and mount health stay vanilla and stack above the bars.

#### Iron's Spells: choosing and casting a spell

![Choosing a spell in Iron's spell wheel shows the mana it costs on the mana bar, then casting it uses that mana](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/spell-cast.gif)

## Settings

Open them from **Mods → RPG Health Bar → Config** (the Mods button is also in the pause menu). Hover over any option to see what each value does.

| Option | Values |
|---|---|
| Armor | **Vanilla** icons, Bar, Hidden |
| Hunger | **Vanilla** icons, Bar, Hidden |
| Health Units | **HP** (20 for a full vanilla bar), Hearts (10) |
| Absorption | **Separate** (`16+4/20`), Combined (`20/20`) |
| HP Text | **On**, Off |
| Mana Text | **On**, Off |
| Health Bar | **Full**, Half width |
| Mana Bar | **Full**, Half width |

Defaults are in bold. **Move HUD...** opens the position editor. With **Moving: All** you drag the whole HUD; with **Moving: One** you drag a single piece. What you'd grab is highlighted. The arrow keys nudge it (Shift moves 10 pixels). Then Save. **Reset** in the editor, or **Reset Position** in the config, puts everything back above the hotbar.

#### The config screen

![The config screen](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/config.png)

#### Moving the whole HUD

![Moving the whole HUD](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/move-hud.png)

#### Moving a single piece

![Moving one piece: the health bar](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/move-pieces.png)

#### Example layout: health and mana in the top-left

![Another layout: health and mana in the top-left corner, armor and hunger just above the XP bar](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/layout-top-left.png)

## Requirements

- Minecraft 1.21.1 with NeoForge 21.1.1 or newer.
- **Client-side only:** servers don't need it, and it works on servers that don't have it.
- Newer Iron's Spells 'n Spellbooks versions need Iron's Lib, which requires NeoForge 21.1.200+.

Source code and issue tracker: [GitHub](https://github.com/joshua-fonseca/mc-rpg-health-bar)
