package br.pucpr.authserver.users

import br.pucpr.authserver.exceptions.BadRequestException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class SortDirTest {

    @ParameterizedTest
    @ValueSource(strings = ["ASC", "asc", "Asc", "aSc"])
    fun `findOrNull should return ASC for valid ASC variations`(input: String) {
        val result = SortDir.findOrNull(input)
        result shouldBe SortDir.ASC
    }

    @ParameterizedTest
    @ValueSource(strings = ["DESC", "desc", "Desc", "dEsC"])
    fun `findOrNull should return DESC for valid DESC variations`(input: String) {
        val result = SortDir.findOrNull(input)
        result shouldBe SortDir.DESC
    }

    @ParameterizedTest
    @ValueSource(strings = ["ASC", "asc", "Asc", "aSc"])
    fun `find should return ASC for valid ASC variations`(input: String) {
        val result = SortDir.find(input)
        result shouldBe SortDir.ASC
    }

    @ParameterizedTest
    @ValueSource(strings = ["DESC", "desc", "Desc", "dEsC"])
    fun `find should return DESC for valid DESC variations`(input: String) {
        val result = SortDir.find(input)
        result shouldBe SortDir.DESC
    }

    @Test
    fun `findOrNull should return null for invalid input`() {
        val result = SortDir.findOrNull("invalid")
        result.shouldBeNull()
    }

    @Test
    fun `find should throw BadRequestException for invalid inputs`() {
        val exception = shouldThrow<BadRequestException> {
            SortDir.find("invalid")
        }
        exception.message shouldBe "Invalid sort dir"
    }

    @Test
    fun `SortDir enum values should contain ASC and DESC`() {
        SortDir.entries.size shouldBe 2
        SortDir.entries shouldBe listOf(SortDir.ASC, SortDir.DESC)
    }
}