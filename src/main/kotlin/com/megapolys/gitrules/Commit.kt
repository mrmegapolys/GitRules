package com.megapolys.gitrules

data class Commit(
    val hash: String,
    val files: Collection<String>
)
