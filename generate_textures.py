#!/usr/bin/env python3
"""Generates 16x16 UV atlas PNG textures for the 3D backpack model."""
import struct
import zlib
import os

def make_png(pixels):
    """Create a 16x16 RGBA PNG from a flat list of (r,g,b,a) tuples."""
    def chunk(tag, data):
        c = tag + data
        return struct.pack('>I', len(data)) + c + struct.pack('>I', zlib.crc32(c) & 0xffffffff)

    sig = b'\x89PNG\r\n\x1a\n'
    ihdr_data = struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0)
    ihdr = chunk(b'IHDR', ihdr_data)

    raw = bytearray()
    for row in range(16):
        raw.append(0)  # filter type None
        for col in range(16):
            r, g, b, a = pixels[row * 16 + col]
            raw += bytes([r, g, b, a])

    idat = chunk(b'IDAT', zlib.compress(bytes(raw)))
    iend = chunk(b'IEND', b'')
    return sig + ihdr + idat + iend

T = (0, 0, 0, 0)  # transparent


def atlas_pixels(body, side, top, strap, outline, buckle):
    """Build 16x16 UV atlas matching backpack_3d.json UV regions:
      x=0-7,   y=0-11:  Body north/south (front/back face)
      x=8-11,  y=0-11:  Body east/west (side faces)
      x=0-7,   y=12-15: Body up/down (top/bottom faces)
      x=12-15, y=0-3:   Pocket north (front face)
      x=8,     y=0-3:   Pocket east/west (1px side strip)
      x=0-3,   y=12:    Pocket up/down (1px top/bot strip)
    """
    b  = body
    sd = side
    tp = top
    s  = strap
    o  = outline
    k  = buckle

    px = [[T] * 16 for _ in range(16)]

    def fill(x0, y0, x1, y1, color):
        for y in range(y0, y1):
            for x in range(x0, x1):
                px[y][x] = color

    # --- Body front/back face (x=0..7, y=0..11) ---
    fill(0, 0,  8,  1,  o)   # top border
    fill(0, 11, 8,  12, o)   # bottom border
    fill(0, 0,  1,  12, o)   # left border
    fill(7, 0,  8,  12, o)   # right border
    fill(1, 1,  7,  3,  s)   # top strap strip
    fill(1, 3,  7,  4,  o)   # line separating strap from body
    fill(1, 4,  7,  11, b)   # main body
    px[6][3] = k              # left buckle dot
    px[6][4] = k              # right buckle dot

    # --- Body side faces (x=8..11, y=0..11) ---
    fill(8,  0,  12, 1,  o)  # top border
    fill(8,  11, 12, 12, o)  # bottom border
    fill(8,  0,  9,  12, o)  # left border (also serves as pocket side texture)
    fill(11, 0,  12, 12, o)  # right border
    fill(9,  1,  11, 11, sd) # inner side panel (darker than front)

    # --- Body top/bottom face (x=0..7, y=12..15) ---
    fill(0, 12, 8, 13, o)    # top border (also serves as pocket top/bot texture)
    fill(0, 15, 8, 16, o)    # bottom border
    fill(0, 12, 1, 16, o)    # left border
    fill(7, 12, 8, 16, o)    # right border
    fill(1, 13, 7, 15, tp)   # inner top/bottom

    # --- Pocket front face (x=12..15, y=0..3) ---
    fill(12, 0, 16, 1, o)    # top border
    fill(12, 3, 16, 4, o)    # bottom border
    fill(12, 0, 13, 4, o)    # left border
    fill(15, 0, 16, 4, o)    # right border
    fill(13, 1, 15, 3, b)    # pocket face (same color as body front)
    px[1][14] = k             # small buckle on pocket

    return [px[row][col] for row in range(16) for col in range(16)]


TIERS = {
    "leather":   dict(
        body=(160, 100, 50, 255),
        side=(110, 65, 28, 255),
        top=(130, 82, 38, 255),
        strap=(120, 72, 32, 255),
        outline=(55, 30, 10, 255),
        buckle=(200, 170, 50, 255),
    ),
    "copper":    dict(
        body=(196, 127, 75, 255),
        side=(148, 88, 42, 255),
        top=(170, 106, 58, 255),
        strap=(155, 95, 50, 255),
        outline=(78, 42, 14, 255),
        buckle=(230, 160, 80, 255),
    ),
    "iron":      dict(
        body=(200, 200, 200, 255),
        side=(148, 148, 148, 255),
        top=(172, 172, 172, 255),
        strap=(168, 168, 168, 255),
        outline=(78, 78, 78, 255),
        buckle=(240, 240, 220, 255),
    ),
    "gold":      dict(
        body=(255, 215, 0, 255),
        side=(195, 155, 0, 255),
        top=(225, 185, 0, 255),
        strap=(215, 175, 0, 255),
        outline=(115, 85, 0, 255),
        buckle=(255, 255, 180, 255),
    ),
    "diamond":   dict(
        body=(80, 230, 215, 255),
        side=(45, 178, 164, 255),
        top=(62, 205, 190, 255),
        strap=(52, 190, 175, 255),
        outline=(0, 95, 85, 255),
        buckle=(210, 255, 250, 255),
    ),
    "netherite": dict(
        body=(75, 70, 82, 255),
        side=(48, 44, 54, 255),
        top=(60, 56, 66, 255),
        strap=(54, 50, 60, 255),
        outline=(20, 18, 24, 255),
        buckle=(130, 120, 140, 255),
    ),
}

out_dir = os.path.join(
    os.path.dirname(__file__),
    "src", "main", "resources", "assets", "limesbackpacks", "textures", "item"
)
os.makedirs(out_dir, exist_ok=True)

for name, colors in TIERS.items():
    pixels = atlas_pixels(**colors)
    png_bytes = make_png(pixels)
    path = os.path.join(out_dir, f"{name}_backpack.png")
    with open(path, "wb") as f:
        f.write(png_bytes)
    print(f"Written: {path}")

print("Done.")
