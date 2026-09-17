# Backpack lantern lighting

Diamond and Netherite support optional LambDynamicLights 4.9.1 for Fabric 1.21.11.
Install the full LambDynamicLights mod on the client; its API alone does not illuminate the world.
Neither LambDynamicLights nor Trinkets is bundled or required for ordinary backpack storage.

Press **G** to toggle backpack lanterns. Rebind **Toggle Backpack Lantern** under
Options > Controls > Key Binds > Lime's Backpacks. There is no on-screen toggle message.
The server selects exactly one lantern backpack: the Trinkets chest/back slot first,
then the main hand, then the offhand. Stored bags and other players' bags are not targets.
Each backpack saves its own on/off state in its item custom data, defaulting to on.
The state follows the item through unequipping, dropping, storage, transfers and reloads,
and synchronizes to other players. Both client and server need the updated mod for toggling.
The previous global `backpack-lantern.properties` preference is no longer read.
Off removes that backpack's dynamic light and replaces its emissive flame windows with dark,
shaded glass. The appearance toggle also works without LambDynamicLights installed.
It does not toggle ordinary Minecraft lanterns or other backpacks.

- Held (either hand) and dropped backpacks use LambDynamicLights' standard item lighting, level 15.
- Worn backpacks in the Trinkets chest/back slot have one moving source per player.
- Rendered worn sources use the lantern center transformed by the same torso/model matrices.
  First-person and off-screen sources use a body-relative fallback that handles standing,
  crouching, swimming and gliding. Exact visual alignment still needs in-game review.
- Stored bags are not searched. A bag in an inventory or inside another container does not
  activate the equipped source. Lower-tier backpacks have no lantern light.
- Unequipping, death, spectator mode, leaving the client world, and player removal clean up
  the tracked source. Worn brightness follows the ordinary lantern's configured LDL luminance.
- Four small emissive window overlays keep the visible flame bright, with or without LDL.
  The lantern's iron frame, straps and backpack retain normal shading. No texture is replaced.
- This is client-side visual light, not server block light or mob-spawn protection.
  Lantern physics are not included.

## Build and checks

The API requires Loom 1.14.5; the project now uses Gradle 9.2.1. Java remains 21.

```
gradlew.bat build
gradlew.bat verifyLantern -PlanternWithApi
gradlew.bat verifyLantern -PlanternWithTrinkets
gradlew.bat verifyLantern -PwithDynamicLights
gradlew.bat verifyQuiver -PquiverWithTrinkets
```

The checks run world-free Fabric class loading and light/model regression tests; they are not
a full rendered-world playtest. Before release, test both hands, thrown items, both equipped tiers,
moving/sneaking/swimming, removing the bag, changing dimension, and another equipped player.
Confirm stored bags and lower tiers remain dark, and check with LDL absent as well as present.
The optional `withDynamicLights` build property adds the runtime for development only.
