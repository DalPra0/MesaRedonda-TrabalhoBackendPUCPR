package br.pucpr.authserver.filters

import br.pucpr.authserver.boardgames.BoardGame
import br.pucpr.authserver.boardgames.BoardGameRepository
import br.pucpr.authserver.exceptions.BadRequestException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.data.domain.Sort

@DataJpaTest
class DynamicFilterTest @Autowired constructor(val games: BoardGameRepository) {
    private val fields = mapOf(
        "name" to String::class.java,
        "minPlayers" to Int::class.javaObjectType,
        "durationMinutes" to Int::class.javaObjectType,
        "weight" to Int::class.javaObjectType,
    )

    @BeforeEach
    fun seed() {
        games.save(BoardGame(name = "Catan", minPlayers = 3, maxPlayers = 4, durationMinutes = 90, weight = 2))
        games.save(BoardGame(name = "Azul", minPlayers = 2, maxPlayers = 4, durationMinutes = 45, weight = 2))
        games.save(BoardGame(name = "Codenames", minPlayers = 4, maxPlayers = 8, durationMinutes = 15, weight = 1))
    }

    private fun names(params: Map<String, String>, ignore: Set<String> = emptySet()) =
        games.findAll(DynamicFilter.of<BoardGame>(params, fields, ignore), Sort.by("name")).map { it.name }

    @Test
    fun `no params means no restriction`() {
        names(emptyMap()).shouldContainExactly("Azul", "Catan", "Codenames")
    }

    @Test
    fun `lte and gte filter numeric fields and combine with AND`() {
        names(mapOf("durationMinutes.lte" to "60")).shouldContainExactly("Azul", "Codenames")
        names(mapOf("weight.gte" to "2", "durationMinutes.gt" to "50")).shouldContainExactly("Catan")
        names(mapOf("minPlayers.lt" to "4", "weight.gte" to "2")).shouldContainExactly("Azul", "Catan")
    }

    @Test
    fun `eq is the default operator and ne excludes`() {
        names(mapOf("weight" to "1")).shouldContainExactly("Codenames")
        names(mapOf("weight.ne" to "2")).shouldContainExactly("Codenames")
        names(mapOf("weight.eq" to "2")).shouldContainExactly("Azul", "Catan")
    }

    @Test
    fun `like is case insensitive and ignored params and blank values are skipped`() {
        names(mapOf("name.like" to "AZ")).shouldContainExactly("Azul")
        names(mapOf("sort" to "name,desc"), ignore = setOf("sort")).shouldContainExactly("Azul", "Catan", "Codenames")
        names(mapOf("weight.gte" to "  ")).shouldContainExactly("Azul", "Catan", "Codenames")
    }

    @Test
    fun `unknown field operator or value is a BadRequest`() {
        shouldThrow<BadRequestException> { names(mapOf("preco" to "1")) }.message shouldBe "Invalid filter field: preco"
        shouldThrow<BadRequestException> { names(mapOf("weight.between" to "1")) }.message shouldBe
            "Invalid filter operator: between"
        shouldThrow<BadRequestException> { names(mapOf("weight" to "pesado")) }.message shouldBe
            "Invalid value for filter weight: pesado"
        shouldThrow<BadRequestException> { names(mapOf("weight.like" to "1")) }
        shouldThrow<BadRequestException> { names(mapOf("name.gt" to "a")) }
    }
}
