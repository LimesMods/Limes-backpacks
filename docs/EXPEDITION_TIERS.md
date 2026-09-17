# Expedition backpack visuals

All six tiers have their own geometry. They use 15 / 24 / 46 / 82 / 91 / 120 elements from leather through netherite. The original hiking model remains the common source for the main form, harness and pocket details; tier geometry overrides are intentional.

| Tier | Equipment |
|---|---|
| Leather | Compact brown pack with a clean front flap |
| Copper | Copper-brown canvas with a clean side pocket and metal fastening |
| Iron | Warm gray canvas, leather canteen cradle and stitched padded handle |
| Gold | Antique gold canvas, forest-green top bedroll |
| Diamond | Soft teal canvas, oatmeal-beige bedroll with sand lining, vanilla lantern and double flap piping |
| Netherite | Charcoal canvas, red top bedroll, reinforced flap corners and a leather quiver with wrapping harnesses |

## September 9 visual polish

Contact details: gold, diamond and netherite now have a very thin shadow strip beneath their sleeping bags, using the existing darker canvas swatch. Netherite's quiver has one small plate below the upper rim on its outward wall, sampling the darker shade of its original metal texture. The previous jar is saved as `build/backups/limesbackpacks-before-bedroll-shadow-quiver-accent.jar`. No preview images were generated.

Oatmeal fabric refinement: diamond's sleeping bag now uses warm oatmeal linen with soft sand shading, ivory highlights and slightly darker rolled ends. Only its fabric atlas reference changes: all model geometry, canteen placement, straps, clasps and other tiers remain unchanged. Prior white-and-cream texture and previews are retained. New renders: `docs/showcase-oatmeal`; prior jar: `build/backups/limesbackpacks-before-oatmeal-bedroll.jar`. Texture creation details: `docs/OATMEAL_TEXTURE.md`.

White-and-cream refinement: canteens now sit at the exact depth midpoint of the main rectangular body (excluding the outer pocket), roughly 0.26 model units away from the arm compared with the last midpoint placement. Heights and sizes are unchanged. Diamond now uses a separate white/cream fabric atlas: white outer cloth with cream lining on the bottom and exposed rolled ends, preserving dark straps and bright fittings. Gold's green and netherite's red roll textures are untouched. Previous jar: `build/backups/limesbackpacks-before-white-cream-bedroll.jar`. Fresh two-sided model renders are in `docs/showcase-white-cream`.

Canteen midpoint refinement: placed each canteen halfway between its preceding centered position and the latest armward position. Height, size, straps and buckles remain unchanged apart from the shared depth translation. Previous jar: `build/backups/limesbackpacks-before-midpoint-canteen.jar`.

Canteen direction correction: replaced the automatic side-panel centering below with the previous canteen origin plus an explicit 1.25 model-unit slide toward the wearer (positive Z, left toward the arm in the supplied side view). Applies to iron through netherite, including straps, mounts and buckle. Height, size and outward stand-off remain unchanged. All canteen bodies remain behind the player-facing harness plane. Previous jar: `build/backups/limesbackpacks-before-canteen-direction-correction.jar`.

Centered canteens: moved the complete canteen and carrier toward the wearer to the depth midpoint of the main side panel on iron, gold, diamond and netherite. Height, dimensions and outward stand-off are unchanged; straps and buckle follow the canteen. Prior jar: `build/backups/limesbackpacks-before-centered-canteen.jar`. No preview images generated.

Lowered sleeping bags: gold, diamond and netherite rolls now have their cloth bottom 0.25 model units above the large outer pocket's top. Closed straps, clasps, hems and rear anchors move with each roll. Netherite also shifts 0.15 units outward to clear the quiver's upper band. Accepted backpack thickness, all equipment sizes, colors and display transforms are unchanged. The previous jar is saved as `build/backups/limesbackpacks-before-lowered-bedroll.jar`. No preview images were generated.

Further slimming: reduced the canvas body and front pocket depth another 5% relative to the slimmer profile below, on every tier. Width, height, full-size equipment and display transforms remain unchanged. The prior jar is saved as `build/backups/limesbackpacks-before-extra-5-percent-slimming.jar`. No preview images were generated.

September 10 slimmer profile: canvas body and front pocket depth reduced by 10% / 12% / 14% / 17% / 20% / 23% from leather through netherite. Height, width, wearer scale and display transforms are unchanged. The player-facing body surface remains anchored; side pockets, canteen carriers and lantern mounts follow the slimmer shell. Sleeping bags and quivers retain their full dimensions and mutual clearance; bedroll anchors still meet the lid. The preceding build is saved as `build/backups/limesbackpacks-before-slimmer-profile.jar`. No preview images were generated.

September 10 follow-up: mirrored the left netherite flap guard so both uprights sit at the outside corners. Removed the dark upper cover tab from every canteen and both copper side-pocket corner buttons. Flask stoppers, lower carrier buckles, mounting straps, colors and wearer scale are unchanged. The prior jar is saved as `build/backups/limesbackpacks-before-corner-and-flask-cleanup.jar`. No preview images were generated.

September 10 cleanup: removed the leather tier's three decorative hand stitches, the two small pocket-flap crease marks from every tier, and the gold sleeping bag's maker label together with its two stitches. Continuous sewn edges, sleeping-bag hems, attachment straps and all other geometry are retained. The previous jar is preserved as `build/backups/limesbackpacks-before-decoration-removal.jar`. No preview images were generated.

Current worn size: every tier and all its attachments render uniformly 15% larger, raising the wearer scale from `1.176` to `1.3524`. The existing shoulder/back pivot is retained. Item display transforms and icon settings are unchanged. All six arrow-feather planes use the iron atlas's light neutral `(212,212,212)` swatch for a whiter appearance. The preceding jar and renderer source are saved under `build/backups/*before-15-percent-size*`.

Latest refinement: the lantern moved half a model unit back toward the pocket from the first clearance adjustment (center X is now `left - 1.1`), shortening the connecting arm. Broad pocket bands sample the original metal texture's shaded rims and bright center; tabs rotate those highlights vertically. Top faces use bright metal and side faces use its darker shade. This applies to front and side fastenings on every metal tier. Small fittings retain their brighter metal color, and the fuller canteen is unchanged. No preview images were made. The immediately preceding build is saved as `build/backups/limesbackpacks-before-fastening-texture.jar`.

Follow-up adjustments: tier-metal fittings again sample the original tier atlases at UV `[1,5.5,3,6.5]`. Both the band and central tab of each front/side pocket fastening use that same brighter metal color on copper through netherite. Cloth, sleeping bags, leather carrying straps and the vanilla lantern retain their colors; the leather tier is unchanged. Canisters are 25% thicker outward from the pack and 8% broader at the same height, with their carriers rebuilt to fit. On diamond and netherite, the lantern is one model unit farther outward along negative X (right in the user's rear view); its sewn mount now sits on the front pocket side directly behind the handle. No preview images were generated for this follow-up. The prior build and model sources are preserved in `build/backups/limesbackpacks-before-radiant-fittings.jar` and `build/backups/models-before-radiant-fittings.zip`.

The main compartment has two shallow shoulder steps and a slightly inset crown. Front-pocket corners are tucked in, with continuous UVs across the subdivided cloth pieces. The overlapping original pocket/flap surfaces have been separated to reduce flickering edges. Thin seams and bedroll hems give the fabric more definition.

The canteen has a continuous leather cradle, an open buckle frame and an upper loop. A sewn leather root joins the lantern hanger to the side of the front pocket. Two additional harness bands wrap around the quiver and meet the backpack. Small fittings and pocket fastenings use tier-metal colors; cloth seams and arrow feathers retain their muted tones. The vanilla lantern texture remains original.

Wearable rendering and all item display transforms are preserved. Only the lantern assembly's placement changes in the follow-up described above. Source textures are unchanged. `build/backups/models-before-visual-polish.zip` and `build/backups/limesbackpacks-before-visual-polish.jar` preserve the version before the initial polish.

Attachments are decorative. The lantern uses Minecraft 1.21.11's vanilla lantern template, scaled uniformly, with a byte-identical copy of its PNG and animation metadata at `limesbackpacks:item/vanilla_lantern`. Keeping all textures in the item atlas prevents Minecraft's mixed-atlas model bake error. Resource packs can override that item texture separately. It does not cast dynamic light. The canteen uses tan upper and dark-brown lower leather covers, a recessed dark waist, a small stopper (the decorative upper cover tab has been removed), based on the user's hiking reference. Its broad face faces outward from the main body's right side. There is no pocket behind the canteen or quiver. Bedroll straps form closed loops around all stepped corners and connect to the pack through rear anchors. The rolls rest on the lid; the upper map pocket and extra front utility pocket have been removed. Inventory capacity, recipes and the wearable renderer are unchanged. Fixed display scale remains 0.75 for every tier.

## Editing and checks

- `scripts/expedition-models.cjs` authors the model files and prints a JSON map of paths to contents. Apply the outputs as reviewed patches. The checked-in JSON files are the runtime source of truth.
- Run `./scripts/check-expedition-models.ps1` to verify item routing, valid bounds and UVs, texture references, every sampled texel's opacity, increasing main-compartment volume, and required netherite equipment.
- Run `node scripts/preview-server.cjs` and open `http://127.0.0.1:8766/docs/tier-preview.html` for a rotatable comparison using the actual model files and textures. This is an asset preview, not Minecraft's renderer.
- Build with `./gradlew.bat build`.
- Run `node scripts/check-attachment-joins.cjs` to check every loop connection, anchor and buckle, canteen material consistency and vanilla lantern face UVs. The texture check reads the vanilla client jar from the user's Gradle cache. Vanilla handle transparency is intentional. `docs/reference/lantern.png` is a local preview reference extracted from Minecraft, not bundled with the mod.

Visual review completed at front, side and both three-quarter angles. In-game animation, armor clipping and lighting still require a game test.

## Texture provenance

The original Flok hiking atlas is retained for harness details, and the project's pre-existing tier atlases supply fittings, straps and equipment accents. The additional cloth sheet is `src/main/resources/assets/limesbackpacks/textures/item/expedition_soft.png`, generated using the built-in image generation tool. Its twelve muted swatches provide six tier colors, yellow/blue/red sleeping bags, and three shared leather tones. The previous material sheet is retained unused for comparison. No original pixels were overwritten.

Generation prompt (built-in image generation tool):

Use case: stylized-concept. Asset type: Minecraft low-poly material texture atlas, not a model picture. Exactly 4 columns by 3 rows of equal rectangular swatches, covering entire image edge to edge without any margins or gaps. Row1 left to right: muted warm taupe leather brown #79604c; dusty copper brown #956e55; soft warm iron gray #858882; muted antique gold canvas #94815a. Row2: desaturated smoky teal #607f82; warm charcoal #49474b; mellow straw yellow sleeping bag #c2ad69; dusty denim blue sleeping bag #69869e. Row3: muted brick red sleeping bag #a25b59; saddle tan leather #997254; deep chocolate leather #514031; dark weathered leather strap #39322c. Each cell is almost flat color with ONLY 3 or 4 large crisp rectangular pixel color patches of tiny contrast, resembling simple soft low-poly hiking equipment. No noise, no fine grain, no tiny squares, no gradients, no lighting, no shadows, no borders, no text, no objects, fully opaque. Exact aligned 4x3 grid. This is a UV swatch sheet used on 3D cuboids; simple broad soft colors essential.
