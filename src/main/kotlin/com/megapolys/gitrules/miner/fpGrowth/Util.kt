package com.megapolys.gitrules.miner.fpGrowth

fun Map<String, Int>.getOrZero(fileName: String) = getOrDefault(fileName, 0)

fun Collection<String>.sortDescendingBySupport(supportMap: Map<String, Int>) =
    sortedWith(
        compareByDescending<String> { supportMap.getOrZero(it) }
            .thenBy { it }
    )