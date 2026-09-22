# PowerShell developer script: generates backpack particle and block-model assets.
# Safety notes:
# - It reads source assets from this repository and writes generated files only under this repository.
# - It does not download code, execute external commands, change system settings, or delete files.
# - Review the paths and any future edits before running an untrusted copy of this file.
#
# Generate fabric-only particle sprites from the main body's existing UV region.
Add-Type -AssemblyName System.Drawing
$assets = Join-Path $PSScriptRoot '../src/main/resources/assets/limesbackpacks'
$tiers = @('leather','copper','iron','gold','diamond','netherite')
$variants = [ordered]@{}
[System.IO.Directory]::CreateDirectory((Join-Path $assets 'textures/block')) | Out-Null
for ($tierIndex=0; $tierIndex -lt $tiers.Count; $tierIndex++) {
    $tier = $tiers[$tierIndex]
    $model = Get-Content (Join-Path $assets "models/item/${tier}_backpack_worn.json") -Raw | ConvertFrom-Json
    $body = $model.elements | Where-Object name -eq 'main_body' | Select-Object -First 1
    $uv = $body.faces.north.uv
    $textureName = $model.textures.($body.faces.north.texture.Substring(1)).Replace('limesbackpacks:', '')
    $atlas = [System.Drawing.Bitmap]::new((Join-Path $assets "textures/$textureName.png"))
    $sprite = [System.Drawing.Bitmap]::new(16,16)
    try {
        for ($y=0; $y -lt 16; $y++) {
            for ($x=0; $x -lt 16; $x++) {
                $u = $uv[0] + ($uv[2] - $uv[0]) * (($x + 0.5)/16)
                $v = $uv[1] + ($uv[3] - $uv[1]) * (($y + 0.5)/16)
                $sprite.SetPixel($x,$y,$atlas.GetPixel([int][Math]::Floor($u*$atlas.Width/16), [int][Math]::Floor($v*$atlas.Height/16)))
            }
        }
        $sprite.Save((Join-Path $assets "textures/block/${tier}_fabric.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    } finally { $atlas.Dispose(); $sprite.Dispose() }
    $blockModel = @{parent='minecraft:block/block';textures=@{particle="limesbackpacks:block/${tier}_fabric"};elements=@()}
    $blockModel | ConvertTo-Json -Depth 6 | Set-Content (Join-Path $assets "models/block/placed_backpack_${tier}.json")
    for ($r=0; $r -lt 8; $r++) {
        $variants["rotation=$r,tier=$tierIndex"] = @{model="limesbackpacks:block/placed_backpack_${tier}"}
    }
}
@{variants=$variants} | ConvertTo-Json -Depth 6 | Set-Content (Join-Path $assets 'blockstates/placed_backpack.json')
