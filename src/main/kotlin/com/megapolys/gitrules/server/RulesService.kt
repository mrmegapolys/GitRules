package com.megapolys.gitrules.server

import com.megapolys.gitrules.model.Itemset
import org.springframework.stereotype.Service

@Service
class RulesService(private val itemsets: List<List<Itemset>>) {
    private val preparedItemsets =
        itemsets
            .drop(2)
            .flatten()

    fun generateRules(files: Collection<String>, size: Int, minConfidence: Double) =
        preparedItemsets
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
        changedFiles: Collection<String>
    ) = itemset.items
        .mapNotNull { currentFile ->
            val fromSet = itemset.items.filter { it != currentFile }
            if (changedFiles.containsAll(fromSet) && !changedFiles.contains(currentFile)) {
                val fromSetSupport =
                    itemsets[fromSet.size]
                        .find { it.items == fromSet }!!
                        .support

                Rule(
                    fromSet = fromSet,
                    toSet = currentFile,
                    support = itemset.support,
                    confidence = itemset.support.toDouble() / fromSetSupport
                )
            } else null
        }
}