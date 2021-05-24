package com.megapolys.gitrules.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance
import com.megapolys.gitrules.model.CompressedItemset
import com.megapolys.gitrules.model.Itemset
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

@Service
class ItemsetProvider(
    private val objectMapper: ObjectMapper,
    @Value("\${itemsets.filename}")
    private val itemsetsFilename: String,
    @Value("\${compressMap.filename}")
    private val compressMapFilename: String
) {
    private val compressMapJavaType = defaultInstance()
        .constructMapType(
            Map::class.java,
            Int::class.java,
            String::class.java
        )

    private val itemsetsJavaType = defaultInstance().run {
        constructCollectionType(
            List::class.java,
            constructCollectionType(
                List::class.java,
                CompressedItemset::class.java
            )
        )
    }

    fun getItemsets(): List<List<Itemset>> {
        val compressMap = objectMapper
            .readValue<Map<Int, String>>(File(compressMapFilename), compressMapJavaType)
        val compressedItemsets = objectMapper
            .readValue<List<List<CompressedItemset>>>(File(itemsetsFilename), itemsetsJavaType)

        return compressedItemsets.decompress(compressMap)
    }
}

private fun List<List<CompressedItemset>>.decompress(compressMap: Map<Int, String>) =
    map { level ->
        level.map { compressedItemset ->
            Itemset(
                items = compressedItemset.items.map { compressMap[it]!! },
                support = compressedItemset.support
            )
        }
    }