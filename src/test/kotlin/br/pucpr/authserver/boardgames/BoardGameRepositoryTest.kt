package br.pucpr.authserver.boardgames

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.data.domain.Sort

@DataJpaTest
class BoardGameRepositoryTest @Autowired constructor(
    val games: BoardGameRepository,
    val tags: TagRepository,
) {
    @Test
    fun `search filters by tag name and player counts and sorts by name`() {
        val party = tags.save(Tag(slug = "party", label = "Party"))
        val euro = tags.save(Tag(slug = "euro", label = "Euro"))

        val catan = games.save(
            BoardGame(
                name = "Catan",
                minPlayers = 3,
                maxPlayers = 4,
                durationMinutes = 90,
                weight = 2,
                description = "Colonos",
            )
        )
        catan.tags.add(party)
        games.save(catan)

        val azul = games.save(
            BoardGame(
                name = "Azul",
                minPlayers = 2,
                maxPlayers = 4,
                durationMinutes = 45,
                weight = 2,
                description = "Azulejos",
            )
        )
        azul.tags.add(euro)
        games.save(azul)

        val codenames = games.save(
            BoardGame(
                name = "Codenames",
                minPlayers = 4,
                maxPlayers = 8,
                durationMinutes = 15,
                weight = 1,
                description = "Espias",
            )
        )
        codenames.tags.add(party)
        games.save(codenames)

        fun names(spec: org.springframework.data.jpa.domain.Specification<BoardGame>, sort: Sort) =
            games.findAll(spec, sort).map { it.name }

        // sem nenhum parâmetro o filtro dinâmico não restringe nada
        names(boardGameSearch(), Sort.by("name")).shouldContainExactly("Azul", "Catan", "Codenames")

        names(boardGameSearch(tag = "party"), Sort.by("name"))
            .shouldContainExactly("Catan", "Codenames")

        names(boardGameSearch(name = "azu"), Sort.by("name"))
            .shouldContainExactly("Azul")

        names(boardGameSearch(minPlayers = 3, maxPlayers = 4), Sort.by("weight"))
            .shouldContainExactly("Catan")

        names(boardGameSearch(tag = "party"), Sort.by(Sort.Direction.DESC, "name"))
            .shouldContainExactly("Codenames", "Catan")

        // critérios combinados: só entra quem atende a todos
        names(boardGameSearch(tag = "party", maxDuration = 30), Sort.by("name"))
            .shouldContainExactly("Codenames")

        names(boardGameSearch(maxDuration = 60), Sort.by("durationMinutes"))
            .shouldContainExactly("Codenames", "Azul")

        names(boardGameSearch(weight = 2), Sort.by("name"))
            .shouldContainExactly("Azul", "Catan")

        names(boardGameSearch(tag = "euro", weight = 1), Sort.by("name")).shouldContainExactly()
    }
}
