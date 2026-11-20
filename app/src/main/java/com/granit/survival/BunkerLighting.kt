package com.granit.survival

/** Stepped, symmetric pixel-art light pools for the bunker ceiling fixtures. */
internal object BunkerLighting {
    data class Layer(
        val size: Float,
        val red: Float,
        val green: Float,
        val blue: Float,
        val alpha: Float
    )

    val poweredLayers = listOf(
        Layer(4.8f, 0.72f, 0.48f, 0.25f, 0.035f),
        Layer(4.0f, 0.76f, 0.51f, 0.27f, 0.045f),
        Layer(3.2f, 0.80f, 0.55f, 0.29f, 0.055f),
        Layer(2.5f, 0.84f, 0.59f, 0.31f, 0.070f),
        Layer(1.8f, 0.88f, 0.63f, 0.33f, 0.085f),
        Layer(1.2f, 0.96f, 0.71f, 0.37f, 0.105f),
        Layer(0.65f, 1.00f, 0.78f, 0.47f, 0.12f)
    )

    val emergencyLayers = listOf(
        Layer(2.6f, 0.48f, 0.19f, 0.10f, 0.035f),
        Layer(1.8f, 0.62f, 0.25f, 0.12f, 0.055f),
        Layer(1.0f, 0.82f, 0.34f, 0.16f, 0.080f),
        Layer(0.42f, 0.96f, 0.48f, 0.22f, 0.10f)
    )
}
