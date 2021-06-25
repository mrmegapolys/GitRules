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

        val miningStartTime = currentTimeMillis()
        try {
            future.get(miningTimeout.toSeconds(), SECONDS)
        } catch (e: TimeoutException) {
            executor.shutdownNow()
            println("Mining timed out, skipping...")
            return null
        }
        val miningTime = currentTimeMillis() - miningStartTime

        val rulesService = RulesService(itemsets!!.levels)

        val evaluationStartTime = currentTimeMillis()
        println("Starting source code navigation evaluation")
        val navigationResult = Experiment(
            SourceCodeNavigationEvaluation(rulesService, minConfidence = 0.3)
        ).run(testCommits.filter { it.files.size in 1..50 }, chunkSize = 25)

        println("Starting error prevention evaluation")
        val preventionResult = Experiment(
            ErrorPreventionEvaluation(rulesService, minConfidence = 0.95)
        ).run(testCommits.filter { it.files.size in 2..50 }, chunkSize = 3)

        println("Starting false alarm evaluation")
        val falseAlarmResult = Experiment(
            FalseAlarmEvaluation(rulesService, minConfidence = 0.95)
        ).run(testCommits.filter { it.files.size in 1..50 }, chunkSize = 5)
        val evaluationTime = currentTimeMillis() - evaluationStartTime

        return mapOf(
            "navigation" to navigationResult,
            "prevention" to preventionResult,
            "falseAlarm" to falseAlarmResult,
            "miningTime" to miningTime,
            "evaluationTime" to evaluationTime,
            "itemsetsSize" to itemsets!!.count
        )
    }
}


