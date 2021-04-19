package com.megapolys.gitrules

private const val FILENAME = "/Users/u18398407/Diploma/commits/th_full_hash.txt"

fun main() {
    val source = GitLogFileDataSource(FILENAME)
    val commits = source.getCommits()
    println(commits.first())

}