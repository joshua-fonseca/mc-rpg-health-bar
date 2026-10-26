# RPG Health Bar

Replaces the vanilla hearts with an RPG-style health bar, drawn in the style of the XP bar. With **Iron's Spells 'n Spellbooks** installed, a mana bar sits right below it.

![The HUD with vanilla armor and hunger icons](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/hud-vanilla-icons.png)

## Features

- **Health bar** the width of the hotbar, with your current and max health on it (`15.5/20`). Max health follows your real max, so health boosts are shown.
- **Absorption** fills the bar in gold, and the text shows it separately (`15.5+4/20`) or added in (`19.5/20`).
- **Mana bar** for Iron's Spells 'n Spellbooks, replacing Iron's own.
- **Spell cost preview:** while Iron's spell wheel is open, the mana the hovered spell would use blinks at the end of the mana bar.
- **Damage trail:** health or mana you just lost stays as a darker section for a moment, then shrinks away.
- **Armor and hunger** as the vanilla icons (default), thin bars, or hidden.
- **Move the whole HUD**, including the XP bar and level, by dragging it. The position holds up when you change the window size or GUI scale.
- **Everything in game:** all options apply immediately, no restart.

![The HUD with armor and hunger as bars, and absorption](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/hud-bars.png)

## Compatibility

- **[Iron's Spells 'n Spellbooks](https://www.curseforge.com/projects/855414) by Iron431 and Lab3:** optional. When it's installed you get the mana bar and the spell cost preview, and Iron's own mana bar is hidden. Tested with Iron's 3.16.2.
- **[Double Hotbar](https://www.curseforge.com/projects/1563792) by sidezbros:** compatible. Open **Move HUD...** and drag the HUD up above the second hotbar once; the bars, XP bar and level move together and stay there.
- Works with other mods that draw near the hotbar the same way: drag the HUD clear of them.
- Air bubbles and mount health stay vanilla and stack above the bars.

![The mana a hovered spell would use, shown on the mana bar](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/spell-cost.png)

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

Defaults are in bold. **Move HUD...** opens the position editor: drag the HUD (or use the arrow keys; Shift moves 10 pixels), then Save. **Reset Position** puts it back above the hotbar.

![The config screen](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/config.png)

![Moving the HUD](https://raw.githubusercontent.com/joshua-fonseca/mc-rpg-health-bar/main/docs/images/move-hud.png)

## Requirements

- Minecraft 1.21.1 with NeoForge 21.1.1 or newer.
- **Client-side only:** servers don't need it, and it works on servers that don't have it.
- Iron's Spells 'n Spellbooks needs a newer NeoForge for its own libraries (Iron's Lib requires 21.1.200+).

Source code and issue tracker: [GitHub](https://github.com/joshua-fonseca/mc-rpg-health-bar)
