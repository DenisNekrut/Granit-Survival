package com.granit.survival

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject

data class LoadedSpriteAtlas(
    val manifest: SpriteAtlasManifest,
    val bitmap: Bitmap
)

/** Decodes a versioned JSON atlas manifest and its PNG from the Android assets folder. */
class AndroidSpriteAtlasLoader(private val assets: AssetManager) {
    fun load(manifestPath: String): LoadedSpriteAtlas {
        require(manifestPath.isNotBlank() && !manifestPath.startsWith("/") &&
            !manifestPath.contains('\\\\') && manifestPath.split('/').none { it == ".." }) { "Invalid atlas manifest path" }

        val jsonText = assets.open(manifestPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(jsonText)
        val texture = root.getString("texture")
        val spriteArray = root.getJSONArray("sprites")
        val sprites = buildList {
            for (index in 0 until spriteArray.length()) {
                val item = spriteArray.getJSONObject(index)
                val frameArray = item.getJSONArray("frames")
                val frames = buildList {
                    for (frameIndex in 0 until frameArray.length()) {
                        val frame = frameArray.getJSONObject(frameIndex)
                        add(SpriteAtlasFrame(
                            x = frame.getInt("x"),
                            y = frame.getInt("y"),
                            width = frame.getInt("width"),
                            height = frame.getInt("height")
                        ))
                    }
                }
                add(SpriteAtlasEntry(
                    id = item.getString("id"),
                    frameWidth = item.getInt("frameWidth"),
                    frameHeight = item.getInt("frameHeight"),
                    frames = frames,
                    pivotX = item.optDouble("pivotX", 0.5).toFloat(),
                    pivotY = item.optDouble("pivotY", 1.0).toFloat()
                ))
            }
        }
        val manifest = SpriteAtlasManifest(
            version = root.getInt("version"),
            texture = texture,
            textureWidth = root.getInt("textureWidth"),
            textureHeight = root.getInt("textureHeight"),
            filter = root.optString("filter", "nearest"),
            sprites = sprites
        )
        val errors = SpriteAtlasManifestValidator.validate(manifest)
        require(errors.isEmpty()) { "Invalid sprite atlas manifest: ${errors.joinToString()}" }

        val bitmap = assets.open(manifest.texture).use { BitmapFactory.decodeStream(it) }
            ?: error("Unable to decode sprite atlas texture: ${manifest.texture}")
        require(bitmap.width == manifest.textureWidth && bitmap.height == manifest.textureHeight) {
            "Texture size ${bitmap.width}x${bitmap.height} does not match manifest " +
                "${manifest.textureWidth}x${manifest.textureHeight}"
        }
        return LoadedSpriteAtlas(manifest, bitmap)
    }
}
