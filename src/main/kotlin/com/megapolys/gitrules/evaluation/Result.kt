package com.megapolys.gitrules.evaluation

import kotlin.reflect.KProperty1

data class Result(
    val precision: Double,
    val fairPrecision: Double,
    val recall: Double,
    val fairRecall: Double,
    val correctFirst: Double,
    val fairCorrectFirst: Double,
    val correctInTop: Double,
    val fairCorrectInTop: Double,
    val correctAll: Double,
    val fairCorrectAll: Double
)

fun List<Result>.aggregate() =
    Result(
        precision = average(Result::precision),
        fairPrecision = filteredAverage(Result::fairPrecision),
        recall = average(Result::recall),
        fairRecall = filteredAverage(Result::fairRecall),
        correctFirst = average(Result::correctFirst),
        fairCorrectFirst = filteredAverage(Result::fairCorrectFirst),
        correctInTop = average(Result::correctInTop),
        fairCorrectInTop = filteredAverage(Result::fairCorrectInTop),
        correctAll = average(Result::correctAll),
        fairCorrectAll = filteredAverage(Result::fairCorrectAll)
    )

private fun List<Result>.average(field: KProperty1<Result, Double>) =
    map(field)
        .average()

private fun List<Result>.filteredAverage(field: KProperty1<Result, Double>) =
    map(field)
        .filterNot(Double::isNaN)
        .average()
