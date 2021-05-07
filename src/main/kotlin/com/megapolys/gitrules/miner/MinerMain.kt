package com.megapolys.gitrules.miner

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.megapolys.gitrules.miner.fpGrowth.FpGrowth
import java.io.File

private const val INPUT_FILENAME = "/Users/u18398407/Diploma/commits/th_full_hash.txt"
private const val MIN_SUPPORT = 8

fun main() {
    val commits = DataSource(INPUT_FILENAME)
        .getCommits()
    val itemsets = FpGrowth(MIN_SUPPORT)
        .runWithStatistics(commits)

    jacksonObjectMapper()
        .writerWithDefaultPrettyPrinter()
        .writeValue(File(generateOutputFilename()), itemsets.levels)
}

private fun generateOutputFilename() =
    INPUT_FILENAME
        .substringAfterLast("/")
        .substringBefore(".") +
            "_pretty" +
            "_$MIN_SUPPORT" +
            ".json"