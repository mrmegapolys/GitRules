package com.megapolys.gitrules.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance
import com.megapolys.gitrules.model.Itemset
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

@Service
class RulesService(
    objectMapper: ObjectMapper,
    @Value("\${itemsets.filename}")
    private val filename: String
) {
    private val javaType = defaultInstance().run {
        constructCollectionType(
            List::class.java,
            constructCollectionType(
                List::class.java,
                Itemset::class.java
            )
        )
    }
    private val itemsets = objectMapper
        .readValue<List<List<Itemset>>>(File(filename), javaType)

    fun generateRules(files: Collection<String>, size: Int) =
        itemsets
            .drop(2)
            .flatten()
            .filter { it.items.any(files::contains) }
            .flatMap { generateRulesFromItemset(it, files) }
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