package com.megapolys.gitrules.evaluation.strategies

import com.megapolys.gitrules.evaluation.Result
import com.megapolys.gitrules.miner.Commit
import com.megapolys.gitrules.server.RulesService

abstract class Evaluation(
    protected val rulesService: RulesService
) {
    abstract fun runQueries(commit: Commit): List<Result>
}