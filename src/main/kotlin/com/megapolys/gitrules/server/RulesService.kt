package com.megapolys.gitrules.server

import com.megapolys.gitrules.model.Itemset
import org.springframework.stereotype.Service

@Service
class RulesService(itemsets: List<List<Itemset>>) {
    private val preparedItemsets =
        itemsets
            .drop(2)
            .flatten()

    private val supportMap =
        itemsets.map { level ->
            level.associate { it.items to it.support }
        }

    fun generateRules(files: Set<String>, size: Int, minConfidence: Double) =
        preparedItemsets
            .filter { files.size >= it.items.size - 1 }
            .generateRules(files)
            .filter { it.confidence >= minConfidence }
            .takeTopUnique(size)

    private fun List<Itemset>.generateRules(changedFiles: Set<String>) =
        mapNotNull { itemset ->
            when (val toSet = itemset.items.singleOrNull { !changedFiles.contains(it) }) {
                null -> null
                else -> {
                    val fromSet = itemset.items - toSet
                    val i = supportMap[fromSet.size][fromSet]
                    if (i == null) {
                        println(itemset)
                    }
                    Rule(
                        fromSet = fromSet,
                        toSet = toSet,
                        support = itemset.support,
                        confidence = itemset.support.toDouble() / i!!
                    )
                }
            }
        }
}

private fun List<Rule>.takeTopUnique(size: Int) =
    sortedWith(
        compareByDescending(Rule::confidence)
            .thenByDescending(Rule::support)
    )
        .distinctBy { it.toSet }
        .take(size)