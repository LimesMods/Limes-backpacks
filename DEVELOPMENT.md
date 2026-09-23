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

## PowerShell asset scripts

These Windows helper scripts are optional. Normal builds do not run them. Each one explains what it reads and writes at the top of the file, and none of them downloads code, runs external commands or changes system settings.

| Script | What it does | Writes files? |
|---|---|---|
| `scripts/check-expedition-models.ps1` | Validates the six backpack item models against their textures and the local Minecraft jar. | No, read-only |
| `scripts/generate-backpack-particles.ps1` | Regenerates the placed-backpack particle textures, block models and blockstate. | Yes, only generated assets in this repository |
| `scripts/generate-unlit-glass.ps1` | Regenerates the unlit lantern glass texture. | Yes, one PNG in this repository |

Run them from the repository root:

```text
powershell -ExecutionPolicy Bypass -File scripts/<script>.ps1
```

`-ExecutionPolicy Bypass` applies to that one run only and does not change your system's execution policy.
