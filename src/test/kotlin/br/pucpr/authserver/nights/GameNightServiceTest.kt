package br.pucpr.authserver.nights

import br.pucpr.authserver.boardgames.BoardGameRepository
import br.pucpr.authserver.boardgames.oneBoardGame
import br.pucpr.authserver.exception.NotFoundException
import br.pucpr.authserver.exceptions.BadRequestException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import java.time.Instant
import java.util.Optional

class GameNightServiceTest {
    private val repository = mockk<GameNightRepository>()
    private val nightGameRepository = mockk<NightGameRepository>()
    private val registrationRepository = mockk<NightRegistrationRepository>()
    private val memberRepository = mockk<MemberRepository>()
    private val boardGameRepository = mockk<BoardGameRepository>()
    private val service = GameNightService(
        repository,
        nightGameRepository,
        registrationRepository,
        memberRepository,
        boardGameRepository,
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        checkUnnecessaryStub(
            repository,
            nightGameRepository,
            registrationRepository,
            memberRepository,
            boardGameRepository,
        )
    }

    @Test
    fun `insert should save and return the night`() {
        val night = oneGameNight(id = null)
        val saved = oneGameNight()
        every { repository.save(night) } returns saved

        service.insert(night) shouldBe saved
    }

    @Test
    fun `insert should throw BadRequestException when title is blank`() {
        val exception = shouldThrow<BadRequestException> {
            service.insert(oneGameNight(title = "  "))
        }

        exception.message shouldBe "title is required"
    }

    @Test
    fun `insert should throw BadRequestException when location is blank`() {
        val exception = shouldThrow<BadRequestException> {
            service.insert(oneGameNight(location = ""))
        }

        exception.message shouldBe "location is required"
    }

    @Test
    fun `findById should throw NotFoundException when the night does not exist`() {
        every { repository.findById(9L) } returns Optional.empty()

        val exception = shouldThrow<NotFoundException> {
            service.findById(9L)
        }

        exception.message shouldBe "Not found. id=9"
    }

    @Test
    fun `update should replace fields and save`() {
        val current = oneGameNight()
        val updated = oneGameNight(
            title = "Rodada 3",
            scheduledAt = Instant.parse("2026-11-01T21:00:00Z"),
            location = "Casa do Lucas",
            status = GameNightStatus.OPEN,
        )
        every { repository.findById(1L) } returns Optional.of(current)
        every { repository.save(current) } returns current

        val result = service.update(1L, updated)

        result.title shouldBe "Rodada 3"
        result.scheduledAt shouldBe Instant.parse("2026-11-01T21:00:00Z")
        result.location shouldBe "Casa do Lucas"
        result.status shouldBe GameNightStatus.OPEN
    }

    @Test
    fun `search should default to scheduledAt descending`() {
        val expected = listOf(oneGameNight())
        val sortSlot = slot<Sort>()
        every {
            repository.findAll(any<Specification<GameNight>>(), capture(sortSlot))
        } returns expected

        val result = service.search(status = null, from = null, to = null, sort = null)

        result shouldBe expected
        sortSlot.captured shouldBe Sort.by(Sort.Direction.DESC, "scheduledAt")
    }

    @Test
    fun `search should reject an unknown sort field`() {
        val exception = shouldThrow<BadRequestException> {
            service.search(null, null, null, "price,asc")
        }

        exception.message shouldBe "Invalid sort field"
    }

    @Test
    fun `search should reject from after to`() {
        val exception = shouldThrow<BadRequestException> {
            service.search(
                status = null,
                from = Instant.parse("2026-11-01T00:00:00Z"),
                to = Instant.parse("2026-10-01T00:00:00Z"),
                sort = null,
            )
        }

        exception.message shouldBe "from cannot be after to"
    }

    @Test
    fun `delete should remove games registrations and the night`() {
        val night = oneGameNight()
        val nightGame = oneNightGame(night = night)
        val registration = oneRegistration(night = night)
        every { repository.findById(1L) } returns Optional.of(night)
        every { nightGameRepository.findByNightId(1L) } returns listOf(nightGame)
        every { nightGameRepository.deleteAll(listOf(nightGame)) } returns Unit
        every { registrationRepository.findByNightId(1L) } returns listOf(registration)
        every { registrationRepository.deleteAll(listOf(registration)) } returns Unit
        every { repository.delete(night) } returns Unit

        service.delete(1L)

        verify(exactly = 1) { repository.delete(night) }
    }

    @Test
    fun `addGame should append the next draft order`() {
        val night = oneGameNight()
        val game = oneBoardGame()
        every { repository.findById(1L) } returns Optional.of(night)
        every { boardGameRepository.findById(1L) } returns Optional.of(game)
        every { nightGameRepository.findByNightIdAndGameId(1L, 1L) } returns null
        every { nightGameRepository.maxDraftOrder(1L) } returns 2
        every { nightGameRepository.save(any<NightGame>()) } answers { firstArg() }

        service.addGame(1L, 1L) shouldBe true
        verify {
            nightGameRepository.save(match {
                it.draftOrder == 3 && !it.confirmed && it.game == game
            })
        }
    }

    @Test
    fun `addGame should return false when the game is already on the night`() {
        every { repository.findById(1L) } returns Optional.of(oneGameNight())
        every { boardGameRepository.findById(1L) } returns Optional.of(oneBoardGame())
        every { nightGameRepository.findByNightIdAndGameId(1L, 1L) } returns oneNightGame()

        service.addGame(1L, 1L) shouldBe false
    }

    @Test
    fun `addGame should throw NotFoundException when the board game does not exist`() {
        every { repository.findById(1L) } returns Optional.of(oneGameNight())
        every { boardGameRepository.findById(8L) } returns Optional.empty()

        val exception = shouldThrow<NotFoundException> {
            service.addGame(1L, 8L)
        }

        exception.message shouldBe "Board game not found. id=8"
    }

    @Test
    fun `removeGame should detach the game`() {
        val nightGame = oneNightGame()
        every { repository.findById(1L) } returns Optional.of(oneGameNight())
        every { boardGameRepository.findById(1L) } returns Optional.of(oneBoardGame())
        every { nightGameRepository.findByNightIdAndGameId(1L, 1L) } returns nightGame
        every { nightGameRepository.delete(nightGame) } returns Unit

        service.removeGame(1L, 1L) shouldBe true
    }

    @Test
    fun `removeGame should return false when the game is not on the night`() {
        every { repository.findById(1L) } returns Optional.of(oneGameNight())
        every { boardGameRepository.findById(1L) } returns Optional.of(oneBoardGame())
        every { nightGameRepository.findByNightIdAndGameId(1L, 1L) } returns null

        service.removeGame(1L, 1L) shouldBe false
    }

    @Test
    fun `addMember should default the role to PLAYER`() {
        val night = oneGameNight()
        val member = oneMember()
        every { repository.findById(1L) } returns Optional.of(night)
        every { memberRepository.findById(1L) } returns Optional.of(member)
        every { registrationRepository.findByNightIdAndMemberId(1L, 1L) } returns null
        every { registrationRepository.save(any<NightRegistration>()) } answers { firstArg() }

        service.addMember(1L, 1L, null) shouldBe true
        verify {
            registrationRepository.save(match { it.role == NightRole.PLAYER && it.member == member })
        }
    }

    @Test
    fun `addMember should keep the requested role`() {
        every { repository.findById(1L) } returns Optional.of(oneGameNight())
        every { memberRepository.findById(1L) } returns Optional.of(oneMember())
        every { registrationRepository.findByNightIdAndMemberId(1L, 1L) } returns null
        every { registrationRepository.save(any<NightRegistration>()) } answers { firstArg() }

        service.addMember(1L, 1L, NightRole.HOST) shouldBe true
        verify { registrationRepository.save(match { it.role == NightRole.HOST }) }
    }

    @Test
    fun `addMember should return false when the member is already registered`() {
        every { repository.findById(1L) } returns Optional.of(oneGameNight())
        every { memberRepository.findById(1L) } returns Optional.of(oneMember())
        every { registrationRepository.findByNightIdAndMemberId(1L, 1L) } returns oneRegistration()

        service.addMember(1L, 1L, NightRole.DJ) shouldBe false
    }

    @Test
    fun `addMember should throw NotFoundException when the member does not exist`() {
        every { repository.findById(1L) } returns Optional.of(oneGameNight())
        every { memberRepository.findById(4L) } returns Optional.empty()

        val exception = shouldThrow<NotFoundException> {
            service.addMember(1L, 4L, null)
        }

        exception.message shouldBe "Member not found. id=4"
    }

    @Test
    fun `confirmDraft should mark games confirmed and move DRAFT to OPEN`() {
        val night = oneGameNight(status = GameNightStatus.DRAFT)
        val nightGame = oneNightGame(night = night, confirmed = false)
        every { repository.findById(1L) } returns Optional.of(night)
        every { nightGameRepository.findByNightId(1L) } returns listOf(nightGame)
        every { nightGameRepository.saveAll(listOf(nightGame)) } returns listOf(nightGame)
        every { repository.save(night) } returns night

        val saved = service.confirmDraft(1L)

        nightGame.confirmed shouldBe true
        saved.status shouldBe GameNightStatus.OPEN
    }

    @Test
    fun `confirmDraft should move OPEN to LIVE`() {
        val night = oneGameNight(status = GameNightStatus.OPEN)
        val nightGame = oneNightGame(night = night, confirmed = true)
        every { repository.findById(1L) } returns Optional.of(night)
        every { nightGameRepository.findByNightId(1L) } returns listOf(nightGame)
        every { nightGameRepository.saveAll(listOf(nightGame)) } returns listOf(nightGame)
        every { repository.save(night) } returns night

        service.confirmDraft(1L).status shouldBe GameNightStatus.LIVE
    }

    @Test
    fun `confirmDraft should keep LIVE as LIVE`() {
        val night = oneGameNight(status = GameNightStatus.LIVE)
        every { repository.findById(1L) } returns Optional.of(night)
        every { nightGameRepository.findByNightId(1L) } returns emptyList()
        every { nightGameRepository.saveAll(emptyList<NightGame>()) } returns emptyList()
        every { repository.save(night) } returns night

        service.confirmDraft(1L).status shouldBe GameNightStatus.LIVE
    }

    @Test
    fun `confirmDraft should reject SCORED and CANCELLED nights`() {
        every { repository.findById(1L) } returns Optional.of(oneGameNight(status = GameNightStatus.SCORED))
        shouldThrow<BadRequestException> { service.confirmDraft(1L) }
            .message shouldBe "Cannot confirm a night with status SCORED"

        every { repository.findById(1L) } returns Optional.of(oneGameNight(status = GameNightStatus.CANCELLED))
        shouldThrow<BadRequestException> { service.confirmDraft(1L) }
            .message shouldBe "Cannot confirm a night with status CANCELLED"
    }
}
