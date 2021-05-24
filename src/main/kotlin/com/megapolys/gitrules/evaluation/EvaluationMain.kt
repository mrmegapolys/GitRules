package com.megapolys.gitrules.evaluation

import com.megapolys.gitrules.evaluation.strategies.ErrorPreventionEvaluation
import com.megapolys.gitrules.evaluation.strategies.FalseAlarmEvaluation
import com.megapolys.gitrules.evaluation.strategies.SourceCodeNavigationEvaluation
import com.megapolys.gitrules.miner.dataSource.SimpleGitLogFileDataSource
import com.megapolys.gitrules.miner.fpGrowth.FpGrowth
import com.megapolys.gitrules.server.RulesService

private const val INPUT_FILENAME = "input/th_full.txt"
private const val MIN_SUPPORT = 8

fun main() {
    val commits = SimpleGitLogFileDataSource(INPUT_FILENAME).getCommits()
    val (train, test) = commits.run { dropLast(1000) to takeLast(1000) }

    val itemsets = FpGrowth(MIN_SUPPORT)
        .runWithStatistics(train)
    val rulesService = RulesService(itemsets.levels)


    val navigationExperiment = Experiment(
        SourceCodeNavigationEvaluation(rulesService)
    )
    val preventionExperiment = Experiment(
        ErrorPreventionEvaluation(rulesService, minConfidence = 0.9)
    )
    val falseAlarmExperiment = Experiment(
        FalseAlarmEvaluation(rulesService, minConfidence = 0.9)
    )

    println("Starting source code navigation evaluation")
    val navigationResult = navigationExperiment.run(test, chunkSize = 25)
    println("Source code navigation evaluation:")
    println(navigationResult)

    println("Starting error prevention evaluation")
    val preventionResult = preventionExperiment.run(test, chunkSize = 3)
    println("Error prevention evaluation:")
    println(preventionResult)

    println("Starting false alarm evaluation")
    val falseAlarmResult = falseAlarmExperiment.run(test, chunkSize = 5)
    println("False alarm evaluation:")
    println(falseAlarmResult)
}

