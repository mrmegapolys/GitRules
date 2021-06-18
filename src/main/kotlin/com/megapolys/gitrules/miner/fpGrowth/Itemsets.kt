package com.megapolys.gitrules.miner.fpGrowth

import com.megapolys.gitrules.model.Itemset

class Itemsets {
    val levels = mutableListOf<MutableList<Itemset>>()

    fun addItemset(itemset: Itemset) {
        val level = itemset.items.size

        while (levels.size <= level) {
            levels.add(mutableListOf())
        }
        levels[level].add(itemset)
    }

    val count
        get() = levels.flatten().count()
}