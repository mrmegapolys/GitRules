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
            .filter { it.items.any(files::contains) }
            .flatMap { generateRulesFromItemset(it, files) }
            .filter { it.confidence >= minConfidence }
            .sortedWith(
                compareByDescending(Rule::confidence)
                    .thenByDescending(Rule::support)
            )
            .distinctBy { it.toSet }
            .take(size)

    private fun generateRulesFromItemset(
        itemset: Itemset,
        changedFiles: Set<String>
    ) = itemset.items
        .mapNotNull { currentFile ->
            val fromSet = itemset.items.filter { it != currentFile }
            if (changedFiles.containsAll(fromSet) && !changedFiles.contains(currentFile)) {
                Rule(
                    fromSet = fromSet,
                    toSet = currentFile,
                    support = itemset.support,
                    confidence = itemset.support.toDouble() / supportMap[fromSet.size][fromSet]!!
                )
            } else null
        }
}