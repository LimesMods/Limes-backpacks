# PowerShell developer script: reads local mod assets and the local Minecraft client jar to validate models.
# Safety notes:
# - This script is read-only: it does not write files, download code, run commands, or change system settings.
# - It reads the repository and the Minecraft jar in the current user's Gradle cache.
# - Review the paths and any future edits before running an untrusted copy of this file.
#
# How to run (from the repository root, after `gradlew.bat build` has downloaded Minecraft):
#   powershell -ExecutionPolicy Bypass -File scripts/check-expedition-models.ps1
# On success it prints one "<tier> PASS" line per backpack tier. On failure it stops with an error.
# `-ExecutionPolicy Bypass` applies to this one run only; it does not change your system policy.
#
# PowerShell concepts used below: `$name` stores a value, `@(...)` creates an array,
# `@{...}` creates a hashtable, `|` passes one command's output to the next command,
# and `try/finally` guarantees that opened image and archive resources are released.

# Stop on the first error so a failed validation cannot be reported as a successful run.
$ErrorActionPreference = 'Stop'
# Load the .NET image and ZIP APIs used to inspect PNG files and the vanilla client jar.
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem
# Open the local, read-only Minecraft client jar for vanilla textures used by the models.
$vanillaJar = [IO.Compression.ZipFile]::OpenRead((Join-Path $env:USERPROFILE '.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-clientonly-deobf/26.2/minecraft-clientonly-deobf-26.2.jar'))
# `$PSScriptRoot` is the directory containing this script, so these paths work from any current directory.
$assets = Join-Path $PSScriptRoot '../src/main/resources/assets/limesbackpacks'
# Some backpack textures come from Minecraft's armor atlas; map atlas sprite names to texture resources.
$armorSources = @{}
$atlas = Get-Content -Raw (Join-Path $assets '../minecraft/atlases/items.json') | ConvertFrom-Json
foreach ($source in $atlas.sources) { $armorSources[$source.sprite] = $source.resource }
# Cache decoded bitmaps and already-checked texture/UV rectangles to avoid repeated work.
$bitmaps = @{}
$checked = @{}
# Each tier must add geometry and increase the main backpack body volume compared with the previous tier.
$previousVolume = 0
$previousCount = 0
try {
    # Validate the tiers in increasing capacity/detail order.
    foreach ($tier in @('leather','copper','iron','gold','diamond','netherite')) {
        # Read the item model and item definition as JSON objects.
        $model = Get-Content -Raw (Join-Path $assets "models/item/${tier}_backpack.json") | ConvertFrom-Json
        $definition = Get-Content -Raw (Join-Path $assets "items/${tier}_backpack.json") | ConvertFrom-Json
        # The item definition must route to the matching worn model rather than an unrelated model.
        if ($definition.model.model -ne "limesbackpacks:item/${tier}_backpack_worn") { throw 'Wrong item routing' }
        # Comparing element counts catches a tier that accidentally lost visible detail.
        if ($model.elements.Count -le $previousCount) { throw 'Tier does not add detail' }
        $previousCount = $model.elements.Count
        # Calculate the axis-aligned volume of the named main body cuboid.
        $body = $model.elements | Where-Object name -eq 'main_body'
        $volume = 1
        foreach ($axis in 0..2) { $volume *= $body.to[$axis]-$body.from[$axis] }
        if ($volume -le $previousVolume) { throw 'Tier does not increase volume' }
        $previousVolume = $volume
        # Check every model element's cuboid bounds and every face's texture coordinates.
        foreach ($element in $model.elements) {
            foreach ($axis in 0..2) {
                # Thin lantern glow/handle planes are intentional; all other elements need positive thickness.
                $plane = $element.name -in @('vanilla_lantern_2','vanilla_lantern_3') -or $element.name.StartsWith('lantern_glow_')
                if (($element.from[$axis] -gt $element.to[$axis]) -or (($element.from[$axis] -eq $element.to[$axis]) -and -not $plane) -or $element.from[$axis] -lt -16 -or $element.to[$axis] -gt 32) { throw "Invalid bounds: $tier/$($element.name)" }
            }
            foreach ($face in $element.faces.PSObject.Properties) {
                # Minecraft model UV coordinates are expected to stay within one 16x16 texture frame.
                $uv = $face.Value.uv
                foreach ($value in $uv) { if ($value -lt 0 -or $value -gt 16) { throw "Invalid UV: $tier/$($element.name)" } }
                # Resolve the face's `#textureKey` against the model texture table.
                $key = $face.Value.texture.TrimStart('#')
                $texture = $model.textures.$key
                if (-not $texture) { throw "Unresolved texture $key" }
                if (-not $bitmaps.ContainsKey($texture)) {
                    # Prefer the vanilla jar for Minecraft/armor-atlas textures; otherwise read the mod texture.
                    if ($texture.StartsWith('minecraft:') -or $armorSources.ContainsKey($texture)) {
                        $resource = if ($armorSources.ContainsKey($texture)) { $armorSources[$texture] } else { $texture }
                        # ZIP entries are streams, so clone the bitmap before closing the stream.
                        $stream=$vanillaJar.GetEntry('assets/minecraft/textures/'+$resource.Split(':')[1]+'.png').Open()
                        $sourceBitmap=[System.Drawing.Bitmap]::new($stream)
                        $bitmaps[$texture]=[System.Drawing.Bitmap]::new($sourceBitmap)
                        $sourceBitmap.Dispose();$stream.Dispose()
                    } else {
                        # Mod texture identifiers are converted to a path under the repository's texture directory.
                        $file = Join-Path $assets ('textures/' + $texture.Split(':')[1] + '.png')
                        $bitmaps[$texture] = [System.Drawing.Bitmap]::new($file)
                    }
                }
                $bitmap = $bitmaps[$texture]
                # Lantern textures are animated vertical strips whose frame height equals their width;
                # ordinary textures use their full bitmap height for UV-to-pixel conversion.
                $frameHeight = if ($texture -in @('minecraft:block/lantern','limesbackpacks:item/vanilla_lantern')) { $bitmap.Width } else { $bitmap.Height }
                # Vanilla handle uses intentional cutout pixels on two rotated planes.
                if ($plane) { continue }
                # The same texture rectangle can appear on many faces, so validate each rectangle once.
                $rectangle = $texture + ':' + ($uv -join ',')
                if (-not $checked.ContainsKey($rectangle)) {
                    # Convert normalized 0..16 UV coordinates to integer pixel ranges and require opaque pixels.
                    for ($y=[int][Math]::Floor($uv[1]*$frameHeight/16); $y -lt [Math]::Ceiling($uv[3]*$frameHeight/16); $y++) {
                        for ($x=[int][Math]::Floor($uv[0]*$bitmap.Width/16); $x -lt [Math]::Ceiling($uv[2]*$bitmap.Width/16); $x++) {
                            if ($bitmap.GetPixel($x,$y).A -ne 255) { throw "Transparent texel: $tier/$($element.name)/$($face.Name) $texture ${x},${y}" }
                        }
                    }
                    $checked[$rectangle] = $true
                }
            }
        }
        if ($tier -eq 'netherite') {
            # Netherite carries the extra expedition equipment and must retain these named elements.
            foreach ($required in @('flask_body','sleeping_bag_core','vanilla_lantern_0','quiver_base','arrow_shaft_0')) {
                if (-not ($model.elements | Where-Object name -eq $required)) { throw "Missing $required" }
            }
        }
        # Report a compact summary only after the entire tier passes validation.
        Write-Output "$tier PASS: $($model.elements.Count) elements; main volume $([Math]::Round($volume,1)); all faces sample opaque pixels."
    }
} finally {
    # Dispose every decoded bitmap and the jar even when a validation error stops the script.
    foreach ($bitmap in $bitmaps.Values) { $bitmap.Dispose() }
    $vanillaJar.Dispose()
}
