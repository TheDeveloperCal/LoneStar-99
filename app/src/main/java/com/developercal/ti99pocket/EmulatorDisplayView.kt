// Copyright (C) 2026 DeveloperCal
// SPDX-License-Identifier: GPL-3.0-only

package com.developercal.ti99pocket

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioFocusRequest
import android.media.AudioTrack
import android.media.AudioManager
import android.os.Handler
import android.os.HandlerThread
import android.view.View
import java.lang.ref.WeakReference
import java.util.Collections

/** Pixel-accurate 4:3 machine output, rendered on a worker thread. */
class EmulatorDisplayView(context: Context) : View(context) {
    private val pixels = IntArray(WIDTH * HEIGHT)
    private val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
    private val paint = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val destination = Rect()
    private val worker = HandlerThread("TI-99 emulator").apply { start() }
    private val handler = Handler(worker.looper)
    @Volatile private var running = false
    @Volatile private var userPaused = false
    @Volatile private var muted = false
    @Volatile private var audioEnabled = true
    @Volatile private var volume = 1f
    @Volatile private var displayScale = 1f
    private var audioTrack: AudioTrack? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val focusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setOnAudioFocusChangeListener { change ->
                when {
                    change > 0 && running -> audioTrack?.play()
                    change < 0 -> audioTrack?.pause()
                }
            }
            .build()
    }
    private val audio = FloatArray(SAMPLES_PER_FRAME)

    private val frame = object : Runnable {
        override fun run() {
            if (!running) return
            val handle = NativeCore.handle
            if (NativeCore.nativeRunFrame(handle, pixels)) {
                synchronized(bitmap) { bitmap.setPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT) }
                if (audioEnabled) {
                    NativeCore.nativeFillAudio(handle, audio)
                    audioTrack?.let { track ->
                        if (muted) audio.fill(0f)
                        track.write(audio, 0, audio.size, AudioTrack.WRITE_BLOCKING)
                    }
                }
                postInvalidateOnAnimation()
            }
            if (running) handler.postDelayed(this, if (audioTrack == null || !audioEnabled) 16L else 1L)
        }
    }

    init {
        setBackgroundColor(Color.BLACK)
        runningViews.add(WeakReference(this))
    }

    fun resumeEmulation() {
        if (running || userPaused) return
        running = true
        handler.post {
            if (audioEnabled && audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                running = false
                return@post
            }
            if (audioEnabled) {
                if (audioTrack == null) audioTrack = createAudioTrack()
                audioTrack?.setVolume(volume)
                audioTrack?.play()
            }
            handler.post(frame)
        }
    }

    fun pauseEmulation() {
        running = false
        handler.removeCallbacks(frame)
        audioManager.abandonAudioFocusRequest(focusRequest)
        handler.post { audioTrack?.pause(); audioTrack?.flush() }
    }

    fun setUserPaused(paused: Boolean) {
        userPaused = paused
        if (paused) pauseEmulation() else resumeEmulation()
    }

    fun setMuted(value: Boolean) { muted = value }
    fun setAudioEnabled(value: Boolean) {
        audioEnabled = value
        handler.post {
            if (value && running) {
                if (audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    if (audioTrack == null) audioTrack = createAudioTrack()
                    audioTrack?.setVolume(volume)
                    audioTrack?.play()
                }
            } else {
                audioTrack?.pause()
                audioManager.abandonAudioFocusRequest(focusRequest)
            }
        }
    }
    fun setVolume(value: Float) { volume = value.coerceIn(0f, 1f); audioTrack?.setVolume(volume) }
    fun setDisplayScale(value: Float) { displayScale = value.coerceIn(0.65f, 1f); postInvalidateOnAnimation() }
    fun setPixelFiltering(enabled: Boolean) { paint.isFilterBitmap = enabled; postInvalidateOnAnimation() }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!userPaused) resumeEmulation()
    }

    override fun onDetachedFromWindow() {
        pauseEmulation()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)
        val scale = minOf(width.toFloat() / WIDTH, height.toFloat() / HEIGHT) * displayScale
        val w = (WIDTH * scale).toInt()
        val h = (HEIGHT * scale).toInt()
        val left = (width - w) / 2
        val top = (height - h) / 2
        destination.set(left, top, left + w, top + h)
        synchronized(bitmap) { canvas.drawBitmap(bitmap, null, destination, paint) }
    }

    companion object {
        private const val WIDTH = 256
        private const val HEIGHT = 192
        private const val SAMPLES_PER_FRAME = 800
        private val runningViews = Collections.synchronizedList(mutableListOf<WeakReference<EmulatorDisplayView>>())

        fun pauseAll() = each { it.pauseEmulation() }
        fun resumeVisibleViews() = each { if (!it.userPaused) it.resumeEmulation() }
        fun setUserPaused(paused: Boolean) = each { it.setUserPaused(paused) }
        fun setMuted(muted: Boolean) = each { it.setMuted(muted) }
        fun setAudioEnabled(enabled: Boolean) = each { it.setAudioEnabled(enabled) }
        fun setVolume(volume: Float) = each { it.setVolume(volume) }
        fun setDisplayScale(scale: Float) = each { it.setDisplayScale(scale) }
        fun setPixelFiltering(enabled: Boolean) = each { it.setPixelFiltering(enabled) }

        private inline fun each(action: (EmulatorDisplayView) -> Unit) {
            synchronized(runningViews) {
                val iterator = runningViews.iterator()
                while (iterator.hasNext()) {
                    val view = iterator.next().get()
                    if (view == null) iterator.remove() else action(view)
                }
            }
        }

        private fun createAudioTrack(): AudioTrack? = runCatching {
            val format = AudioFormat.Builder()
                .setSampleRate(48_000)
                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(format)
                .setBufferSizeInBytes(SAMPLES_PER_FRAME * Float.SIZE_BYTES * 4)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull()
    }
}
