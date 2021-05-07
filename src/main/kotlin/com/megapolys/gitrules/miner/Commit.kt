package com.megapolys.gitrules.miner

data class Commit(
    val hash: String,
    val files: Collection<String>
)
