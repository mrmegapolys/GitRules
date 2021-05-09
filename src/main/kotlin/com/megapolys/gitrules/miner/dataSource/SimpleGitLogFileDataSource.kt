package com.megapolys.gitrules.miner.dataSource

import com.megapolys.gitrules.miner.Commit
import java.io.File

class SimpleGitLogFileDataSource(
    private val fileName: String
) : DataSource {
    override fun getCommits() =
        File(fileName)
            .readText()
            .split("\n\n")
            .filter(String::isNotBlank)
            .map(String::trim)
            .map { it.split("\n") }
            .map(::Commit)
}