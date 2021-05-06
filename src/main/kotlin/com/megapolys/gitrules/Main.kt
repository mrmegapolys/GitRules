package com.megapolys.gitrules

import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPGrowth

private const val INPUT_FILENAME = "/Users/u18398407/Diploma/commits/th_full_hash.txt"
private const val MIN_SUPPORT = 8

fun main() {
    val algo = AlgoFPGrowth()

    val commits = GitLogFileDataSource(INPUT_FILENAME)
        .getCommits()
    val itemsets = algo
        .runAlgorithm(commits, MIN_SUPPORT)
        .levels

    algo.printStats()

    CorrectnessChecker.checkVersusBaseline(itemsets)
}