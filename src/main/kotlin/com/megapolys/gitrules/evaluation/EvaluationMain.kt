package com.megapolys.gitrules.evaluation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.megapolys.gitrules.miner.dataSource.SimpleGitLogFileDataSource
import java.io.File
import java.time.Duration.ofSeconds

private val projects = listOf(
    "guava",
    "jackson",
    "junit4",
    "lombok",
    "mockito",
    "netty",
    "tradehub"
)

private val supports = 18 downTo 2

fun main() {
    val objectWriter = jacksonObjectMapper()
        .writerWithDefaultPrettyPrinter()

    projects.forEach { project ->
        println("Starting $project evaluation")
        val commits = SimpleGitLogFileDataSource("input/$project.txt")
            .getCommits()
        val testSize = commits.size / 10
        println("Test size: $testSize")

        val (train, test) = commits.run { dropLast(testSize) to takeLast(testSize) }

        val results = supports.mapNotNull { support ->
            println("Starting evaluation with minSupport $support")
            ExperimentRunner().run(
                trainCommits = train,
                testCommits = test,
                minSupport = support,
                miningTimeout = ofSeconds(30)
            )?.let { support to it }
        }
            .toMap()
            .let {
                mutableMapOf<Any, Any>(
                    "commitsSize" to commits.size,
                    "testSize" to testSize,
                ).putAll(it)
            }

        objectWriter.writeValue(File("results/gen1/$project.json"), results)
    }
}