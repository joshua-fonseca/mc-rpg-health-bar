# mc-rpg-health-bar

A client-side NeoForge mod for Minecraft 1.21.1 that replaces the vanilla hearts, armor and hunger HUD with RPG-style bars, plus an optional mana bar for Iron's Spells 'n Spellbooks. See [PLAN.md](PLAN.md) for the full design.

## Development

Requires Java 21.

```sh
./gradlew build      # builds the jar into build/libs/
./gradlew runClient  # launches a dev Minecraft client with the mod loaded
./gradlew runClient -PwithIronsSpells  # same, plus Iron's Spells and its dependencies
```

Iron's Spells is an optional dependency. Without it installed, the mana bar is simply not shown.
