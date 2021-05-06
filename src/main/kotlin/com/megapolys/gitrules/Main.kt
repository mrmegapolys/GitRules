package com.megapolys.gitrules

import com.megapolys.gitrules.spmf.FpGrowth

private const val INPUT_FILENAME = "/Users/u18398407/Diploma/commits/th_full_hash.txt"
private const val MIN_SUPPORT = 8

fun main() {
    val commits = GitLogFileDataSource(INPUT_FILENAME)
        .getCommits()
    val itemsets = FpGrowth(MIN_SUPPORT)
        .runWithStatistics(commits)
        .levels

    CorrectnessChecker.checkVersusBaseline(itemsets)
}