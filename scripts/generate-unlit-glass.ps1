# PowerShell developer script: generates the unlit lantern glass texture.
# Safety notes:
# - It overwrites one generated PNG in this repository:
#   src/main/resources/assets/limesbackpacks/textures/item/lantern_unlit_glass.png
# - Nothing outside this repository is touched.
# - It does not download code, execute external commands or change system settings.
# - Review the paths and any future edits before running an untrusted copy of this file.
#
# How to run (from the repository root):
#   powershell -ExecutionPolicy Bypass -File scripts/generate-unlit-glass.ps1
# It prints nothing on success. On failure it stops with an error.
# `-ExecutionPolicy Bypass` applies to this one run only; it does not change your system policy.
#
# PowerShell concepts used below: arrays hold the palette, nested `for` loops visit each pixel,
# and `try/finally` releases the bitmap even if drawing or saving fails.

# Stop on the first error so a failed run cannot leave a half-written texture unnoticed.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
# Build a 4x8 source image: four columns wide and eight rows high.
$glass = [System.Drawing.Bitmap]::new(4, 8)
try {
    # Four shades of dark glass, one per two-row band from top to bottom; there are deliberately no flame pixels.
    $palette = @('#303438', '#353A3D', '#292D30', '#222528')
    # Fill every pixel from the palette, then apply two small pixel-art highlight/shadow accents.
    for ($y=0; $y -lt 8; $y++) {
        for ($x=0; $x -lt 4; $x++) {
            # Two rows share one palette color, producing four horizontal bands.
            $color = $palette[[Math]::Min(3, [int][Math]::Floor($y/2))]
            # Add the left-edge highlight only to the upper part of the glass.
            if ($x -eq 0 -and $y -lt 5) { $color = '#41484B' }
            # Add a dark lower accent to suggest the glass edge.
            if ($x -eq 2 -and $y -ge 5) { $color = '#1A1918' }
            $glass.SetPixel($x, $y, [System.Drawing.ColorTranslator]::FromHtml($color))
        }
    }
    # Save next to the lantern assets; `$PSScriptRoot` makes the destination independent of the current directory.
    $glass.Save((Join-Path $PSScriptRoot '../src/main/resources/assets/limesbackpacks/textures/item/lantern_unlit_glass.png'), [System.Drawing.Imaging.ImageFormat]::Png)
} finally {
    # Release the native bitmap handle, including when an error is thrown.
    $glass.Dispose()
}
