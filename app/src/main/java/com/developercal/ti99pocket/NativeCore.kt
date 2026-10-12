// Copyright (C) 2026 DeveloperCal
// SPDX-License-Identifier: GPL-3.0-only

package com.developercal.ti99pocket

import kotlinx.coroutines.flow.MutableStateFlow

/** JNI boundary for the vendored, offline Libre99 machine core. */
internal object NativeCore {
    val appForeground = MutableStateFlow(true)

    init { System.loadLibrary("ti99pocket_core_android") }

    private val instance = lazy(LazyThreadSafetyMode.SYNCHRONIZED) { nativeCreate() }
    val handle: Long get() = instance.value
    fun releaseAllKeys() {
        if (instance.isInitialized()) (0..50).forEach { nativeSetKey(instance.value, it, false) }
    }

    external fun nativeCreate(): Long
    external fun nativeDestroy(handle: Long)
    external fun nativeRunFrame(handle: Long, pixels: IntArray): Boolean
    external fun nativeReset(handle: Long)
    external fun nativeSetKey(handle: Long, key: Int, down: Boolean)
    external fun nativeLoadCartridge(handle: Long, bytes: ByteArray): Boolean
    external fun nativeLoadDisk(handle: Long, drive: Int, bytes: ByteArray, key: String): Boolean
    external fun nativeEjectDisk(handle: Long, drive: Int): Boolean
    external fun nativeEjectCartridge(handle: Long): Boolean
    external fun nativeGetDiskImage(handle: Long, drive: Int): ByteArray?
    external fun nativeSaveState(handle: Long): ByteArray?
    external fun nativeLoadState(handle: Long, bytes: ByteArray): Boolean
    external fun nativeFillAudio(handle: Long, samples: FloatArray): Boolean
}
