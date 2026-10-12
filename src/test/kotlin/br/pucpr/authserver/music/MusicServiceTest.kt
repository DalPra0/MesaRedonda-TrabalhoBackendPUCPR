package br.pucpr.authserver.music

import br.pucpr.authserver.exception.NotFoundException
import br.pucpr.authserver.exceptions.BadRequestException
import br.pucpr.authserver.music.requests.CastVoteRequest
import br.pucpr.authserver.music.requests.CreateMusicRoomRequest
import br.pucpr.authserver.music.requests.EnqueueRequest
import br.pucpr.authserver.nights.GameNightRepository
import br.pucpr.authserver.nights.MemberRepository
import br.pucpr.authserver.nights.oneGameNight
import br.pucpr.authserver.nights.oneMember
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

class MusicServiceTest {
    private val musicRoomRepository = mockk<MusicRoomRepository>()
    private val queueEntryRepository = mockk<QueueEntryRepository>()
    private val voteRepository = mockk<VoteRepository>()
    private val trackRepository = mockk<TrackRepository>()
    private val gameNightRepository = mockk<GameNightRepository>()
    private val memberRepository = mockk<MemberRepository>()
    private val service = MusicService(
        musicRoomRepository,
        queueEntryRepository,
        voteRepository,
        trackRepository,
        gameNightRepository,
        memberRepository,
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        checkUnnecessaryStub(
            musicRoomRepository,
            queueEntryRepository,
            voteRepository,
            trackRepository,
            gameNightRepository,
            memberRepository,
        )
    }

    @Test
    fun `enqueue without a music room asks to create it first`() {
        every { gameNightRepository.findById(1L) } returns Optional.of(oneGameNight())
        every { musicRoomRepository.findByNightId(1L) } returns null

        val exception = shouldThrow<NotFoundException> {
            service.enqueue(1L, EnqueueRequest(trackId = 3L, memberId = 1L))
        }

        exception.message shouldBe "Music room not found. Create the music room before using the queue"
    }

    @Test
    fun `enqueue appends after the highest position`() {
        val room = room()
        val track = track()
        val member = oneMember()
        every { gameNightRepository.findById(1L) } returns Optional.of(room.gameNight)
        every { musicRoomRepository.findByNightId(1L) } returns room
        every { trackRepository.findById(3L) } returns Optional.of(track)
        every { memberRepository.findById(1L) } returns Optional.of(member)
        every { queueEntryRepository.maxPosition(5L) } returns 2L
        every { queueEntryRepository.save(any<QueueEntry>()) } answers {
            firstArg<QueueEntry>().also { it.id = 8L }
        }

        val saved = service.enqueue(1L, EnqueueRequest(trackId = 3L, memberId = 1L))

        saved.position shouldBe 3
        saved.status shouldBe QueueStatus.QUEUED
        saved.track shouldBe track
        saved.requestedBy shouldBe member
    }

    @Test
    fun `play marks the entry PLAYING and the previous one DONE`() {
        val room = room()
        val playing = entry(room, id = 1L, position = 1, status = QueueStatus.PLAYING)
        val queued = entry(room, id = 2L, position = 2, status = QueueStatus.QUEUED)
        every { gameNightRepository.findById(1L) } returns Optional.of(room.gameNight)
        every { musicRoomRepository.findByNightId(1L) } returns room
        every { queueEntryRepository.findByNightIdAndId(1L, 2L) } returns queued
        every { queueEntryRepository.findByRoomAndStatus(5L, QueueStatus.PLAYING) } returns listOf(playing)
        every { queueEntryRepository.save(any<QueueEntry>()) } answers { firstArg() }

        val saved = service.play(1L, 2L)

        saved.status shouldBe QueueStatus.PLAYING
        playing.status shouldBe QueueStatus.DONE
    }

    @Test
    fun `play rejects a removed entry`() {
        val room = room()
        val removed = entry(room, id = 2L, position = 2, status = QueueStatus.REMOVED)
        every { gameNightRepository.findById(1L) } returns Optional.of(room.gameNight)
        every { musicRoomRepository.findByNightId(1L) } returns room
        every { queueEntryRepository.findByNightIdAndId(1L, 2L) } returns removed

        val exception = shouldThrow<BadRequestException> {
            service.play(1L, 2L)
        }

        exception.message shouldBe "Cannot play a removed queue entry"
    }

    @Test
    fun `skip marks the entry SKIPPED`() {
        val room = room()
        val queued = entry(room, id = 2L, position = 2, status = QueueStatus.QUEUED)
        every { gameNightRepository.findById(1L) } returns Optional.of(room.gameNight)
        every { musicRoomRepository.findByNightId(1L) } returns room
        every { queueEntryRepository.findByNightIdAndId(1L, 2L) } returns queued
        every { queueEntryRepository.save(any<QueueEntry>()) } answers { firstArg() }

        service.skip(1L, 2L).status shouldBe QueueStatus.SKIPPED
    }

    @Test
    fun `remove keeps the row and sets REMOVED`() {
        val room = room()
        val queued = entry(room, id = 2L, position = 2, status = QueueStatus.QUEUED)
        every { gameNightRepository.findById(1L) } returns Optional.of(room.gameNight)
        every { musicRoomRepository.findByNightId(1L) } returns room
        every { queueEntryRepository.findByNightIdAndId(1L, 2L) } returns queued
        every { queueEntryRepository.save(queued) } returns queued

        service.remove(1L, 2L)

        queued.status shouldBe QueueStatus.REMOVED
        queued.position shouldBe 2
    }

    @Test
    fun `a second vote from the same member updates the type`() {
        val room = room()
        val queued = entry(room, id = 2L, position = 1, status = QueueStatus.QUEUED)
        val member = queued.requestedBy
        val vote = Vote(id = 9L, queueEntry = queued, member = member, type = VoteType.UP)
        every { gameNightRepository.findById(1L) } returns Optional.of(room.gameNight)
        every { musicRoomRepository.findByNightId(1L) } returns room
        every { queueEntryRepository.findByNightIdAndId(1L, 2L) } returns queued
        every { memberRepository.findById(1L) } returns Optional.of(member)
        every { voteRepository.findByEntryAndMember(2L, 1L) } returns vote
        every { voteRepository.save(vote) } returns vote

        val cast = service.vote(1L, 2L, CastVoteRequest(type = VoteType.DOWN, memberId = 1L))

        cast.created shouldBe false
        cast.vote.type shouldBe VoteType.DOWN
    }

    @Test
    fun `opening an existing room returns it unchanged`() {
        val room = room(mood = "chill")
        every { gameNightRepository.findById(1L) } returns Optional.of(room.gameNight)
        every { musicRoomRepository.findByNightId(1L) } returns room

        val opened = service.open(1L, CreateMusicRoomRequest(inviteCode = "OTHER1", mood = "loud", active = false))

        opened.created shouldBe false
        opened.room.inviteCode shouldBe "PULSE1"
        opened.room.mood shouldBe "chill"
        opened.room.active shouldBe true
        verify(exactly = 0) { musicRoomRepository.save(any()) }
    }

    @Test
    fun `deleteForNight does nothing when the night has no room`() {
        every { musicRoomRepository.findByNightId(4L) } returns null

        service.deleteForNight(4L)
    }

    private fun room(mood: String? = null) = MusicRoom(
        id = 5L,
        gameNight = oneGameNight(),
        inviteCode = "PULSE1",
        mood = mood,
        active = true,
    )

    private fun track() = Track(
        id = 3L,
        title = "Night Drive",
        artist = "Pulse",
        durationSeconds = 180,
        externalId = null,
    )

    private fun entry(
        room: MusicRoom,
        id: Long,
        position: Int,
        status: QueueStatus,
    ) = QueueEntry(
        id = id,
        musicRoom = room,
        track = track(),
        requestedBy = oneMember(),
        position = position,
        status = status,
    )
}
