package com.megapolys.gitrules.evaluation.strategies

import com.megapolys.gitrules.evaluation.Result
import com.megapolys.gitrules.miner.Commit
import com.megapolys.gitrules.server.Rule
import com.megapolys.gitrules.server.RulesService
import kotlin.Double.Companion.NaN

class ErrorPreventionEvaluation(
    rulesService: RulesService,
    private val minConfidence: Double
) : Evaluation(rulesService) {
    override fun runQueries(commit: Commit) =
        commit.files
            .map { currentFile ->
                val changedFiles = commit.files - currentFile
                val actual = rulesService
                    .generateRules(changedFiles.toSet(), 7, minConfidence)
                    .map(Rule::toSet)

                val containsExpected = if (actual.contains(currentFile)) 1.0 else 0.0
                Result(
                    precision = actual.calculatePrecision(containsExpected) ?: 1.0,
                    fairPrecision = actual.calculatePrecision(containsExpected) ?: NaN,
                    recall = containsExpected
                )
            }

    private fun List<String>.calculatePrecision(containsExpected: Double) =
        takeIf(List<String>::isNotEmpty)
            ?.run { containsExpected / size }
}