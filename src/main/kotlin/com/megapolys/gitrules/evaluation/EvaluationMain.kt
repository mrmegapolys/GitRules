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
//    "tradehub"
)

private val supports = 18 downTo 1

fun main() {
    val objectWriter = jacksonObjectMapper()
        .writerWithDefaultPrettyPrinter()

    val resultFolder = "results/gen8_max5_30s"
    File(resultFolder).mkdirs()

    projects.forEach { project ->
        println("Starting $project evaluation")
        val commits = SimpleGitLogFileDataSource("input/$project.txt")
            .getCommits()
        val testSize = commits.size / 10
        println("Test size: $testSize")

        val (train, test) = commits.run { dropLast(testSize) to takeLast(testSize) }

        val results = mutableMapOf<Int, Map<String, Any>>()
        for (support in supports) {
            println("Starting $project evaluation with minSupport $support")
            ExperimentRunner().run(
                trainCommits = train,
                testCommits = test,
                minSupport = support,
                miningTimeout = ofSeconds(30)
            )?.apply { results[support] = this } ?: break
        }

        objectWriter.writeValue(
            File("$resultFolder/$project.json"),
            results + ("commitsSize" to commits.size)
        )
    }
}