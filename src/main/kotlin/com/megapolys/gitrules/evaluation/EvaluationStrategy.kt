package com.megapolys.gitrules.evaluation

import com.megapolys.gitrules.miner.Commit
import com.megapolys.gitrules.server.RulesService

abstract class EvaluationStrategy(
    protected val rulesService: RulesService
) {
    abstract fun runQueries(commit: Commit): List<Result>
}