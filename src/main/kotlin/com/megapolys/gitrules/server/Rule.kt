package com.megapolys.gitrules.server

data class Rule(
    val fromSet: Collection<String>,
    val toSet: String,
    val support: Int,
    val confidence: Double
)
