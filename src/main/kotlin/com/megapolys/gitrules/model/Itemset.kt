package com.megapolys.gitrules.model

data class Itemset(
    val items: Collection<String>,
    val support: Int
)
