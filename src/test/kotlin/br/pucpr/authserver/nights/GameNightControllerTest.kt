package br.pucpr.authserver.nights

import br.pucpr.authserver.exception.NotFoundException
import br.pucpr.authserver.nights.requests.CreateGameNightRequest
import br.pucpr.authserver.nights.requests.RegisterMemberRequest
import br.pucpr.authserver.nights.requests.UpdateGameNightRequest
import br.pucpr.authserver.nights.responses.GameNightDetailResponse
import br.pucpr.authserver.nights.responses.GameNightResponse
import br.pucpr.authserver.nights.responses.NightGameResponse
import br.pucpr.authserver.nights.responses.NightMemberResponse
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
import java.time.Instant

class GameNightControllerTest {
    private val service = mockk<GameNightService>()
    private val controller = GameNightController(service)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        checkUnnecessaryStub(service)
    }

    @Test
    fun `list should return filtered nights`() {
        val night = oneGameNight()
        val from = Instant.parse("2026-10-01T00:00:00Z")
        val to = Instant.parse("2026-10-31T23:59:59Z")
        every { service.search(GameNightStatus.DRAFT, from, to, "scheduledAt,desc") } returns listOf(night)

        val response = controller.list(
            status = GameNightStatus.DRAFT,
            from = from,
            to = to,
            sort = "scheduledAt,desc",
        )

        response.statusCode shouldBe HttpStatus.OK
        response.body shouldBe listOf(GameNightResponse(night))
    }

    @Test
    fun `insert should create and return the night with status 201`() {
        val request = CreateGameNightRequest(
            title = "Liga SI",
            scheduledAt = Instant.parse("2026-10-16T22:00:00Z"),
            location = "Sala 1",
            status = null,
        )
        val created = oneGameNight()
        every {
            service.insert(match { it.title == "Liga SI" && it.status == GameNightStatus.DRAFT })
        } returns created

        val response = controller.insert(request)

        response.statusCode shouldBe HttpStatus.CREATED
        response.body shouldBe GameNightResponse(created)
    }

    @Test
    fun `getById should return games and members`() {
        val night = oneGameNight()
        val nightGame = oneNightGame(night = night)
        val registration = oneRegistration(night = night)
        every { service.findById(1L) } returns night
        every { service.listGames(1L) } returns listOf(nightGame)
        every { service.listMembers(1L) } returns listOf(registration)

        val response = controller.getById(1L)

        response.statusCode shouldBe HttpStatus.OK
        response.body shouldBe GameNightDetailResponse(night, listOf(nightGame), listOf(registration))
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
    fun `update should return the updated night`() {
        val request = UpdateGameNightRequest(
            title = "Rodada 3",
            scheduledAt = Instant.parse("2026-11-01T21:00:00Z"),
            location = "Casa do Lucas",
            status = GameNightStatus.OPEN,
        )
        val updated = oneGameNight(
            title = "Rodada 3",
            scheduledAt = Instant.parse("2026-11-01T21:00:00Z"),
            location = "Casa do Lucas",
            status = GameNightStatus.OPEN,
        )
        every {
            service.update(1L, match { it.title == "Rodada 3" && it.status == GameNightStatus.OPEN })
        } returns updated

        val response = controller.update(1L, request)

        response.statusCode shouldBe HttpStatus.OK
        response.body shouldBe GameNightResponse(updated)
    }

    @Test
    fun `delete should return no content`() {
        every { service.delete(1L) } returns Unit

        controller.delete(1L).statusCode shouldBe HttpStatus.NO_CONTENT
    }

    @Test
    fun `listGames should return the draft`() {
        val nightGame = oneNightGame()
        every { service.listGames(1L) } returns listOf(nightGame)

        val response = controller.listGames(1L)

        response.statusCode shouldBe HttpStatus.OK
        response.body shouldBe listOf(NightGameResponse(nightGame))
    }

    @Test
    fun `addGame should return ok when the association is created`() {
        every { service.addGame(1L, 2L) } returns true

        controller.addGame(1L, 2L).statusCode shouldBe HttpStatus.OK
    }

    @Test
    fun `addGame should return no content when the game is already associated`() {
        every { service.addGame(1L, 2L) } returns false

        controller.addGame(1L, 2L).statusCode shouldBe HttpStatus.NO_CONTENT
    }

    @Test
    fun `removeGame should return ok when the game was on the night`() {
        every { service.removeGame(1L, 2L) } returns true

        controller.removeGame(1L, 2L).statusCode shouldBe HttpStatus.OK
    }

    @Test
    fun `listMembers should return registrations`() {
        val registration = oneRegistration(role = NightRole.HOST)
        every { service.listMembers(1L) } returns listOf(registration)

        val response = controller.listMembers(1L)

        response.statusCode shouldBe HttpStatus.OK
        response.body shouldBe listOf(NightMemberResponse(registration))
    }

    @Test
    fun `addMember should pass the role and return ok`() {
        every { service.addMember(1L, 3L, NightRole.DJ) } returns true

        controller.addMember(1L, 3L, RegisterMemberRequest(NightRole.DJ)).statusCode shouldBe HttpStatus.OK
    }

    @Test
    fun `addMember should default to a null role when the body is missing`() {
        every { service.addMember(1L, 3L, null) } returns true

        controller.addMember(1L, 3L, null).statusCode shouldBe HttpStatus.OK
    }

    @Test
    fun `addMember should return no content when the member is already registered`() {
        every { service.addMember(1L, 3L, NightRole.HOST) } returns false

        controller.addMember(1L, 3L, RegisterMemberRequest(NightRole.HOST)).statusCode shouldBe HttpStatus.NO_CONTENT
    }

    @Test
    fun `removeMember should return no content when the member was not registered`() {
        every { service.removeMember(1L, 3L) } returns false

        controller.removeMember(1L, 3L).statusCode shouldBe HttpStatus.NO_CONTENT
    }

    @Test
    fun `confirmDraft should return the night with the new status`() {
        val night = oneGameNight(status = GameNightStatus.OPEN)
        every { service.confirmDraft(1L) } returns night

        val response = controller.confirmDraft(1L)

        response.statusCode shouldBe HttpStatus.OK
        response.body shouldBe GameNightResponse(night)
    }
}
