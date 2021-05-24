package com.megapolys.gitrules.server

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RulesController(
    private val rulesService: RulesService
) {
    @GetMapping("/rules")
    fun getRules(
        @RequestBody files: Collection<String>,
        @RequestParam size: Int,
        @RequestParam(defaultValue = "0") minConfidence: Double
    ) = rulesService.generateRules(files.toSet(), size, minConfidence)
}