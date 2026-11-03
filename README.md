# RPG Health Bar

> **Disclaimer:** this mod is fully vibe-coded, built with the Claude Code CLI to get more hands-on experience with it.

<img src="docs/images/icon.png" alt="RPG Health Bar icon" width="96" align="right">

A client-side Minecraft mod that replaces the vanilla hearts with an RPG-style health bar. With [Iron's Spells 'n Spellbooks](https://modrinth.com/mod/irons-spells-n-spellbooks) installed, a mana bar sits right below it.

This is the **1.20.1 (Forge)** branch. The 1.21.1 (NeoForge) version is on [`main`](https://github.com/joshua-fonseca/mc-rpg-health-bar/tree/main).

#### Default: vanilla armor and hunger icons

![The HUD with vanilla armor and hunger icons](docs/images/hud-vanilla-icons.png)

## Features

- **Health bar** the width of the hotbar, with your current and max health on it (`15.5/20`). It goes beyond the vanilla max of 20 for mods that add hearts to the player (`32/40`), following your real max health.
- **Absorption** fills the bar in gold, and the text shows it separately (`15.5+4/20`) or added in (`19.5/20`).
- **Mana bar support:** an overhaul of the mana bar from Iron's Spells 'n Spellbooks, in the same style as the health bar, replacing Iron's own.
- **Status colors:** like the vanilla hearts, the health bar turns green-yellow when poisoned, black when withered and light blue when frozen, and the hunger bar turns green with the Hunger effect. On 1.21.1, heart colors added by other mods are followed too.
- **Armor and hunger** as the vanilla icons (default), thin bars, or hidden.
- **AppleSkin support:** saturation is outlined in yellow on the hunger bar, and held food's value flashes on it, like AppleSkin's preview on the icons.
- <details>
  <summary><b>Highly customizable</b> (click to expand)</summary>

  - **Half-size bars:** health and mana can each be full or half width. With both half, they sit side by side above the hotbar.
  - **Move the HUD** by dragging it: the whole set, including the XP bar and level, or each piece (armor, hunger, health, mana, XP bar) on its own. It stays in place when you change the window size or GUI scale.
  - **Item name height:** set how high the held item's name sits, with a preview that moves as you change it. It starts at vanilla's height, which leaves room for mods that add hotbars.
  - **Air bubbles and mount health** can sit above hunger, like vanilla, or below it, for layouts with health or mana above hunger.
  - **Text position:** the numbers on the health and mana bars can sit on the left, right or center, or be turned off.
  - **On/off switch:** turn the whole mod off for a fully vanilla HUD, and back on, without losing your settings.
  - **Live preview:** the config screen can show the HUD beside the options, drawn as your settings would, so you see each change without leaving the screen.
  - **Everything in game:** changes show on the HUD as you make them, no restart. Save Settings keeps them, and leaving without saving asks first.

  </details>

#### Armor and hunger as bars, with absorption

![The HUD with armor and hunger as bars, and absorption](docs/images/hud-bars.png)

#### Status colors: poison and the Hunger effect

![Poisoned health bar and green hunger bar from the Hunger effect](docs/images/status-colors.png)

#### Half-size health and mana bars

![Half-size health and mana bars side by side above the hotbar](docs/images/half-bars.png)

#### AppleSkin: saturation and food preview

![With AppleSkin: saturation outlined in yellow on the hunger bar, and the hunger and saturation the held bread would restore flashing after it, then filling when it's eaten](docs/images/appleskin.gif)

#### Iron's Spells: choosing and casting a spell

![Choosing a spell in Iron's spell wheel shows the mana it costs on the mana bar, then casting it uses that mana](docs/images/spell-cast.gif)

## Settings

Open them from **Mods → RPG Health Bar → Config** (the Mods button is also in the pause menu). The options are grouped into sections, and the list scrolls if the window is small. Hover over any option to see what each value does. Changes show on the HUD right away: **Save Settings** keeps them. **Back to Mods** (or Esc) asks whether to save them if you changed anything: Yes keeps them, No undoes them.

| Option | Values |
|---|---|
| Mod State | **Enabled**, Disabled (a fully vanilla HUD; your settings are kept) |
| **Armor + Hunger** | |
| Armor | **Vanilla** icons, Bar, Hidden |
| Hunger | **Vanilla** icons, Bar, Hidden |
| Air Bubbles/Mount HP | **Above** hunger, Below it |
| **Health** | |
| Health Units | **HP** (20 for a full vanilla bar), Hearts (10) |
| Absorption | **Separate** (`16+4/20`), Combined (`20/20`) |
| HP Text | **Left**, Right, Center, Off |
| Health Bar Width | **Full**, Half |
| **Mana** | (only with Iron's Spells installed) |
| Mana Text | **Left**, Right, Center, Off |
| Mana Bar Width | **Full**, Half |
| **Positioning** | |
| Item Name... | Height of the held item's name: **Vanilla** (0), or pixels up (+) or down (-), down to right above the hotbar |

Defaults are in bold.

- **Preview (eye button beside the title):** shows the HUD on the left, drawn as your settings would at its default spot, so you can see each change without leaving the screen.
  - Off by default.
  - Needs a wide enough window.
- **Move HUD...** opens the position editor.
  - **Moving: All** drags the whole HUD; **Moving: One** drags a single piece. What you'd grab is highlighted.
  - The arrow keys nudge it (Shift: 10 pixels).
  - **Save** (or Esc) keeps the position; **Cancel** doesn't.
  - **Reset** puts everything back above the hotbar.
- **Item Name...** sets the held item's name height.
  - Use the slider or the arrow keys (Shift: 10 pixels).
  - A preview shows where the name will go as you change it, with the values for vanilla's height and right above the hotbar.
  - The name never goes onto the HUD or the hotbar.

#### The config screen

![The config screen: options in sections on the right, and the preview of the HUD on the left](docs/images/config.png)

#### Moving the whole HUD

![Moving the whole HUD](docs/images/move-hud.png)

#### Moving a single piece

![Moving one piece: the health bar](docs/images/move-pieces.png)

#### Setting the item name height

![The Item Name page: a slider sets the height, and the held item's name is shown where it will go, here right above the hotbar](docs/images/item-name.png)

#### Example layout: health and mana in the top-left

![Another layout: health and mana in the top-left corner, armor and hunger just above the XP bar](docs/images/layout-top-left.png)

Settings are stored in `config/rpghealthbar-client.toml`.

## Compatibility

| | 1.21.1 (NeoForge) | 1.20.1 (Forge) |
|---|---|---|
| **Mod loader** | NeoForge 21.1.1+ | Forge 47.0.0+ |
| **Iron's Spells 'n Spellbooks** | 3.9.1+ | 1.1.5+ (spell cost preview: 3.2.0+) |
| **AppleSkin** | 3.0.4+ | 2.5.0+ |
| **Double Hotbar** | Any ([Double Hotbar NeoForge](https://www.curseforge.com/projects/1563792), unofficial port by \_retrospective\_) | 1.3.2.1+ ([Double Hotbar](https://www.curseforge.com/minecraft/mc-mods/double-hotbar) by sidezbros) |

- Client-side only: servers don't need it, and it works on servers that don't have it.
- [Iron's Spells 'n Spellbooks](https://www.curseforge.com/projects/855414) by Iron431 and Lab3: optional. When it's installed you get the mana bar and the spell cost preview, and Iron's own mana bar is hidden.
- [AppleSkin](https://www.curseforge.com/minecraft/mc-mods/appleskin) by squeek502: optional. When it's installed, saturation is outlined in yellow on the hunger bar and held food's value flashes on it; with the vanilla hunger icons, AppleSkin's own overlays move with them.
- Double Hotbar: compatible with [Double Hotbar](https://www.curseforge.com/minecraft/mc-mods/double-hotbar) by sidezbros on 1.20.1, and its unofficial NeoForge port, [Double Hotbar NeoForge](https://www.curseforge.com/projects/1563792) by \_retrospective\_, on 1.21.1. Open **Move HUD...** and drag the HUD up above the second hotbar once; the bars, XP bar and level move together and stay there, and the item name stays above them.
- Works with other mods that draw near the hotbar the same way: drag the HUD, or just the pieces in the way, clear of them.
- Air bubbles and mount health stay vanilla and stack above the bars.
- **Texture packs:** armor and hunger on Vanilla use your pack's icons, and the hotbar, XP bar, air bubbles and mount health stay vanilla, so packs restyle them. The health and mana bars, and armor and hunger as bars, are drawn by the mod and don't change with a pack.

## Development

Requires Java 17. See [PLAN.md](PLAN.md) for the full design (written for 1.21.1; the differences on 1.20.1 are listed below).

```sh
./gradlew build      # builds the jar into build/libs/
./gradlew runClient  # launches a dev Minecraft client with the mod loaded
./gradlew runClient -PwithIronsSpells  # same, plus Iron's Spells and its dependencies
./gradlew runClient -PwithAppleSkin    # same, plus AppleSkin (the flags can be combined)
./gradlew runClient -Pforge_version=47.0.0  # run (or compile) on another Forge version
./gradlew runClient -PwithIronsSpells -PironsRuntime=io.redspace.ironsspellbooks:irons_spellbooks:1.20.1-1.1.5  # run another Iron's version
# Iron's 3.15 and newer moved to the io.redspace group and need Iron's Lib and newer dependencies:
./gradlew runClient -PwithIronsSpells -PwithIronsLib -PironsRuntime=io.redspace:irons_spellbooks:1.20.1-3.16.3 \
    -Pirons_lib_version=1.20.1-2.3.0 -Pcurios_version=5.14.1+1.20.1 -Pgeckolib_version=4.8.2
```

### Differences from the 1.21.1 version

- Forge 1.20.1 has no GUI layers: the mod hooks Forge's GUI overlays instead, and draws the XP bar itself (with vanilla's texture, so resource packs still apply) so the level can sit on the bar; vanilla draws both in one overlay.
- AppleSkin 2.5 draws its overlays inside the food overlay's events rather than in overlays of its own, so they're moved and hidden along with the food icons.
- Forge 1.20.1 has no event for other mods' heart types, so the health bar only follows the vanilla ones (poison, wither, freezing).
- Forge 1.20.1 writes a config file on every change and reloads it on another thread, which can undo a change made right after another. Saved positions are applied in memory at once and written one value per tick.

## License

[MIT](LICENSE)
