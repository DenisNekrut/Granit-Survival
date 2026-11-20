# Granit Survival — rules for graphical resources

## Source asset standard
- Authored pixel-art sprites: lossless PNG, 8-bit RGBA, transparent background.
- Pixel-art textures use nearest-neighbour filtering. Linear filtering is allowed only for separately approved non-pixel illustrations.
- Source and atlas color space: sRGB; alpha: straight/unassociated.
- Sprite frame dimensions and scale factors use integer pixels; no fractional resampling.
- File names and sprite IDs use stable, unique lower_snake_case identifiers.
- Keep editable masters in art/source/ and runtime atlas pages/manifests in app/src/main/assets/sprites/.
- Do not commit temporary exports, editor caches, previews or duplicate versions.

## Atlas manifest v1
The runtime manifest is app/src/main/assets/sprites/atlas-manifest.json. The texture path is relative to the Android assets root. Frame coordinates are integer pixels with a top-left origin. pivotX/pivotY are normalized to 0..1; the default anchor is bottom-centre (0.5, 1.0). The filter is nearest or linear.

Every sprite has a unique id, positive frameWidth/frameHeight and one or more frames of identical dimensions. Frames must remain within the PNG bounds. External atlas pages must be PNG RGBA8888 and actual dimensions must match textureWidth/textureHeight.

Leave at least one transparent pixel between independent regions and use edge extrusion when packing scaled atlas pages to prevent texture bleeding. Manifest v1 does not encode padding/extrusion or trim/source-size metadata; do not trim frames until a versioned manifest extension and loader migration are implemented.

## Validation
Run python3 tools/validate_sprite_assets.py. It checks manifest structure, IDs, paths, dimensions, pivots, frame bounds and PNG IHDR. Example manifests are checked structurally but do not require their demonstration texture. The code-generated atlas is additionally checked by PixelSpriteAtlas.validate() and Kotlin unit tests.

The external atlas is currently an optional override. If absent, the game generates a built-in atlas from PixelSpriteAtlas. This is a fallback, not a completed external art-import pipeline.


## 2026-11-20 — expanded pixel-art direction
- World sprites use a shared 1.18 scale multiplier; collision tiles, map dimensions and gameplay coordinates remain unchanged.
- The built-in atlas and fallback renderer share one cold-weather color grade: deeper slate shadows, clearer midtones and restrained warm highlights.
- Repeated environmental props use deterministic shape/material variants. Variant selection is coordinate-based and stable across visits and save restoration.
- Every variant remains a pixel sprite with nearest-neighbour rendering. No SVG or vector runtime assets are introduced.
- Variant families currently cover crates, floor grates, trees, bushes, grass, fallen logs, barrels, rubble, debris, metal scraps, puddles, streetlamps, road signs and wrecks.
- Further hand-authored redraws of the player, bunker furniture and large landmark sprites remain part of the visual pass; the present change establishes consistent scaling, grading and repeatable prop variation.


## Android launcher icon
- Original pixel art built around the Granit Survival bunker hatch, with the cold slate palette and restrained warm lamp highlight.
- The 48x48 authored pixel master is stored as compact indexed-color data in `app/src/main/assets/launcher-icon.txt`.
- The Android Gradle build generates density-specific PNG resources (mdpi through xxxhdpi) using nearest-neighbour scaling, plus adaptive and round launcher resource definitions.
- The icon is registered through `android:icon` and `android:roundIcon` in the application manifest. No external image/vector runtime dependency is required.

## Infected character sprites
- Three distinct 12x24 pixel-art silhouettes are authored for the shambler, runner and brute archetypes.
- Each archetype has two frames; the renderer selects its matching sprite and alternates frames during movement/attack cooldown updates.
- The infected remain non-graphic, stylized game characters and share the atlas palette/nearest-neighbour pipeline.
