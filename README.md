# Lime's Backpacks

Lime's Backpacks adds six wearable backpacks to Fabric 1.21.11. Each tier has its own capacity, materials, model details and equipment loadout.

## Features

- Wearable backpacks with persistent inventories.
- Leather, Copper, Iron, Golden, Diamond and Netherite tiers.
- Capacities of 9, 18, 27, 36, 54 and 81 slots.
- Optional Trinkets support for the chest/back slot. The mod also works without Trinkets.
- Optional LambDynamicLights support for the Diamond and Netherite lanterns.
- A quiver on the Netherite backpack that supplies arrows to bows and crossbows.
- Toggleable lanterns with a rebindable **Toggle Backpack Lantern** key.
- Crouch right-click placement with hopper interaction, item preservation and tier-specific sounds.
- A dedicated advancement tab with one milestone for each crafted tier.

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.19.2 or newer
- Fabric API
- Java 21

Trinkets Updated and LambDynamicLights are optional integrations. They are suggested by the mod and are not bundled, so the backpack storage and ordinary lantern features work without either one.

### Optional integrations and third-party licenses

- [Trinkets Updated](https://modrinth.com/mod/trinkets-updated) is used for the wearable chest/back slot integration. It is licensed under the [MIT License](https://www.curseforge.com/minecraft/mc-mods/trinkets-updated/license).
- [LambDynamicLights](https://modrinth.com/mod/lambdynamiclights) is used for dynamic lantern lighting on the Diamond and Netherite backpacks. It is licensed under the [Lambda License](https://github.com/LambdAurora/LambDynamicLights/blob/1.21.5/LICENSE).

Lime's Backpacks links to these public APIs but does not include either mod's jar, code, or assets. Install them separately when you want the corresponding integrations.

## Controls

- **B** opens and closes the equipped backpack.
- **Toggle Backpack Lantern** is available under Options > Controls > Lime's Backpacks and defaults to **G**.
- Crouch right-click with a backpack to place it. Crouch right-click a placed backpack to pick it up.

## Development

```text
gradlew.bat build
```

The build runs the GUI, placement, sound, quiver and lantern verification checks. Useful focused checks are:

```text
gradlew.bat verifyLantern -PlanternWithApi
gradlew.bat verifyLantern -PlanternWithTrinkets
gradlew.bat verifyQuiver -PquiverWithTrinkets
node scripts/check-attachment-joins.cjs
```

The checked-in JSON model files are the runtime source of truth. `scripts/expedition-models.cjs` documents and generates the expedition model geometry.

## Credits and licensing

Original Lime's Backpacks code and original project material are covered by the included [LIMESMODS LICENSE](LICENSE). You may use and privately modify that material for personal, non-commercial use. Unmodified copies may also be included in private or public, non-commercial modpacks and run on public, non-commercial servers through normal modpack platforms and launchers. You may record, stream, review, showcase, and publish videos featuring Lime's Backpacks, including monetized videos, provided that proper credit and a link to the official Modrinth or GitHub page are included. Standalone redistribution, public modifications, copying into other projects, and commercial use of the mod itself require written permission from LimesMods.

The repository is public for source visibility and development. Public visibility does not grant permission to redistribute the mod or reuse its original code and assets.

Backpack geometry and texture details are adapted from [Hiking Backpack by Flok](https://sketchfab.com/3d-models/hiking-backpack-a49cffb713294180ae9005309922e969), licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). That third-party material remains under its original license and is not relicensed by the Lime's Backpacks license. The asset was heavily modified for Lime's Backpacks, including redesigned tier geometry, materials, equipment, textures and Minecraft integration. See [HIKING_BACKPACK_CREDITS.md](HIKING_BACKPACK_CREDITS.md) for the complete attribution.

Optional integrations are separate projects with their own licenses: [Trinkets Updated](https://modrinth.com/mod/trinkets-updated) uses the [MIT License](https://www.curseforge.com/minecraft/mc-mods/trinkets-updated/license), and [LambDynamicLights](https://modrinth.com/mod/lambdynamiclights) uses the [Lambda License](https://github.com/LambdAurora/LambDynamicLights/blob/1.21.5/LICENSE). Neither is bundled with Lime's Backpacks.
