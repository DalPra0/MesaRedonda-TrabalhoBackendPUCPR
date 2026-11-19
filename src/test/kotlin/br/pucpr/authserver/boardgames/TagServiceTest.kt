package br.pucpr.authserver.boardgames

import br.pucpr.authserver.exception.NotFoundException
import br.pucpr.authserver.exceptions.BadRequestException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

class TagServiceTest {
    private val repository = mockk<TagRepository>()
    private val boardGameRepository = mockk<BoardGameRepository>()
    private val service = TagService(repository, boardGameRepository)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        checkUnnecessaryStub(repository, boardGameRepository)
    }

    @Test
    fun `insert should save when slug is free`() {
        val tag = oneTag(id = null, slug = "solo", label = "Solo")
        val saved = oneTag(id = 5L, slug = "solo", label = "Solo")
        every { repository.findBySlug("solo") } returns null
        every { repository.save(tag) } returns saved

        service.insert(tag) shouldBe saved
    }

    @Test
    fun `insert should throw BadRequestException when slug already exists`() {
        every { repository.findBySlug("party") } returns oneTag()

        val exception = shouldThrow<BadRequestException> {
            service.insert(oneTag(id = null))
        }

        exception.message shouldBe "Tag already exists"
    }

    @Test
    fun `insert should throw BadRequestException when label is blank`() {
        shouldThrow<BadRequestException> {
            service.insert(oneTag(id = null, label = " "))
        }
    }

    @Test
    fun `update should change slug and label`() {
        val tag = oneTag()
        every { repository.findById(1L) } returns Optional.of(tag)
        every { repository.findBySlug("festa") } returns null
        every { repository.save(tag) } returns tag

        val result = service.update(1L, oneTag(id = null, slug = "festa", label = "Festa"))

        result.slug shouldBe "festa"
        result.label shouldBe "Festa"
    }

    @Test
    fun `update should throw BadRequestException when slug belongs to another tag`() {
        every { repository.findById(1L) } returns Optional.of(oneTag())
        every { repository.findBySlug("euro") } returns oneTag(id = 2L, slug = "euro")

        shouldThrow<BadRequestException> {
            service.update(1L, oneTag(id = null, slug = "euro", label = "Euro"))
        }
    }

    @Test
    fun `update should throw NotFoundException when tag does not exist`() {
        every { repository.findById(9L) } returns Optional.empty()

        shouldThrow<NotFoundException> {
            service.update(9L, oneTag(id = null))
        }
    }

    @Test
    fun `delete should detach the tag from board games before deleting it`() {
        val tag = oneTag()
        val game = oneBoardGame(tags = mutableSetOf(tag))
        every { repository.findById(1L) } returns Optional.of(tag)
        every { boardGameRepository.findByTagId(1L) } returns listOf(game)
        every { boardGameRepository.save(game) } returns game
        every { repository.delete(tag) } just runs

        service.delete(1L)

        game.tags shouldBe mutableSetOf()
        verify { repository.delete(tag) }
    }

    @Test
    fun `delete should throw NotFoundException when tag does not exist`() {
        every { repository.findById(9L) } returns Optional.empty()

        shouldThrow<NotFoundException> {
            service.delete(9L)
        }
    }
}
