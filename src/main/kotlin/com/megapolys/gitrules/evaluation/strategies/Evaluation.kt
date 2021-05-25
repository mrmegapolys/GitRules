package com.megapolys.gitrules.evaluation.strategies

import com.megapolys.gitrules.evaluation.Result
import com.megapolys.gitrules.miner.Commit
import com.megapolys.gitrules.server.Rule
import com.megapolys.gitrules.server.RulesService

abstract class Evaluation(
    private val rulesService: RulesService,
    private val minConfidence: Double
) {
    abstract fun runQueries(commit: Commit): List<Result>

    protected fun getRules(files: Collection<String>) =
        rulesService.generateRules(files.toSet(), 7, minConfidence)
            .map(Rule::toSet)
}