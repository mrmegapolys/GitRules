package com.megapolys.gitrules.evaluation.strategies

import com.megapolys.gitrules.evaluation.Result
import com.megapolys.gitrules.miner.Commit
import com.megapolys.gitrules.server.Rule
import com.megapolys.gitrules.server.RulesService
import kotlin.Double.Companion.NaN

class SourceCodeNavigationEvaluation(
    rulesService: RulesService
) : Evaluation(rulesService) {
    override fun runQueries(commit: Commit) =
        commit.files
            .map { currentFile ->
                val expected = commit.files - currentFile
                val actual = rulesService
                    .generateRules(listOf(currentFile), 7, 0.0)
                    .map(Rule::toSet)
                val intersectionSize = actual
                    .intersect(expected)
                    .size

                val actualFraction = actual.calculateFraction(intersectionSize)
                val expectedFraction = expected.calculateFraction(intersectionSize)

                val hasCorrectInFirst = hasIntersection(actual.take(1), expected)
                val hasCorrectInTop = hasIntersection(actual.take(3), expected)
                val hasCorrectInAll = hasIntersection(actual, expected)

                Result(
                    precision = actualFraction ?: 1.0,
                    fairPrecision = actualFraction ?: NaN,
                    recall = expectedFraction ?: 1.0,
                    fairRecall = expectedFraction ?: NaN,
                    correctFirst = hasCorrectInFirst ?: 1.0,
                    fairCorrectFirst = hasCorrectInFirst ?: NaN,
                    correctInTop = hasCorrectInTop ?: 1.0,
                    fairCorrectInTop = hasCorrectInTop ?: NaN,
                    correctAll = hasCorrectInAll ?: 1.0,
                    fairCorrectAll = hasCorrectInAll ?: NaN
                )
            }
}

private fun List<String>.calculateFraction(intersectionSize: Int) =
    takeIf(List<String>::isNotEmpty)
        ?.run { intersectionSize.toDouble() / size }

private fun hasIntersection(actual: List<String>, expected: List<String>) =
    actual
        .takeIf(List<String>::isNotEmpty)
        ?.intersect(expected)
        ?.run { if (isNotEmpty()) 1.0 else 0.0 }