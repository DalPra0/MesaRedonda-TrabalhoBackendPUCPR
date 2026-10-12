package br.pucpr.authserver.nights

import br.pucpr.authserver.boardgames.BoardGameRepository
import br.pucpr.authserver.boardgames.oneBoardGame
import br.pucpr.authserver.exception.NotFoundException
import br.pucpr.authserver.exceptions.BadRequestException
import br.pucpr.authserver.nights.requests.CreateScoreRequest
import br.pucpr.authserver.nights.requests.UpdateScoreRequest
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
import java.util.Optional

class ScoreServiceTest {
    private val repository = mockk<ScoreEntryRepository>()
    private val gameNightRepository = mockk<GameNightRepository>()
    private val memberRepository = mockk<MemberRepository>()
    private val boardGameRepository = mockk<BoardGameRepository>()
    private val nightGameRepository = mockk<NightGameRepository>()
    private val registrationRepository = mockk<NightRegistrationRepository>()
    private val service = ScoreService(
        repository,
        gameNightRepository,
        memberRepository,
        boardGameRepository,
        nightGameRepository,
        registrationRepository,
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        checkUnnecessaryStub(
            repository,
            gameNightRepository,
            memberRepository,
            boardGameRepository,
            nightGameRepository,
            registrationRepository,
        )
    }

    @Test
    fun `score points are max of zero of 11 minus placement plus bonus`() {
        scorePoints(1, 0) shouldBe 10
        scorePoints(2, 0) shouldBe 9
        scorePoints(10, 0) shouldBe 1
        scorePoints(11, 0) shouldBe 0
        scorePoints(12, 4) shouldBe 4
        scorePoints(1, 3) shouldBe 13
    }

    @Test
    fun `insert should save the line and set season points to the season sum`() {
        val night = oneGameNight()
        val member = oneMember()
        val game = oneBoardGame()
        stubLinks(night, member, game)
        var persisted: ScoreEntry? = null
        every { repository.save(any<ScoreEntry>()) } answers {
            val score = firstArg<ScoreEntry>()
            score.id = 10L
            persisted = score
            score
        }
        every { repository.flush() } returns Unit
        every { repository.findByMemberId(1L) } answers { listOf(persisted!!) }
        every { memberRepository.save(member) } returns member

        val saved = service.insert(1L, CreateScoreRequest(1L, 1L, placement = 1, bonusPoints = 2, notes = "  mesa  "))

        saved.id shouldBe 10L
        saved.placement shouldBe 1
        saved.bonusPoints shouldBe 2
        saved.notes shouldBe "mesa"
        member.seasonPoints shouldBe 12
    }

    @Test
    fun `insert should reject a placement below 1`() {
        val exception = shouldThrow<BadRequestException> {
            service.insert(1L, CreateScoreRequest(1L, 1L, placement = 0, bonusPoints = 0, notes = null))
        }

        exception.message shouldBe "placement must be at least 1"
    }

    @Test
    fun `insert should reject a missing placement`() {
        val exception = shouldThrow<BadRequestException> {
            service.insert(1L, CreateScoreRequest(1L, 1L, placement = null, bonusPoints = 0, notes = null))
        }

        exception.message shouldBe "placement must be at least 1"
    }

    @Test
    fun `insert should reject a negative bonus`() {
        val exception = shouldThrow<BadRequestException> {
            service.insert(1L, CreateScoreRequest(1L, 1L, placement = 1, bonusPoints = -1, notes = null))
        }

        exception.message shouldBe "bonusPoints cannot be negative"
    }

    @Test
    fun `insert should throw NotFoundException when the night does not exist`() {
        every { gameNightRepository.findById(9L) } returns Optional.empty()

        val exception = shouldThrow<NotFoundException> {
            service.insert(9L, CreateScoreRequest(1L, 1L, placement = 1, bonusPoints = 0, notes = null))
        }

        exception.message shouldBe "Not found. id=9"
    }

    @Test
    fun `insert should throw NotFoundException when the member does not exist`() {
        every { gameNightRepository.findById(1L) } returns Optional.of(oneGameNight())
        every { memberRepository.findById(4L) } returns Optional.empty()

        val exception = shouldThrow<NotFoundException> {
            service.insert(1L, CreateScoreRequest(4L, 1L, placement = 1, bonusPoints = 0, notes = null))
        }

        exception.message shouldBe "Member not found. id=4"
    }

    @Test
    fun `insert should reject a member who is not on the night`() {
        val night = oneGameNight()
        val member = oneMember()
        every { gameNightRepository.findById(1L) } returns Optional.of(night)
        every { memberRepository.findById(1L) } returns Optional.of(member)
        every { registrationRepository.findByNightIdAndMemberId(1L, 1L) } returns null

        val exception = shouldThrow<BadRequestException> {
            service.insert(1L, CreateScoreRequest(1L, 1L, placement = 1, bonusPoints = 0, notes = null))
        }

        exception.message shouldBe "Member is not registered on this night"
    }

    @Test
    fun `insert should throw NotFoundException when the board game does not exist`() {
        val night = oneGameNight()
        val member = oneMember()
        every { gameNightRepository.findById(1L) } returns Optional.of(night)
        every { memberRepository.findById(1L) } returns Optional.of(member)
        every { registrationRepository.findByNightIdAndMemberId(1L, 1L) } returns oneRegistration(night = night, member = member)
        every { boardGameRepository.findById(8L) } returns Optional.empty()

        val exception = shouldThrow<NotFoundException> {
            service.insert(1L, CreateScoreRequest(1L, 8L, placement = 1, bonusPoints = 0, notes = null))
        }

        exception.message shouldBe "Board game not found. id=8"
    }

    @Test
    fun `insert should reject a board game that is not on the night`() {
        val night = oneGameNight()
        val member = oneMember()
        val game = oneBoardGame()
        every { gameNightRepository.findById(1L) } returns Optional.of(night)
        every { memberRepository.findById(1L) } returns Optional.of(member)
        every { registrationRepository.findByNightIdAndMemberId(1L, 1L) } returns oneRegistration(night = night, member = member)
        every { boardGameRepository.findById(1L) } returns Optional.of(game)
        every { nightGameRepository.findByNightIdAndGameId(1L, 1L) } returns null

        val exception = shouldThrow<BadRequestException> {
            service.insert(1L, CreateScoreRequest(1L, 1L, placement = 1, bonusPoints = 0, notes = null))
        }

        exception.message shouldBe "Board game is not on this night"
    }

    @Test
    fun `insert should reject a duplicate member and game on the same night`() {
        val night = oneGameNight()
        val member = oneMember()
        val game = oneBoardGame()
        stubLinks(night, member, game, duplicate = oneScore(night = night, member = member, game = game))

        val exception = shouldThrow<BadRequestException> {
            service.insert(1L, CreateScoreRequest(1L, 1L, placement = 2, bonusPoints = 0, notes = null))
        }

        exception.message shouldBe "Score already exists for this member and board game"
    }

    @Test
    fun `update should recalculate the same member from the new placement`() {
        val member = oneMember(seasonPoints = 10)
        val score = oneScore(member = member, placement = 1, bonusPoints = 0)
        stubLinks(score.gameNight, member, score.boardGame, duplicate = score)
        every { repository.findByNightIdAndId(1L, 10L) } returns score
        every { repository.save(score) } returns score
        every { repository.flush() } returns Unit
        every { repository.findByMemberId(1L) } answers { listOf(score) }
        every { memberRepository.save(member) } returns member

        service.update(1L, 10L, UpdateScoreRequest(1L, 1L, placement = 11, bonusPoints = 4, notes = "  "))

        score.placement shouldBe 11
        score.bonusPoints shouldBe 4
        score.notes shouldBe null
        member.seasonPoints shouldBe 4
    }

    @Test
    fun `update should move the score and recalculate both members`() {
        val night = oneGameNight()
        val lucas = oneMember(id = 1L, displayName = "Lucas", seasonPoints = 10)
        val ana = oneMember(id = 2L, displayName = "Ana", seasonPoints = 0)
        val game = oneBoardGame()
        val score = oneScore(night = night, member = lucas, game = game, placement = 1, bonusPoints = 0)
        every { gameNightRepository.findById(1L) } returns Optional.of(night)
        every { repository.findByNightIdAndId(1L, 10L) } returns score
        every { memberRepository.findById(2L) } returns Optional.of(ana)
        every { registrationRepository.findByNightIdAndMemberId(1L, 2L) } returns oneRegistration(night = night, member = ana)
        every { boardGameRepository.findById(1L) } returns Optional.of(game)
        every { nightGameRepository.findByNightIdAndGameId(1L, 1L) } returns oneNightGame(night = night, game = game)
        every { repository.findByNightMemberAndGame(1L, 2L, 1L) } returns null
        every { repository.save(score) } returns score
        every { repository.flush() } returns Unit
        every { repository.findByMemberId(1L) } returns emptyList()
        every { repository.findByMemberId(2L) } answers { listOf(score) }
        every { memberRepository.save(any<Member>()) } answers { firstArg() }

        val updated = service.update(1L, 10L, UpdateScoreRequest(2L, 1L, placement = 1, bonusPoints = 0, notes = null))

        updated.member shouldBe ana
        lucas.seasonPoints shouldBe 0
        ana.seasonPoints shouldBe 10
    }

    @Test
    fun `delete should remove the line and zero season points when nothing remains`() {
        val member = oneMember(seasonPoints = 10)
        val score = oneScore(member = member)
        every { gameNightRepository.findById(1L) } returns Optional.of(score.gameNight)
        every { repository.findByNightIdAndId(1L, 10L) } returns score
        every { repository.delete(score) } returns Unit
        every { repository.flush() } returns Unit
        every { repository.findByMemberId(1L) } returns emptyList()
        every { memberRepository.save(member) } returns member

        service.delete(1L, 10L)

        member.seasonPoints shouldBe 0
    }

    @Test
    fun `delete should throw NotFoundException when the score is not on the night`() {
        every { gameNightRepository.findById(1L) } returns Optional.of(oneGameNight())
        every { repository.findByNightIdAndId(1L, 8L) } returns null

        val exception = shouldThrow<NotFoundException> {
            service.delete(1L, 8L)
        }

        exception.message shouldBe "Not found. id=8"
    }

    @Test
    fun `list should return the night scores`() {
        val score = oneScore()
        every { gameNightRepository.findById(1L) } returns Optional.of(oneGameNight())
        every { repository.findByNightId(1L) } returns listOf(score)

        service.list(1L) shouldBe listOf(score)
    }

    @Test
    fun `list should throw NotFoundException when the night does not exist`() {
        every { gameNightRepository.findById(4L) } returns Optional.empty()

        val exception = shouldThrow<NotFoundException> {
            service.list(4L)
        }

        exception.message shouldBe "Not found. id=4"
    }

    @Test
    fun `deleteScoresForNight should keep points earned on other nights`() {
        val member = oneMember(seasonPoints = 19)
        val gone = oneScore(id = 10L, member = member, placement = 1, bonusPoints = 0)
        val stays = oneScore(
            id = 11L,
            night = oneGameNight(id = 2L),
            member = member,
            placement = 2,
            bonusPoints = 0,
        )
        every { repository.findByNightId(1L) } returns listOf(gone)
        every { repository.deleteAll(listOf(gone)) } returns Unit
        every { repository.flush() } returns Unit
        every { repository.findByMemberId(1L) } returns listOf(stays)
        every { memberRepository.save(member) } returns member

        service.deleteScoresForNight(1L)

        member.seasonPoints shouldBe 9
    }

    @Test
    fun `deleteScoresForNight should do nothing when the night has no scores`() {
        every { repository.findByNightId(3L) } returns emptyList()

        service.deleteScoresForNight(3L)

        verify(exactly = 0) { repository.deleteAll(any<List<ScoreEntry>>()) }
    }

    @Test
    fun `deleteScoresForBoardGame should drop that game and keep the rest of the season`() {
        val member = oneMember(seasonPoints = 14)
        val gone = oneScore(member = member, placement = 1, bonusPoints = 0)
        val stays = oneScore(
            id = 11L,
            game = oneBoardGame(id = 2L, name = "Azul"),
            member = member,
            placement = 12,
            bonusPoints = 4,
        )
        every { repository.findByBoardGameId(1L) } returns listOf(gone)
        every { repository.deleteAll(listOf(gone)) } returns Unit
        every { repository.flush() } returns Unit
        every { repository.findByMemberId(1L) } returns listOf(stays)
        every { memberRepository.save(member) } returns member

        service.deleteScoresForBoardGame(1L)

        member.seasonPoints shouldBe 4
    }

    private fun stubLinks(
        night: GameNight,
        member: Member,
        game: br.pucpr.authserver.boardgames.BoardGame,
        duplicate: ScoreEntry? = null,
    ) {
        every { gameNightRepository.findById(night.id!!) } returns Optional.of(night)
        every { memberRepository.findById(member.id!!) } returns Optional.of(member)
        every { registrationRepository.findByNightIdAndMemberId(night.id!!, member.id!!) } returns
            oneRegistration(night = night, member = member)
        every { boardGameRepository.findById(game.id!!) } returns Optional.of(game)
        every { nightGameRepository.findByNightIdAndGameId(night.id!!, game.id!!) } returns
            oneNightGame(night = night, game = game)
        every { repository.findByNightMemberAndGame(night.id!!, member.id!!, game.id!!) } returns duplicate
    }
}
