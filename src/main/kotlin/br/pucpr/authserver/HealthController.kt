package br.pucpr.authserver

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** Health check usado por load balancers e para validar a instalação (Aula 02). */
@RestController
class HealthController {
    @GetMapping("/health")
    fun health() = mapOf("status" to "OK")
}
