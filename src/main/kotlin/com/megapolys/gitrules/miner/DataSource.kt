package com.megapolys.gitrules.miner

import java.io.File

class DataSource(private val fileName: String) {
    fun getCommits() =
        File(fileName)
            .readText()
            .split("\n\n")
            .filter(String::isNotBlank)
            .map(String::trim)
            .map { it.split("\n") }
            .map(::Commit)
}