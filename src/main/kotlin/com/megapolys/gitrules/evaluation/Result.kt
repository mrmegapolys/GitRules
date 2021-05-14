package com.megapolys.gitrules.evaluation

data class Result(
    val precision: Double,
    val recall: Double,
    val fairPrecision: Double,
    val fairRecall: Double
)
