// Generates 32x32 UV atlas PNG textures for each backpack tier.
// UV coords in model JSON remain 0-16; each UV unit = 2 pixels.
//
// Atlas layout (pixel coords):
//   [0,0,16,24]   body front/back   UV [0,0,8,12]
//   [16,0,24,24]  body east/west    UV [8,0,12,12]
//   [0,24,16,32]  body top/bottom   UV [0,12,8,16]
//   [24,0,32,ppH] pocket front      UV [12,0,16,pH]
//   [28,16,32,20] strap clip        UV [14,8,16,10]
//   [24,20,32,24] top handle        UV [12,10,16,12]
//   [16,24,32,30] sleeping bag front UV [8,12,16,15]
//   [24,12,28,20] lantern body      UV [12,6,14,10]
//   [28,12,32,16] lantern cap       UV [14,6,16,8]
//
// Body front column layout (all 24 rows):
//   cols 0-1   : dark outline edge
//   cols 2-5   : LEFT STRAP BAND  (UV x 1-3, sampled by left_band north face)
//   cols 6-9   : CENTER BODY      (warm flap rows 0-7, seam row 8, dark compartment rows 9-23)
//   cols 10-13 : RIGHT STRAP BAND (UV x 5-7, sampled by right_band north face)
//   cols 14-15 : dark outline edge
//
// NOTE: UV v=0 (texture top row) = high model Y = TOP of rendered element.
//       Texture top rows appear at the top of the rendered bag.
'use strict';
const zlib = require('zlib');
const fs   = require('fs');
const path = require('path');

const SZ = 32;

function makePng(pixels) {
    function crc32(buf) {
        const table = [];
        for (let n = 0; n < 256; n++) {
            let c = n;
            for (let k = 0; k < 8; k++) c = (c & 1) ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
            table[n] = c;
        }
        let crc = 0xffffffff;
        for (let i = 0; i < buf.length; i++) crc = table[(crc ^ buf[i]) & 0xff] ^ (crc >>> 8);
        return (crc ^ 0xffffffff) >>> 0;
    }
    function chunk(tag, data) {
        const t = Buffer.from(tag), len = Buffer.alloc(4);
        len.writeUInt32BE(data.length);
        const body = Buffer.concat([t, data]), crc = Buffer.alloc(4);
        crc.writeUInt32BE(crc32(body));
        return Buffer.concat([len, body, crc]);
    }
    const sig = Buffer.from([0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a]);
    const ihdrData = Buffer.alloc(13);
    ihdrData.writeUInt32BE(SZ,0); ihdrData.writeUInt32BE(SZ,4);
    ihdrData[8]=8; ihdrData[9]=6;
    const raw = [];
    for (let row = 0; row < SZ; row++) {
        raw.push(0);
        for (let col = 0; col < SZ; col++) {
            const [r,g,b,a] = pixels[row*SZ+col];
            raw.push(r,g,b,a);
        }
    }
    return Buffer.concat([sig, chunk('IHDR',ihdrData), chunk('IDAT',zlib.deflateSync(Buffer.from(raw))), chunk('IEND',Buffer.alloc(0))]);
}

const T     = [0,0,0,0];
const clamp = v => Math.max(0, Math.min(255, Math.round(v)));
const shade = ([r,g,b,a], f) => [clamp(r*f), clamp(g*f), clamp(b*f), a];
const mix   = ([r1,g1,b1], [r2,g2,b2], t) => [
    clamp(r1*(1-t)+r2*t), clamp(g1*(1-t)+g2*t), clamp(b1*(1-t)+b2*t), 255
];

// Deterministic per-pixel noise for material grain
const noise = (x, y, amp) => {
    const v = Math.sin(x * 127.1 + y * 311.7) * 43758.5453;
    return (v - Math.floor(v)) * amp - amp * 0.5;
};
const grain = ([r,g,b,a], x, y, amp) => [
    clamp(r + noise(x, y, amp)),
    clamp(g + noise(x+1, y, amp)),
    clamp(b + noise(x, y+1, amp)),
    a
];

function atlasPixels(cfg) {
    const { body, bodyHi, strap, side, top, outline, buckle,
            pocket, pocketHeight, clip, handle, sleepingBag, lantern } = cfg;

    const px = Array.from({length:SZ}, () => Array(SZ).fill(T));
    const fp = (x0,y0,x1,y1,c) => { for(let y=y0;y<y1;y++) for(let x=x0;x<x1;x++) px[y][x]=c; };

    // ── BODY FRONT/BACK  UV [0,0,8,12] → px [0,0,16,24] ──────────────────────
    // Texture row 0 = top of rendered bag. Row 0-7 = warm flap visible at top.
    // Row 8 = horizontal seam line. Row 9-23 = dark main compartment.
    // Cols 2-5 and 10-13 = strap bands (also sampled by the band elements in front).

    for (let r = 0; r < 24; r++) {
        for (let c = 0; c < 16; c++) {
            let base;
            const isStrap = (c >= 2 && c <= 5) || (c >= 10 && c <= 13);
            const isEdge  = c <= 1 || c >= 14;

            if (isEdge) {
                base = outline;
            } else if (isStrap) {
                // Strap bands: dark charcoal grey, subtle fabric grain
                base = grain(strap, c, r, 8);
            } else {
                // Center body between straps
                if (r < 8) {
                    // Top flap: warm material, slight vignette toward corners
                    base = grain(bodyHi, c, r, 10);
                } else if (r === 8) {
                    // Horizontal seam line between flap and compartment
                    base = shade(outline, 1.5);
                } else {
                    // Main compartment: very dark, subtle depth gradient darker at bottom
                    const depth = 1.0 - (r - 9) / 25.0;
                    base = grain(shade(body, 0.85 + depth * 0.20), c, r, 6);
                }
            }
            px[r][c] = base;
        }
    }

    // Subtle darkening at left/right edges of strap bands (gives rounded depth)
    for (let r = 0; r < 24; r++) {
        px[r][2] = shade(px[r][2], 0.72);
        px[r][5] = shade(px[r][5], 0.72);
        px[r][10] = shade(px[r][10], 0.72);
        px[r][13] = shade(px[r][13], 0.72);
    }

    // Strap buckle detail on straps at mid-height (rows 10-13, center of strap)
    // Left strap buckle area
    for (let c = 2; c <= 5; c++) {
        px[10][c] = shade(buckle, 0.75);
        px[11][c] = buckle;
        px[12][c] = buckle;
        px[13][c] = shade(buckle, 0.75);
    }
    // Right strap buckle area
    for (let c = 10; c <= 13; c++) {
        px[10][c] = shade(buckle, 0.75);
        px[11][c] = buckle;
        px[12][c] = buckle;
        px[13][c] = shade(buckle, 0.75);
    }

    // ── BODY EAST/WEST SIDES  UV [8,0,12,12] → px [16,0,24,24] ───────────────
    // Very dark side faces — gradient from slightly lighter at front edge to deeper at back
    for (let r = 0; r < 24; r++) {
        for (let c = 16; c < 24; c++) {
            const t = (c - 16) / 7.0;
            const sidePx = mix(shade(side, 0.88), shade(side, 0.38), t * 0.78);
            px[r][c] = grain(sidePx, c, r, 5);
        }
    }

    // ── BODY TOP  UV [0,12,8,16] → px [0,24,16,32] ────────────────────────────
    // Warm material showing at the very top edge of the bag. Also sampled by
    // sleeping bag up/down faces (which share this UV region).
    for (let r = 24; r < 32; r++) {
        for (let c = 0; c < 16; c++) {
            px[r][c] = grain(top, c, r, 10);
        }
    }
    // Subtle edge darkening on top face
    fp(0, 24, 16, 25, shade(top, 0.55));
    fp(0, 31, 16, 32, shade(top, 0.55));
    fp(0, 24,  2, 32, shade(top, 0.65));
    fp(14, 24, 16, 32, shade(top, 0.65));

    // ── POCKET  UV [12,0,16,pH] → px [24,0,32,pH*2] ───────────────────────────
    const ppH = pocketHeight * 2;
    for (let r = 0; r < ppH; r++) {
        for (let c = 24; c < 32; c++) {
            px[r][c] = grain(pocket, c, r, 8);
        }
    }
    // Pocket border stitching
    fp(24, 0, 32, 1, shade(outline, 1.4));
    if (ppH >= 2) fp(24, ppH-1, 32, ppH, shade(outline, 1.4));
    fp(24, 0, 25, ppH, shade(outline, 1.4));
    fp(31, 0, 32, ppH, shade(outline, 1.4));
    // Center stitching dots
    if (ppH >= 4) {
        const sr = Math.floor(ppH / 2);
        for (let c = 25; c < 31; c += 2) px[sr][c] = shade(outline, 2.2);
    }

    // ── STRAP CLIP  UV [14,8,16,10] → px [28,16,32,20]  (iron+) ──────────────
    if (clip) {
        fp(28, 16, 32, 20, clip);
        fp(28, 16, 32, 17, shade(clip, 1.35));   // top highlight
        fp(28, 19, 32, 20, shade(clip, 0.45));   // bottom shadow
        fp(28, 16, 29, 20, shade(clip, 0.55));   // left shadow
        fp(31, 16, 32, 20, shade(clip, 0.50));   // right shadow
    }

    // ── TOP HANDLE  UV [12,10,16,12] → px [24,20,32,24]  (gold+) ─────────────
    if (handle) {
        fp(24, 20, 32, 24, handle);
        fp(24, 20, 32, 21, shade(handle, 1.25));  // top highlight
        fp(24, 23, 32, 24, shade(handle, 0.55));  // bottom shadow
        fp(24, 20, 25, 24, shade(handle, 0.60));  // left edge
        fp(31, 20, 32, 24, shade(handle, 0.60));  // right edge
    }

    // ── SLEEPING BAG  UV [8,12,16,15] → px [16,24,32,30]  (diamond+) ──────────
    // 16px wide × 6px tall. Texture row 24 = top of sleeping bag element in render.
    // Design: light pink highlight at top → rich crimson → darker shadow at bottom.
    // Matches reference: cylindrical roll with highlight stripe across top.
    if (sleepingBag) {
        const sbBase    = sleepingBag;
        const sbLight   = shade(sbBase, 1.40);   // bright highlight row
        const sbLo      = shade(sbBase, 0.80);
        const sbShadow  = shade(sbBase, 0.55);
        const sbRim     = shade(sbBase, 0.32);   // dark rims

        // Row-by-row cylinder profile:
        fp(16, 24, 32, 25, shade(sbLight, 1.10));  // very top: near-white rim
        fp(16, 25, 32, 26, sbLight);               // bright highlight stripe
        fp(16, 26, 32, 27, sbBase);                // main color
        fp(16, 27, 32, 28, sbLo);                  // slight shadow
        fp(16, 28, 32, 29, sbShadow);              // deeper shadow
        fp(16, 29, 32, 30, sbRim);                 // dark bottom rim

        // Vertical binding straps every 5 columns (hold the roll)
        for (let c = 19; c < 32; c += 5) {
            for (let r = 24; r < 30; r++) {
                px[r][c] = shade(sbRim, 0.80);
            }
        }
        // Extra noise / fabric weave texture
        for (let r = 26; r < 29; r++) {
            for (let c = 16; c < 32; c++) {
                px[r][c] = grain(px[r][c], c*3, r*5, 12);
            }
        }
    }

    // ── LANTERN  UV [12,6,14,10]+[14,6,16,8] → px [24,12,28,20]+[28,12,32,16]
    if (lantern) {
        const iron      = [72, 68, 80, 255];
        const ironDark  = [26, 24, 32, 255];
        const ironLight = [105, 100, 115, 255];
        const glow      = lantern;
        const glowDim   = shade(lantern, 0.38);

        // Lantern body [24,12,28,20]: 4×8 px iron frame + amber glow window
        fp(24, 12, 28, 20, ironDark);          // dark fill
        fp(24, 12, 28, 13, iron);              // top horizontal bar
        fp(24, 19, 28, 20, iron);              // bottom horizontal bar
        fp(24, 12, 25, 20, iron);              // left vertical bar
        fp(27, 12, 28, 20, iron);              // right vertical bar
        fp(25, 13, 27, 19, glow);              // amber glow window
        // Bright center flicker
        px[15][25] = shade(glow, 1.30); px[15][26] = shade(glow, 1.30);
        px[16][25] = shade(glow, 1.30); px[16][26] = shade(glow, 1.30);
        // Corner rivets
        px[12][24] = ironLight; px[12][27] = ironLight;
        px[19][24] = ironLight; px[19][27] = ironLight;
        // Subtle outer glow bleed
        px[13][23] = glowDim; px[13][28] = glowDim;
        px[18][23] = glowDim; px[18][28] = glowDim;

        // Lantern top cap [28,12,32,16]: 4×4 px
        fp(28, 12, 32, 16, iron);
        fp(28, 12, 32, 13, ironDark);          // recessed shadow at top
        fp(29, 13, 31, 15, ironDark);          // vent slot
        fp(28, 15, 32, 16, ironLight);         // bottom edge highlight
    }

    return px.flat();
}

// ── Tier configurations ──────────────────────────────────────────────────────
// Colors extracted from reference images:
// - body / bodyHi: dark compartment / slightly lighter panel area
// - strap: dark grey charcoal (clearly visible against body)
// - side: very dark sides (near-black)
// - top: warm material on top face (seen at very top edge of bag)
// - outline: darkest edges / seams
// - buckle: metal hardware color
// - pocket: front pocket color (slightly different from body)
const TIERS = {
    leather: {
        // Reference image 1: warm medium-brown bag, dark grey straps, tan top
        body:       [  85,  52,  24, 255],  // dark warm brown compartment
        bodyHi:     [ 158, 108,  58, 255],  // lighter warm brown flap
        strap:      [  44,  40,  44, 255],  // dark charcoal grey straps
        side:       [  68,  40,  16, 255],  // dark side
        top:        [ 162, 122,  68, 255],  // warm tan top face
        outline:    [  20,  11,   4, 255],
        buckle:     [ 188, 152,  38, 255],  // warm brass
        pocket:     [ 118,  74,  34, 255],  // front pocket
        pocketHeight: 3,
        clip: null, handle: null, sleepingBag: null, lantern: null,
    },
    copper: {
        // Copper-orange tones, same structure as leather
        body:       [  78,  44,  14, 255],  // dark copper-brown
        bodyHi:     [ 175, 102,  44, 255],  // warm copper-orange flap
        strap:      [  46,  42,  44, 255],
        side:       [  62,  35,  10, 255],
        top:        [ 172, 108,  48, 255],  // warm copper top
        outline:    [  22,  11,   3, 255],
        buckle:     [ 205, 142,  52, 255],  // copper-brass
        pocket:     [ 112,  65,  22, 255],
        pocketHeight: 5,
        clip: null, handle: null, sleepingBag: null, lantern: null,
    },
    iron: {
        body:       [  42,  42,  46, 255],  // dark steel grey
        bodyHi:     [ 128, 128, 135, 255],  // lighter grey flap
        strap:      [  68,  68,  75, 255],  // grey straps (slightly lighter than body)
        side:       [  34,  34,  38, 255],
        top:        [ 138, 138, 146, 255],  // medium grey top
        outline:    [  14,  14,  17, 255],
        buckle:     [ 212, 212, 212, 255],  // silver
        pocket:     [  88,  88,  95, 255],
        pocketHeight: 6,
        clip:       [  82,  82,  90, 255],  // iron strap clip
        handle: null, sleepingBag: null, lantern: null,
    },
    gold: {
        body:       [  58,  44,   4, 255],  // very dark gold-brown compartment
        bodyHi:     [ 205, 168,  28, 255],  // bright gold flap
        strap:      [  48,  44,  42, 255],  // dark grey straps (contrast on gold)
        side:       [  48,  36,   3, 255],
        top:        [ 215, 175,  32, 255],  // brilliant gold top
        outline:    [  24,  18,   2, 255],
        buckle:     [ 252, 232, 105, 255],  // bright gold
        pocket:     [ 155, 118,  18, 255],
        pocketHeight: 6,
        clip:       [ 135, 102,  12, 255],
        handle:     [ 175, 138,  22, 255],
        sleepingBag: null, lantern: null,
    },
    diamond: {
        // Reference images 2-4 (dark body, crimson roll)
        body:       [   8,  38,  34, 255],  // near-black teal compartment
        bodyHi:     [  48, 185, 170, 255],  // bright teal flap
        strap:      [  42,  40,  50, 255],  // dark grey straps
        side:       [   6,  28,  25, 255],
        top:        [  55, 195, 180, 255],  // bright teal top
        outline:    [   3,  16,  14, 255],
        buckle:     [ 182, 248, 240, 255],  // aqua white
        pocket:     [  18,  85,  78, 255],
        pocketHeight: 6,
        clip:       [  22,  95,  88, 255],
        handle:     [  38, 152, 140, 255],
        // Bright crimson sleeping roll — exactly matching reference image highlight structure
        sleepingBag:[ 195,  32,  50, 255],
        lantern: null,
    },
    netherite: {
        // Reference image 4: near-black body, dark grey straps, dark crimson roll, amber lantern
        body:       [  16,  13,  19, 255],  // near-black
        bodyHi:     [  72,  64,  84, 255],  // dark purple-grey flap
        strap:      [  52,  50,  60, 255],  // slightly lighter grey straps (contrast on near-black)
        side:       [  12,  10,  15, 255],
        top:        [  68,  60,  80, 255],  // dark purple top
        outline:    [   6,   5,   8, 255],
        buckle:     [ 118, 108, 132, 255],  // muted purple-silver
        pocket:     [  38,  32,  48, 255],
        pocketHeight: 6,
        clip:       [  44,  40,  54, 255],
        handle:     [  52,  46,  62, 255],
        sleepingBag:[ 145,  22,  36, 255],  // dark blood-red roll
        lantern:    [ 215, 125,  32, 255],  // amber
    },
};

// ── Write PNGs ───────────────────────────────────────────────────────────────
const outDir = path.join(__dirname,'src','main','resources','assets','limesbackpacks','textures','item');
fs.mkdirSync(outDir, { recursive: true });

for (const [name, cfg] of Object.entries(TIERS)) {
    const outPath = path.join(outDir, `${name}_backpack.png`);
    fs.writeFileSync(outPath, makePng(atlasPixels(cfg)));
    console.log(`Written: ${outPath}`);
}
console.log('Done.');
