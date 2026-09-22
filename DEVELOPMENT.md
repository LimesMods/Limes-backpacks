# Lime's Backpacks Development

Build and verification instructions for contributors working on Lime's Backpacks.

## Build

```text
gradlew.bat build
```

The build runs the GUI, placement, sound and lantern verification checks. Quiver verification is available separately. Useful focused checks are:

```text
gradlew.bat verifyLantern -PlanternWithApi
gradlew.bat verifyLantern -PlanternWithTrinkets
gradlew.bat verifyQuiver
gradlew.bat verifyQuiver -PquiverWithTrinkets
node scripts/check-attachment-joins.cjs
```

The checked-in JSON model files are the runtime source of truth. `scripts/expedition-models.cjs` documents and generates the expedition model geometry.
