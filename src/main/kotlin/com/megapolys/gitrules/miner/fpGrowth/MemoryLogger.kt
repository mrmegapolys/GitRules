package com.megapolys.gitrules.miner.fpGrowth

import java.lang.Runtime.getRuntime
import kotlin.math.max

object MemoryLogger {
    var maxMemory = 0.0

    fun reset() {
        maxMemory = 0.0
    }

    fun checkMemory() {
        val currentMemory = getRuntime()
            .run { totalMemory() - freeMemory() }
            .toDouble() / 1024 / 1024
        maxMemory = max(maxMemory, currentMemory)
    }
}