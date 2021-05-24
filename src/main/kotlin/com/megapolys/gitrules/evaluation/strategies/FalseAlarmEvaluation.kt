package com.megapolys.gitrules.evaluation.strategies

import com.megapolys.gitrules.evaluation.Result
import com.megapolys.gitrules.miner.Commit
import com.megapolys.gitrules.server.RulesService

class FalseAlarmEvaluation(
    rulesService: RulesService,
    private val minConfidence: Double
) : Evaluation(rulesService) {
    override fun runQueries(commit: Commit) =
        rulesService.generateRules(commit.files.toSet(), 7, minConfidence).run {
            listOf(
                Result(
                    precision = if (isEmpty()) 1.0 else 0.0
                )
            )
        }
}