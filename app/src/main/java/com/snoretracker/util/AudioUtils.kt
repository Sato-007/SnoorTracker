package com.snoretracker.util

import kotlin.math.log10

object AudioUtils {
    fun calculateDecibels(maxAmplitude: Int): Float {
        if (maxAmplitude == 0) return 0f
        return (20 * log10(maxAmplitude.toDouble())).toFloat()
    }

    fun calculateZeroCrossingRate(buffer: ShortArray, size: Int): Float {
        if (size <= 1) return 0f
        var crossings = 0
        for (i in 1 until size) {
            val prev = buffer[i - 1].toInt()
            val curr = buffer[i].toInt()
            if ((curr >= 0 && prev < 0) || (curr < 0 && prev >= 0)) {
                crossings++
            }
        }
        return crossings.toFloat() / (size - 1)
    }
}
