# PowerShell developer script: reads local mod assets and the local Minecraft client jar to validate models.
# Safety notes:
# - This script is read-only: it does not write files, download code, run commands, or change system settings.
# - It reads the repository and the Minecraft jar in the current user's Gradle cache.
# - Review the paths and any future edits before running an untrusted copy of this file.

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem
$vanillaJar = [IO.Compression.ZipFile]::OpenRead((Join-Path $env:USERPROFILE '.gradle/caches/fabric-loom/1.21.11/minecraft-client.jar'))
$assets = Join-Path $PSScriptRoot '../src/main/resources/assets/limesbackpacks'
$armorSources = @{}
$atlas = Get-Content -Raw (Join-Path $assets '../minecraft/atlases/items.json') | ConvertFrom-Json
foreach ($source in $atlas.sources) { $armorSources[$source.sprite] = $source.resource }
$bitmaps = @{}
$checked = @{}
$previousVolume = 0
$previousCount = 0
try {
 foreach ($tier in @('leather','copper','iron','gold','diamond','netherite')) {
  $model = Get-Content -Raw (Join-Path $assets "models/item/${tier}_backpack.json") | ConvertFrom-Json
  $definition = Get-Content -Raw (Join-Path $assets "items/${tier}_backpack.json") | ConvertFrom-Json
  if ($definition.model.model -ne "limesbackpacks:item/${tier}_backpack_worn") { throw 'Wrong item routing' }
  if ($model.elements.Count -le $previousCount) { throw 'Tier does not add detail' }
  $previousCount = $model.elements.Count
  $body = $model.elements | Where-Object name -eq 'main_body'
  $volume = 1
  foreach ($axis in 0..2) { $volume *= $body.to[$axis]-$body.from[$axis] }
  if ($volume -le $previousVolume) { throw 'Tier does not increase volume' }
  $previousVolume = $volume
  foreach ($element in $model.elements) {
   foreach ($axis in 0..2) {
    $plane = $element.name -in @('vanilla_lantern_2','vanilla_lantern_3') -or $element.name.StartsWith('lantern_glow_')
    if (($element.from[$axis] -gt $element.to[$axis]) -or (($element.from[$axis] -eq $element.to[$axis]) -and -not $plane) -or $element.from[$axis] -lt -16 -or $element.to[$axis] -gt 32) { throw "Invalid bounds: $tier/$($element.name)" }
   }
   foreach ($face in $element.faces.PSObject.Properties) {
    $uv = $face.Value.uv
    foreach ($value in $uv) { if ($value -lt 0 -or $value -gt 16) { throw "Invalid UV: $tier/$($element.name)" } }
    $key = $face.Value.texture.TrimStart('#')
    $texture = $model.textures.$key
    if (-not $texture) { throw "Unresolved texture $key" }
    if (-not $bitmaps.ContainsKey($texture)) {
     if ($texture.StartsWith('minecraft:') -or $armorSources.ContainsKey($texture)) {
      $resource = if ($armorSources.ContainsKey($texture)) { $armorSources[$texture] } else { $texture }
      $stream=$vanillaJar.GetEntry('assets/minecraft/textures/'+$resource.Split(':')[1]+'.png').Open()
      $sourceBitmap=[System.Drawing.Bitmap]::new($stream)
      $bitmaps[$texture]=[System.Drawing.Bitmap]::new($sourceBitmap)
      $sourceBitmap.Dispose();$stream.Dispose()
     } else {
      $file = Join-Path $assets ('textures/' + $texture.Split(':')[1] + '.png')
      $bitmaps[$texture] = [System.Drawing.Bitmap]::new($file)
     }
    }
    $bitmap = $bitmaps[$texture]
    $frameHeight = if ($texture -in @('minecraft:block/lantern','limesbackpacks:item/vanilla_lantern')) { $bitmap.Width } else { $bitmap.Height }
    # Vanilla handle uses intentional cutout pixels on two rotated planes.
    if ($plane) { continue }
    $rectangle = $texture + ':' + ($uv -join ',')
    if (-not $checked.ContainsKey($rectangle)) {
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
   foreach ($required in @('flask_body','sleeping_bag_core','vanilla_lantern_0','quiver_base','arrow_shaft_0')) {
    if (-not ($model.elements | Where-Object name -eq $required)) { throw "Missing $required" }
   }
  }
  Write-Output "$tier PASS: $($model.elements.Count) elements; main volume $([Math]::Round($volume,1)); all faces sample opaque pixels."
 }
} finally { foreach ($bitmap in $bitmaps.Values) { $bitmap.Dispose() }; $vanillaJar.Dispose() }
