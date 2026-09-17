# Original pixel-art glass asset. No flame or emissive pixels; the existing
# lantern model retains its frame and uses this only on its four windows.
Add-Type -AssemblyName System.Drawing
$glass = [System.Drawing.Bitmap]::new(4, 8)
$palette = @('#303438', '#353A3D', '#292D30', '#222528')
for ($y=0; $y -lt 8; $y++) {
    for ($x=0; $x -lt 4; $x++) {
        $color = $palette[[Math]::Min(3, [int][Math]::Floor($y/2))]
        if ($x -eq 0 -and $y -lt 5) { $color = '#41484B' }
        if ($x -eq 2 -and $y -ge 5) { $color = '#1A1918' }
        $glass.SetPixel($x, $y, [System.Drawing.ColorTranslator]::FromHtml($color))
    }
}
try {
    $glass.Save((Join-Path $PSScriptRoot '../src/main/resources/assets/limesbackpacks/textures/item/lantern_unlit_glass.png'), [System.Drawing.Imaging.ImageFormat]::Png)
} finally { $glass.Dispose() }
