package com.megapolys.gitrules.server

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class Config(
    private val itemsetProvider: ItemsetProvider
) {
    @Bean
    open fun itemsets() = itemsetProvider.getItemsets()
}