# Changelog

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
