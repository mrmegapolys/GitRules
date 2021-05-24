package com.megapolys.gitrules.miner.dataSource

import java.io.File

fun File.readCommitLines() =
    readText()
        .split("\n\n")
        .filter(String::isNotBlank)
        .map(String::trim)
        .map { it.split("\n") }