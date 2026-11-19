package com.granit.survival

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.sin

/** Locally synthesized quiet ambient music and lightweight gameplay sound effects. */
class GranitAudioEngine {
    @Volatile private var musicVolume = 0.28f
    @Volatile private var effectsVolume = 0.55f
    @Volatile private var running = false
    @Volatile private var moving = false
    private val pendingEffect = AtomicInteger(0)
    private var audioTrack: AudioTrack? = null
    private var audioThread: Thread? = null

    fun setVolumes(music: Float, effects: Float) {
        musicVolume = music.coerceIn(0f, 1f)
        effectsVolume = effects.coerceIn(0f, 1f)
    }

    fun setMoving(value: Boolean) { moving = value }
    fun playAction() { pendingEffect.set(1) }
    fun playFootstep() { pendingEffect.compareAndSet(0, 2) }

    @Synchronized
    fun start() {
        if (running) return
        val minBuffer = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuffer <= 0) return
        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(maxOf(minBuffer, CHUNK_SAMPLES * 4))
                .setTransferMode(AudioTrack.MODE_STREAM).build()
        }.getOrNull() ?: return
        audioTrack = track
        running = true
        audioThread = Thread({ renderAudio(track) }, "Granit-Audio").apply { isDaemon = true; start() }
    }

    @Synchronized
    fun stop() {
        running = false
        audioThread?.interrupt()
        runCatching { audioTrack?.pause() }
        runCatching { audioTrack?.flush() }
        runCatching { audioTrack?.stop() }
        runCatching { audioTrack?.release() }
        audioTrack = null
        audioThread = null
        moving = false
    }

    private fun renderAudio(track: AudioTrack) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val samples = ShortArray(CHUNK_SAMPLES)
        var cursor = 0L
        var effectRemaining = 0
        var effectPhase = 0.0
        var effectFrequency = 720.0
        var nextStep = 0L
        try {
            track.play()
            while (running) {
                when (pendingEffect.getAndSet(0)) {
                    1 -> { effectRemaining = SAMPLE_RATE / 12; effectFrequency = 740.0; effectPhase = 0.0 }
                    2 -> { effectRemaining = SAMPLE_RATE / 30; effectFrequency = 105.0; effectPhase = 0.0 }
                }
                if (moving && cursor >= nextStep) {
                    if (effectRemaining == 0) {
                        effectRemaining = SAMPLE_RATE / 30
                        effectFrequency = 105.0
                        effectPhase = 0.0
                    }
                    nextStep = cursor + SAMPLE_RATE * 2L / 5L
                }
                for (i in samples.indices) {
                    val time = cursor.toDouble() / SAMPLE_RATE
                    val root = AMBIENT_NOTES[(time / 8.0).toInt() % AMBIENT_NOTES.size]
                    val breathing = 0.72 + 0.28 * sin(2.0 * PI * time / 11.0)
                    val pad = (sin(2.0 * PI * root * time) * 0.52 +
                        sin(2.0 * PI * root * 1.5 * time) * 0.22 +
                        sin(2.0 * PI * root * 2.0 * time) * 0.12) * breathing * musicVolume * 0.10
                    var effect = 0.0
                    if (effectRemaining > 0) {
                        val envelope = (effectRemaining.toDouble() / (SAMPLE_RATE / 8.0)).coerceIn(0.0, 1.0)
                        effect = sin(effectPhase) * envelope * effectsVolume * 0.20
                        effectPhase += 2.0 * PI * effectFrequency / SAMPLE_RATE
                        effectRemaining--
                    }
                    samples[i] = ((pad + effect).coerceIn(-0.85, 0.85) * Short.MAX_VALUE).toInt().toShort()
                    cursor++
                }
                track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            }
        } catch (_: Throwable) {
            // Audio is optional: audio-device errors must never stop gameplay.
        }
    }

    companion object {
        private const val SAMPLE_RATE = 22_050
        private const val CHUNK_SAMPLES = 512
        private val AMBIENT_NOTES = doubleArrayOf(146.83, 174.61, 196.00, 130.81)
    }
}