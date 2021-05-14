package com.megapolys.gitrules.evaluation

import com.megapolys.gitrules.miner.Commit
import java.util.concurrent.Executors.newFixedThreadPool

class Evaluation(private val evaluationStrategy: EvaluationStrategy) {
    fun run(testCommits: List<Commit>) =
        with(newFixedThreadPool(8)) {
            try {
                testCommits
                    .chunked(25)
                    .mapIndexed { index, commits ->
                        val result = mutableListOf<Result>()
                        val future = submit {
                            commits
                                .flatMap(evaluationStrategy::runQueries)
                                .forEach(result::add)
                            println("Done chunk $index")
                        }
                        future to result
                    }.flatMap { (future, result) ->
                        future.get()
                        result
                    }.run {
                        Result(
                            precision = sumOf(Result::precision) / size,
                            fairPrecision = map(Result::fairPrecision)
                                .filterNot(Double::isNaN)
                                .let { it.sum() / it.size },
                            recall = sumOf(Result::recall) / size,
                            fairRecall = map(Result::fairRecall)
                                .filterNot(Double::isNaN)
                                .let { it.sum() / it.size }
                        )
                    }
            } finally {
                shutdown()
            }
        }
}
