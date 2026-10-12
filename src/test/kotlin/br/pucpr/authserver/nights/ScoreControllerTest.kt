package br.pucpr.authserver.nights

import br.pucpr.authserver.exception.NotFoundException
import br.pucpr.authserver.nights.requests.CreateScoreRequest
import br.pucpr.authserver.nights.requests.UpdateScoreRequest
import br.pucpr.authserver.nights.responses.ScoreResponse
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

class ScoreControllerTest {
    private val service = mockk<ScoreService>()
    private val controller = ScoreController(service)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        checkUnnecessaryStub(service)
    }

    @Test
    fun `list should return the night scores`() {
        val score = oneScore()
        every { service.list(1L) } returns listOf(score)

        val response = controller.list(1L)

        response.statusCode shouldBe HttpStatus.OK
        response.body shouldBe listOf(ScoreResponse(score))
    }

    @Test
    fun `insert should create and return the score with status 201`() {
        val request = CreateScoreRequest(1L, 1L, placement = 1, bonusPoints = 2, notes = "final")
        val member = oneMember(seasonPoints = 12)
        val score = oneScore(member = member, placement = 1, bonusPoints = 2, notes = "final")
        every { service.insert(1L, request) } returns score

        val response = controller.insert(1L, request)

        response.statusCode shouldBe HttpStatus.CREATED
        response.body shouldBe ScoreResponse(score)
        response.body!!.points shouldBe 12
        response.body!!.seasonPoints shouldBe 12
    }

    @Test
    fun `update should return the updated score`() {
        val request = UpdateScoreRequest(1L, 1L, placement = 11, bonusPoints = 4, notes = null)
        val member = oneMember(seasonPoints = 4)
        val score = oneScore(member = member, placement = 11, bonusPoints = 4)
        every { service.update(1L, 10L, request) } returns score

        val response = controller.update(1L, 10L, request)

        response.statusCode shouldBe HttpStatus.OK
        response.body shouldBe ScoreResponse(score)
        response.body!!.points shouldBe 4
    }

    @Test
    fun `delete should return no content`() {
        every { service.delete(1L, 10L) } returns Unit

        controller.delete(1L, 10L).statusCode shouldBe HttpStatus.NO_CONTENT
    }

    @Test
    fun `list should propagate NotFoundException`() {
        every { service.list(4L) } throws NotFoundException(4L)

        val exception = shouldThrow<NotFoundException> {
            controller.list(4L)
        }

        exception.message shouldBe "Not found. id=4"
    }
}
