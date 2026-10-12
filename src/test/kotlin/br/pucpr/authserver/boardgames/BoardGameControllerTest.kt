package br.pucpr.authserver.boardgames

import br.pucpr.authserver.boardgames.requests.CreateBoardGameRequest
import br.pucpr.authserver.boardgames.requests.UpdateBoardGameRequest
import br.pucpr.authserver.boardgames.responses.BoardGameResponse
import br.pucpr.authserver.exception.NotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class BoardGameControllerTest {
    private val service = mockk<BoardGameService>()
    private val controller = BoardGameController(service)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        checkUnnecessaryStub(service)
    }

    @Test
    fun `list should return filtered games`() {
        val game = oneBoardGame(tags = mutableSetOf(oneTag()))
        every { service.search("party", 3, 4, "cat", "name,asc") } returns listOf(game)

        val response = controller.list(
            tag = "party",
            minPlayers = 3,
            maxPlayers = 4,
            name = "cat",
            sort = "name,asc",
        )

        response.statusCode shouldBe HttpStatus.OK
        response.body shouldBe listOf(BoardGameResponse(game))
    }

    @Test
    fun `insert should create and return the game with status 201`() {
        val request = CreateBoardGameRequest(
            name = "Azul",
            minPlayers = 2,
            maxPlayers = 4,
            durationMinutes = 45,
            weight = 2,
            description = "Azulejos",
        )
        val created = oneBoardGame(
            name = "Azul",
            minPlayers = 2,
            maxPlayers = 4,
            durationMinutes = 45,
            weight = 2,
            description = "Azulejos",
        )
        every {
            service.insert(match {
                it.name == "Azul" && it.minPlayers == 2 && it.weight == 2
            })
        } returns created

        val response = controller.insert(request)

        response.statusCode shouldBe HttpStatus.CREATED
        response.body shouldBe BoardGameResponse(created)
    }

    @Test
    fun `getById should propagate NotFoundException`() {
        every { service.findById(4L) } throws NotFoundException(4L)

        val exception = shouldThrow<NotFoundException> {
            controller.getById(4L)
        }

        exception.message shouldBe "Not found. id=4"
    }

    @Test
    fun `update should return the updated game`() {
        val request = UpdateBoardGameRequest(
            name = "Wingspan",
            minPlayers = 1,
            maxPlayers = 5,
            durationMinutes = 70,
            weight = 3,
            description = "Aves",
        )
        val updated = oneBoardGame(
            name = "Wingspan",
            minPlayers = 1,
            maxPlayers = 5,
            durationMinutes = 70,
            weight = 3,
            description = "Aves",
        )
        every {
            service.update(1L, match { it.name == "Wingspan" && it.maxPlayers == 5 })
        } returns updated

        val response = controller.update(1L, request)

        response.statusCode shouldBe HttpStatus.OK
        response.body shouldBe BoardGameResponse(updated)
    }

    @Test
    fun `delete should return no content`() {
        every { service.delete(1L) } returns Unit

        val response = controller.delete(1L)

        response.statusCode shouldBe HttpStatus.NO_CONTENT
    }

    @Test
    fun `addTag should return ok when the association is created`() {
        every { service.addTag(1L, 2L) } returns true

        controller.addTag(1L, 2L).statusCode shouldBe HttpStatus.OK
    }

    @Test
    fun `removeTag should return no content when the tag was not associated`() {
        every { service.removeTag(1L, 2L) } returns false

        controller.removeTag(1L, 2L).statusCode shouldBe HttpStatus.NO_CONTENT
    }
}
