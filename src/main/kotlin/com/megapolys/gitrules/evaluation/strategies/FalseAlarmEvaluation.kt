package com.megapolys.gitrules.evaluation.strategies

import com.megapolys.gitrules.evaluation.Result
import com.megapolys.gitrules.miner.Commit
import com.megapolys.gitrules.server.RulesService

class FalseAlarmEvaluation(
    rulesService: RulesService,
    minConfidence: Double
) : Evaluation(rulesService, minConfidence) {
    override fun runQueries(commit: Commit) =
        listOf(
            Result(
                precision = if (getRules(commit.files).isEmpty()) 1.0 else 0.0
            )
        )
}