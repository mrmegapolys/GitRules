package com.megapolys.gitrules.evaluation

import com.megapolys.gitrules.evaluation.strategies.Evaluation
import com.megapolys.gitrules.miner.Commit
import java.lang.System.currentTimeMillis
import java.util.concurrent.Executors.newFixedThreadPool
import java.util.concurrent.Future

class Experiment(private val evaluation: Evaluation) {
    fun run(testCommits: List<Commit>, chunkSize: Int) =
        withTimer {
            with(newFixedThreadPool(16)) {
                try {
                    testCommits
                        .chunked(chunkSize)
                        .mapIndexed { index, commits ->
                            with(mutableListOf<Result>()) {
                                submit({
                                    val startTime = currentTimeMillis()
                                    commits
                                        .flatMap(evaluation::runQueries)
                                        .forEach(::add)
                                    println("Done chunk $index in ${(currentTimeMillis() - startTime) / 1000}s")
                                }, this)
                            }
                        }
                        .flatMap(Future<MutableList<Result>>::get)
                        .aggregate()
                } finally {
                    shutdown()
                }
            }
        }
}

private fun <T> withTimer(function: () -> T) =
    currentTimeMillis().let { startTime ->
        function().apply {
            println("Calculated in ${(currentTimeMillis() - startTime) / 1000}s")
        }
    }
