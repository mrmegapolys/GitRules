package com.megapolys.gitrules.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class ServerApp

fun main() {
    runApplication<ServerApp>()
}