package com.megapolys.gitrules.model

import com.fasterxml.jackson.annotation.JsonProperty

data class CompressedItemset(
    @JsonProperty("i")
    val items: Collection<Int>,
    @JsonProperty("s")
    val support: Int
)
