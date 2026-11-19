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
