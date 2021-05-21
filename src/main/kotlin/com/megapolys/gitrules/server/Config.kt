package com.megapolys.gitrules.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import com.megapolys.gitrules.model.Itemset
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
open class Config(
    private val objectMapper: ObjectMapper,
    @Value("\${itemsets.filename}")
    private val filename: String
) {
    private val javaType = TypeFactory.defaultInstance().run {
        constructCollectionType(
            List::class.java,
            constructCollectionType(
                List::class.java,
                Itemset::class.java
            )
        )
    }

    @Bean
    open fun itemsets(): List<List<Itemset>> =
        objectMapper.readValue(File(filename), javaType)
}