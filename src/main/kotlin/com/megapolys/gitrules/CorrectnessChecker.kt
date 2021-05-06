package com.megapolys.gitrules

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.megapolys.gitrules.spmf.Itemset
import java.io.File

private val mapper = jacksonObjectMapper()
private val type: JavaType = TypeFactory.defaultInstance().run {
    constructCollectionType(
        List::class.java,
        constructCollectionType(
            List::class.java,
            Itemset::class.java
        )
    )
}

object CorrectnessChecker {
    fun checkVersusBaseline(itemsets: List<List<Itemset>>) {
        val baselineItemsets = mapper.readValue<List<List<Itemset>>>(
            File("th_full_hash_pretty_8.json"), type
        )
        baselineItemsets.forEachIndexed { index, row ->
            assert(row.size == itemsets[index].size)
            row.forEachIndexed { rowIndex, itemset ->
                assert(itemset == itemsets[index][rowIndex])
            }
        }
        assert(itemsets == baselineItemsets)
    }
}