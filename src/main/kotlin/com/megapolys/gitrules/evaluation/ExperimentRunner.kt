package com.megapolys.gitrules.evaluation

import com.megapolys.gitrules.evaluation.strategies.ErrorPreventionEvaluation
import com.megapolys.gitrules.evaluation.strategies.FalseAlarmEvaluation
import com.megapolys.gitrules.evaluation.strategies.SourceCodeNavigationEvaluation
import com.megapolys.gitrules.miner.Commit
import com.megapolys.gitrules.miner.fpGrowth.FpGrowth
import com.megapolys.gitrules.miner.fpGrowth.Itemsets
import com.megapolys.gitrules.server.RulesService
import java.lang.System.currentTimeMillis
import java.time.Duration
import java.util.concurrent.Executors.newSingleThreadExecutor
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.TimeoutException

class ExperimentRunner {
    fun run(
        trainCommits: List<Commit>,
        testCommits: List<Commit>,
        minSupport: Int,
        miningTimeout: Duration
    ): Map<String, Any>? {
        val executor = newSingleThreadExecutor()

        var itemsets: Itemsets? = null
        val future = executor.submit({
            itemsets = FpGrowth(minSupport)
                .runWithStatistics(trainCommits)
        }, itemsets)
        executor.shutdown()

        val startTime = currentTimeMillis()
        try {
            future.get(miningTimeout.toSeconds(), SECONDS)
        } catch (e: TimeoutException) {
            executor.shutdownNow()
            println("Mining timed out, skipping...")
            return null
        }
        val miningTime = currentTimeMillis() - startTime

        if (itemsets!!.count > 50_000_000) {
            println("Too much itemsets, skipping...")
            return null
        }

        val rulesService = RulesService(itemsets!!.levels)

        println("Starting source code navigation evaluation")
        val navigationResult = Experiment(
            SourceCodeNavigationEvaluation(rulesService, minConfidence = 0.5)
        ).run(testCommits, chunkSize = 25)

        println("Starting error prevention evaluation")
        val preventionResult = Experiment(
            ErrorPreventionEvaluation(rulesService, minConfidence = 0.95)
        ).run(testCommits, chunkSize = 3)

        println("Starting false alarm evaluation")
        val falseAlarmResult = Experiment(
            FalseAlarmEvaluation(rulesService, minConfidence = 0.95)
        ).run(testCommits, chunkSize = 5)

        return mapOf(
            "navigation" to navigationResult,
            "prevention" to preventionResult,
            "falseAlarm" to falseAlarmResult,
            "miningTime" to miningTime
        )
    }
}


