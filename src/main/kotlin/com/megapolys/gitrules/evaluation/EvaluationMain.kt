package com.megapolys.gitrules.evaluation

import com.megapolys.gitrules.evaluation.strategies.SourceCodeNavigationEvaluation
import com.megapolys.gitrules.miner.DataSource
import com.megapolys.gitrules.miner.fpGrowth.FpGrowth
import com.megapolys.gitrules.server.RulesService

private const val INPUT_FILENAME = "/Users/u18398407/Diploma/commits/th_full_hash.txt"
private const val MIN_SUPPORT = 8

fun main() {
    val commits = DataSource(INPUT_FILENAME).getCommits()
    val (train, test) = commits.run { dropLast(1000) to takeLast(1000) }

    val itemsets = FpGrowth(MIN_SUPPORT)
        .runWithStatistics(train)
    val rulesService = RulesService(itemsets.levels)

    val experiment = Experiment(SourceCodeNavigationEvaluation(rulesService))
    println(experiment.run(test, chunkSize = 25))
}

