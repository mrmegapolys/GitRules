package com.megapolys.gitrules

import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPGrowth

private const val INPUT_FILENAME = "/Users/u18398407/Diploma/commits/th_full_hash.txt"
private const val MIN_SUPPORT = 8

fun main() {
    val commits = GitLogFileDataSource(INPUT_FILENAME)
        .getCommits()
    val algo = AlgoFPGrowth()
    val itemsets = algo
        .runAlgorithm(commits, MIN_SUPPORT)

    algo.printStats()
}