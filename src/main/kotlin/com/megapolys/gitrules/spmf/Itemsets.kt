package com.megapolys.gitrules.spmf

class Itemsets {
    val levels = mutableListOf<MutableList<Itemset>>()

    fun addItemset(itemset: Itemset, level: Int) {
        while (levels.size <= level) {
            levels.add(mutableListOf())
        }
        levels[level].add(itemset)
    }

    val count
        get() = levels.flatten().count()
}