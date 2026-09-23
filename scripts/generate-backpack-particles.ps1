# PowerShell developer script: generates backpack particle and block-model assets.
# Safety notes:
# - It reads source assets from this repository and overwrites these generated files in it:
#   - textures/block/<tier>_fabric.png (six particle textures)
#   - models/block/placed_backpack_<tier>.json (six block models)
#   - blockstates/placed_backpack.json
#   All of them are under src/main/resources/assets/limesbackpacks. Nothing outside this repository is touched.
# - It does not download code, execute external commands or change system settings.
# - Review the paths and any future edits before running an untrusted copy of this file.
#
# How to run (from the repository root):
#   powershell -ExecutionPolicy Bypass -File scripts/generate-backpack-particles.ps1
# It prints nothing on success. On failure it stops with an error.
# `-ExecutionPolicy Bypass` applies to this one run only; it does not change your system policy.
#
# PowerShell concepts used below: `$name` stores a value, `@(...)` creates an array,
# `@{...}` creates a hashtable, and `try/finally` guarantees that image resources are released.

# Stop on the first error so a missing model or texture cannot produce broken generated files.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
# Resolve all generated paths relative to the script location, not the shell's current directory.
$assets = Join-Path $PSScriptRoot '../src/main/resources/assets/limesbackpacks'
# The array order is also the numeric tier order written into the blockstate variants.
$tiers = @('leather','copper','iron','gold','diamond','netherite')
# An ordered hashtable keeps blockstate variant output stable and easy to review in Git.
$variants = [ordered]@{}
# Create the destination directory if it does not exist; `Out-Null` hides the routine return value.
[System.IO.Directory]::CreateDirectory((Join-Path $assets 'textures/block')) | Out-Null
# Generate one particle texture and model reference for each backpack tier.
for ($tierIndex=0; $tierIndex -lt $tiers.Count; $tierIndex++) {
    $tier = $tiers[$tierIndex]
    # Read the worn item model, then select the main body and its north-face UV rectangle.
    $model = Get-Content (Join-Path $assets "models/item/${tier}_backpack_worn.json") -Raw | ConvertFrom-Json
    $body = $model.elements | Where-Object name -eq 'main_body' | Select-Object -First 1
    $uv = $body.faces.north.uv
    # Resolve the model's texture key to a repository PNG and open it as a source bitmap.
    $textureName = $model.textures.($body.faces.north.texture.Substring(1)).Replace('limesbackpacks:', '')
    $atlas = [System.Drawing.Bitmap]::new((Join-Path $assets "textures/$textureName.png"))
    # Particle sprites are always 16x16 pixels, matching Minecraft's standard texture size.
    $sprite = [System.Drawing.Bitmap]::new(16,16)
    try {
        # For each output pixel, sample the center of the corresponding point in the source UV rectangle.
        for ($y=0; $y -lt 16; $y++) {
            for ($x=0; $x -lt 16; $x++) {
                $u = $uv[0] + ($uv[2] - $uv[0]) * (($x + 0.5)/16)
                $v = $uv[1] + ($uv[3] - $uv[1]) * (($y + 0.5)/16)
                $sprite.SetPixel($x,$y,$atlas.GetPixel([int][Math]::Floor($u*$atlas.Width/16), [int][Math]::Floor($v*$atlas.Height/16)))
            }
        }
        # Save the sampled fabric image at the block texture path used by the generated model.
        $sprite.Save((Join-Path $assets "textures/block/${tier}_fabric.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        # Bitmap handles are native resources; always release both source and destination images.
        $atlas.Dispose(); $sprite.Dispose()
    }
    # The placed-backpack model only needs a particle texture; its actual geometry is rendered in Java.
    $blockModel = @{parent='minecraft:block/block';textures=@{particle="limesbackpacks:block/${tier}_fabric"};elements=@()}
    $blockModel | ConvertTo-Json -Depth 6 | Set-Content (Join-Path $assets "models/block/placed_backpack_${tier}.json")
    # The placed backpack block has eight rotations (see BackpackBlock.ROTATION). Store each rotation/tier combination in the blockstate map.
    for ($r=0; $r -lt 8; $r++) {
        $variants["rotation=$r,tier=$tierIndex"] = @{model="limesbackpacks:block/placed_backpack_${tier}"}
    }
}
# Write the complete blockstate table after all 48 rotation/tier combinations have been collected.
@{variants=$variants} | ConvertTo-Json -Depth 6 | Set-Content (Join-Path $assets 'blockstates/placed_backpack.json')
