package com.snoretracker.util

import kotlin.math.log10

object AudioUtils {
    fun calculateDecibels(maxAmplitude: Int): Float {
        if (maxAmplitude == 0) return 0f
        return (20 * log10(maxAmplitude.toDouble())).toFloat()
    }
}
