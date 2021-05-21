package com.megapolys.gitrules.evaluation

import kotlin.Double.Companion.NaN
import kotlin.reflect.KProperty1

data class Result(
    val precision: Double,
    val fairPrecision: Double = NaN,
    val recall: Double = NaN,
    val fairRecall: Double = NaN,
    val fairCorrectInTop: Double = NaN
)

fun List<Result>.aggregate() =
    Result(
        precision = average(Result::precision),
        fairPrecision = filteredAverage(Result::fairPrecision),
        recall = average(Result::recall),
        fairRecall = filteredAverage(Result::fairRecall),
        fairCorrectInTop = filteredAverage(Result::fairCorrectInTop)
    )

private fun List<Result>.average(field: KProperty1<Result, Double>) =
    map(field)
        .average()

private fun List<Result>.filteredAverage(field: KProperty1<Result, Double>) =
    map(field)
        .filterNot(Double::isNaN)
        .average()
