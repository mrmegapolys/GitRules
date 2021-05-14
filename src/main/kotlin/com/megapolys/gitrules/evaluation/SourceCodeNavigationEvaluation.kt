package com.megapolys.gitrules.evaluation

import com.megapolys.gitrules.miner.Commit
import com.megapolys.gitrules.server.Rule
import com.megapolys.gitrules.server.RulesService
import kotlin.Double.Companion.NaN

class SourceCodeNavigationEvaluation(
    rulesService: RulesService
) : EvaluationStrategy(rulesService) {
    override fun runQueries(commit: Commit) =
        commit.files
            .map { currentFile ->
                val expected = commit.files - currentFile
                val actual = rulesService
                    .generateRules(listOf(currentFile), 7)
                    .map(Rule::toSet)
                val intersectionSize = actual
                    .intersect(expected)
                    .size
                    .toDouble()

                val expectedSize = expected.size
                val actualSize = actual.size

                Result(
                    precision = if (actualSize != 0) intersectionSize / actualSize else 1.0,
                    fairPrecision = if (actualSize != 0) intersectionSize / actualSize else NaN,
                    recall = if (expectedSize != 0) intersectionSize / expectedSize else 1.0,
                    fairRecall = if (expectedSize != 0) intersectionSize / expectedSize else NaN
                )
            }
}