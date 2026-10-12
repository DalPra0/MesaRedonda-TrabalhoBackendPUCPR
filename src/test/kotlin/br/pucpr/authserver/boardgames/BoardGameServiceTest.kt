package br.pucpr.authserver.boardgames

import br.pucpr.authserver.exception.NotFoundException
import br.pucpr.authserver.exceptions.BadRequestException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import java.util.Optional

class BoardGameServiceTest {
    private val repository = mockk<BoardGameRepository>()
    private val tagRepository = mockk<TagRepository>()
    private val service = BoardGameService(repository, tagRepository)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        checkUnnecessaryStub(repository, tagRepository)
    }

    @Test
    fun `insert should save and return game when name does not exist`() {
        val game = oneBoardGame(id = null)
        val saved = oneBoardGame()
        every { repository.findByNameIgnoreCase(game.name) } returns null
        every { repository.save(game) } returns saved

        service.insert(game) shouldBe saved
    }

    @Test
    fun `insert should throw BadRequestException when game already exists`() {
        val game = oneBoardGame()
        every { repository.findByNameIgnoreCase(game.name) } returns oneBoardGame(id = 2L)

        val exception = shouldThrow<BadRequestException> {
            service.insert(game)
        }

        exception.message shouldBe "Board game already exists"
    }

    @Test
    fun `insert should throw BadRequestException when minPlayers is greater than maxPlayers`() {
        val game = oneBoardGame(minPlayers = 5, maxPlayers = 2)

        val exception = shouldThrow<BadRequestException> {
            service.insert(game)
        }

        exception.message shouldBe "minPlayers cannot be greater than maxPlayers"
    }

    @Test
    fun `findById should throw NotFoundException when game does not exist`() {
        every { repository.findById(9L) } returns Optional.empty()

        val exception = shouldThrow<NotFoundException> {
            service.findById(9L)
        }

        exception.message shouldBe "Not found. id=9"
    }

    @Test
    fun `update should replace fields and save`() {
        val current = oneBoardGame()
        val updated = oneBoardGame(
            name = "Catan 5th",
            minPlayers = 3,
            maxPlayers = 6,
            durationMinutes = 120,
            weight = 3,
            description = "Edição nova",
        )
        every { repository.findById(1L) } returns Optional.of(current)
        every { repository.findByNameIgnoreCase("Catan 5th") } returns null
        every { repository.save(current) } returns current

        val result = service.update(1L, updated)

        result.name shouldBe "Catan 5th"
        result.maxPlayers shouldBe 6
        result.weight shouldBe 3
        result.description shouldBe "Edição nova"
    }

    @Test
    fun `delete should remove the game`() {
        val game = oneBoardGame(tags = mutableSetOf(oneTag()))
        every { repository.findById(1L) } returns Optional.of(game)
        every { repository.save(game) } returns game
        every { repository.delete(game) } returns Unit

        service.delete(1L)

        game.tags.isEmpty() shouldBe true
        verify(exactly = 1) { repository.delete(game) }
    }

    @Test
    fun `search should filter by tag players and name and sort by name ascending`() {
        val expected = listOf(oneBoardGame())
        val sort = Sort.by(Sort.Direction.ASC, "name")
        every { repository.search("cat", "party", 3, 4, sort) } returns expected

        val result = service.search(
            tag = " party ",
            minPlayers = 3,
            maxPlayers = 4,
            name = " cat ",
            sort = "name,asc",
        )

        result shouldBe expected
    }

    @Test
    fun `search should reject an unknown sort field`() {
        val exception = shouldThrow<BadRequestException> {
            service.search(null, null, null, null, "price,asc")
        }

        exception.message shouldBe "Invalid sort field"
    }

    @Test
    fun `addTag should associate a tag`() {
        val game = oneBoardGame(tags = mutableSetOf())
        val tag = oneTag()
        every { repository.findById(1L) } returns Optional.of(game)
        every { tagRepository.findById(1L) } returns Optional.of(tag)
        every { repository.save(game) } returns game

        service.addTag(1L, 1L) shouldBe true
        game.tags shouldBe mutableSetOf(tag)
    }

    @Test
    fun `addTag should return false when the tag is already associated`() {
        val tag = oneTag()
        val game = oneBoardGame(tags = mutableSetOf(tag))
        every { repository.findById(1L) } returns Optional.of(game)
        every { tagRepository.findById(1L) } returns Optional.of(tag)

        service.addTag(1L, 1L) shouldBe false
    }

    @Test
    fun `addTag should throw NotFoundException when the tag does not exist`() {
        every { repository.findById(1L) } returns Optional.of(oneBoardGame())
        every { tagRepository.findById(8L) } returns Optional.empty()

        val exception = shouldThrow<NotFoundException> {
            service.addTag(1L, 8L)
        }

        exception.message shouldBe "Tag not found. id=8"
    }

    @Test
    fun `removeTag should detach the tag`() {
        val tag = oneTag()
        val game = oneBoardGame(tags = mutableSetOf(tag))
        every { repository.findById(1L) } returns Optional.of(game)
        every { tagRepository.findById(1L) } returns Optional.of(tag)
        every { repository.save(game) } returns game

        service.removeTag(1L, 1L) shouldBe true
        game.tags.isEmpty() shouldBe true
    }
}
