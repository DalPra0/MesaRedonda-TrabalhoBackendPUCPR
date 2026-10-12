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

        games.search(name = null, tag = "party", minPlayers = null, maxPlayers = null, sort = Sort.by("name"))
            .map { it.name }
            .shouldContainExactly("Catan", "Codenames")

        games.search(name = "azu", tag = null, minPlayers = null, maxPlayers = null, sort = Sort.by("name"))
            .map { it.name }
            .shouldContainExactly("Azul")

        games.search(name = null, tag = null, minPlayers = 3, maxPlayers = 4, sort = Sort.by("weight"))
            .map { it.name }
            .shouldContainExactly("Catan")

        games.search(
            name = null,
            tag = "party",
            minPlayers = null,
            maxPlayers = null,
            sort = Sort.by(Sort.Direction.DESC, "name"),
        ).map { it.name }
            .shouldContainExactly("Codenames", "Catan")
    }
}
