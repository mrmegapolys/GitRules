package com.megapolys.gitrules.miner

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.megapolys.gitrules.miner.fpGrowth.FpGrowth
import com.megapolys.gitrules.miner.fpGrowth.Itemsets
import com.megapolys.gitrules.model.CompressedItemset
import com.megapolys.gitrules.model.Itemset
import java.io.File

private const val INPUT_FILENAME = "input/th_full.txt"
private const val MIN_SUPPORT = 8

fun main() {
    val commits = DataSource(INPUT_FILENAME)
        .getCommits()
    val itemsets = FpGrowth(MIN_SUPPORT)
        .runWithStatistics(commits)

    val compressMap = buildCompressMap(itemsets)
    val compressedItemsets = itemsets.compress(compressMap)

    jacksonObjectMapper().run {
        writeValue(File(generateOutputFilename("map")), compressMap.inverse())
        writeValue(File(generateOutputFilename("itemsets")), compressedItemsets)
    }
}

private fun buildCompressMap(itemsets: Itemsets) =
    itemsets.levels
        .flatten()
        .flatMap(Itemset::items)
        .distinct()
        .mapIndexed { index, item -> item to index }
        .toMap()

private fun Itemsets.compress(compressMap: Map<String, Int>) =
    levels.map { level ->
        level.map { itemset ->
            CompressedItemset(
                items = itemset.items.map { compressMap[it]!! },
                support = itemset.support
            )
        }
    }

private fun <K, V> Map<K, V>.inverse() =
    map { (key, value) -> value to key }.toMap()

private fun generateOutputFilename(type: String) =
    INPUT_FILENAME
        .substringAfterLast("/")
        .substringBefore(".") +
            "_$MIN_SUPPORT" +
            "_$type" +
            ".json"