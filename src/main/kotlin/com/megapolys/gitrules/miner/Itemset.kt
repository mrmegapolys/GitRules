package com.megapolys.gitrules.miner

data class Itemset(
    val items: Collection<String>,
    val support: Int
)
