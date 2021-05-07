package com.megapolys.gitrules.miner

import java.io.File

class DataSource(private val fileName: String) {
    fun getCommits() =
        File(fileName)
            .readText()
            .split("\n\n")
            .map(String::trim)
            .map { it.split("\n", limit = 2) }
            .filter { it.size == 2 }
            .map {
                Commit(
                    hash = it.first(),
                    files = it.last().split("\n")
                )
            }
}