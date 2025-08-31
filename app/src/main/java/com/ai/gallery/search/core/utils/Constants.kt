package com.ai.gallery.search.core.utils

import android.util.Log

// Convert embedding to/from FloatArray for easier manipulation
fun ByteArray.toFloatArray(): FloatArray {
    if (this.size % 4 != 0) {
        "ByteArray size ${this.size} is not divisible by 4".logIt()
        return FloatArray(0)
    }

    return FloatArray(this.size / 4) { i ->
        try {
            val bytes = this.sliceArray(i * 4 until (i + 1) * 4)
            java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).float
        } catch (e: Exception) {
            Log.w(
                "ByteArrayConversion",
                "Error converting bytes at index $i: ${e.message}"
            )
            0f
        }
    }
}

fun FloatArray.toByteArray(): ByteArray {
    val buffer = java.nio.ByteBuffer.allocate(this.size * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN)
    this.forEach { buffer.putFloat(it) }
    return buffer.array()
}

enum class SearchMode {
    EXACT,      // Only exact label matches
    PREFIX,     // Labels starting with query (hand -> handbag)
    FUZZY,      // Flexible matching with multiple strategies
    CONTAINS    // Contains anywhere (original behavior)
}

fun String.logIt(tag: String = "cvv") {
    Log.d(tag, "logIt: $this")
}