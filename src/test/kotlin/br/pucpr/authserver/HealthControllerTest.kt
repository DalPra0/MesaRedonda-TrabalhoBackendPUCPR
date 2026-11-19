package br.pucpr.authserver

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class HealthControllerTest {
    @Test
    fun `health should report ok`() {
        HealthController().health() shouldBe mapOf("status" to "OK")
    }
}
