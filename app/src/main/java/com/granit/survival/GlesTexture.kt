package com.granit.survival

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils

/**
 * Owns one GLES2 texture uploaded from a decoded Android Bitmap.
 *
 * Every method must be called on the EGL render thread while its context is current.
 * Recreate this object and upload again after EGL context loss.
 */
class GlesTexture {
    var textureId: Int = 0
        private set
    var width: Int = 0
        private set
    var height: Int = 0
        private set

    val isUploaded: Boolean get() = textureId != 0

    fun upload(bitmap: Bitmap, filter: String = "nearest") {
        check(textureId == 0) { "Texture is already uploaded; release it before replacing" }
        require(!bitmap.isRecycled) { "Cannot upload a recycled bitmap" }
        require(filter == "nearest" || filter == "linear") { "Unsupported texture filter: $filter" }

        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        check(ids[0] != 0) { "Unable to allocate GLES texture" }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
        val sampling = if (filter == "nearest") GLES20.GL_NEAREST else GLES20.GL_LINEAR
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, sampling)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, sampling)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        try {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            val error = GLES20.glGetError()
            check(error == GLES20.GL_NO_ERROR) { "Texture upload failed with GLES error 0x${error.toString(16)}" }
            textureId = ids[0]
            width = bitmap.width
            height = bitmap.height
        } catch (failure: Throwable) {
            GLES20.glDeleteTextures(1, ids, 0)
            throw failure
        } finally {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }
    }

    fun bind(textureUnit: Int = 0) {
        check(isUploaded) { "Texture has not been uploaded" }
        require(textureUnit >= 0) { "Texture unit must not be negative" }
        val maxUnits = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, maxUnits, 0)
        require(textureUnit < maxUnits[0]) { "Texture unit $textureUnit exceeds device limit ${maxUnits[0]}" }
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureUnit)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
    }

    fun release() {
        if (textureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            textureId = 0
            width = 0
            height = 0
        }
    }
}
