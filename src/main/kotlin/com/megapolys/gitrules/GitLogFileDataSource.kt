package com.megapolys.gitrules

import java.io.File

class GitLogFileDataSource(
    private val fileName: String
) : DataSource {
    override fun getCommits() =
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