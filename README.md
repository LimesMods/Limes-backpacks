# Lime's Backpacks

Lime's Backpacks adds six wearable expedition backpacks to Fabric 1.21.11. Each tier has its own capacity, materials, model details and equipment loadout.

## Features

- Wearable backpacks with persistent inventories.
- Leather, Copper, Iron, Golden, Diamond and Netherite tiers.
- Capacities of 9, 18, 27, 36, 54 and 81 slots.
- Optional Trinkets support for the chest/back slot. The mod also works without Trinkets.
- Optional LambDynamicLights support for the Diamond and Netherite lanterns.
- A Netherite quiver that supplies arrows to bows and crossbows.
- Toggleable lanterns with a rebindable **Toggle Backpack Lantern** key.
- Crouch right-click placement with hopper interaction, item preservation and tier-specific sounds.
- A dedicated advancement tab with one milestone for each crafted tier.

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.19.2 or newer
- Fabric API
- Java 21

Trinkets and LambDynamicLights are optional integrations. They are suggested by the mod and are not bundled.

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

The checked-in JSON model files are the runtime source of truth. `scripts/expedition-models.cjs` documents and generates the expedition model geometry; the preview server can inspect the models without launching Minecraft.

## Credits and license

The project code is released under the included [CC0 1.0 license](LICENSE).

Backpack geometry and texture details are adapted from [Hiking Backpack by Flok](https://sketchfab.com/3d-models/hiking-backpack-a49cffb713294180ae9005309922e969), licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). The asset was heavily modified for Lime's Backpacks, including redesigned tier geometry, materials, equipment, textures and Minecraft integration. See [HIKING_BACKPACK_CREDITS.md](HIKING_BACKPACK_CREDITS.md) for the complete attribution.

Final model renders are available in [`docs/showcase-final-details`](docs/showcase-final-details/).
