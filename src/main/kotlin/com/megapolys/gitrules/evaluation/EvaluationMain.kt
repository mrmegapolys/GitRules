package com.megapolys.gitrules.evaluation

import com.megapolys.gitrules.evaluation.strategies.ErrorPreventionEvaluation
import com.megapolys.gitrules.evaluation.strategies.FalseAlarmEvaluation
import com.megapolys.gitrules.evaluation.strategies.SourceCodeNavigationEvaluation
import com.megapolys.gitrules.miner.dataSource.SimpleGitLogFileDataSource
import com.megapolys.gitrules.miner.fpGrowth.FpGrowth
import com.megapolys.gitrules.server.RulesService

private const val INPUT_FILENAME = "input/guava.txt"
private const val MIN_SUPPORT = 12
fun main() {
    val commits = SimpleGitLogFileDataSource(INPUT_FILENAME).getCommits()
    val (train, test) = commits.run { dropLast(1000) to takeLast(1000) }

    val itemsets = FpGrowth(MIN_SUPPORT)
        .runWithStatistics(train)
    val rulesService = RulesService(itemsets.levels)


    val navigationExperiment = Experiment(
        SourceCodeNavigationEvaluation(rulesService, minConfidence = 0.5)
    )
    val preventionExperiment = Experiment(
        ErrorPreventionEvaluation(rulesService, minConfidence = 0.95)
    )
    val falseAlarmExperiment = Experiment(
        FalseAlarmEvaluation(rulesService, minConfidence = 0.95)
    )

    println("Starting source code navigation evaluation")
    val navigationResult = navigationExperiment.run(test, chunkSize = 125)
    println("Source code navigation evaluation:")
    println(navigationResult)

    println("Starting error prevention evaluation")
    val preventionResult = preventionExperiment.run(test, chunkSize = 125)
    println("Error prevention evaluation:")
    println(preventionResult)

    println("Starting false alarm evaluation")
    val falseAlarmResult = falseAlarmExperiment.run(test, chunkSize = 125)
    println("False alarm evaluation:")
    println(falseAlarmResult)
}

