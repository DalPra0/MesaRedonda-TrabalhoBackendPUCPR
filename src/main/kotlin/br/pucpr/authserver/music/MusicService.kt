package br.pucpr.authserver.music

import br.pucpr.authserver.exception.NotFoundException
import br.pucpr.authserver.exceptions.BadRequestException
import br.pucpr.authserver.music.requests.CastVoteRequest
import br.pucpr.authserver.music.requests.CreateMusicRoomRequest
import br.pucpr.authserver.music.requests.EnqueueRequest
import br.pucpr.authserver.nights.GameNightRepository
import br.pucpr.authserver.nights.MemberRepository
import br.pucpr.authserver.users.SortDir
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.random.Random

/**
 * Fila PulseRoom de uma noite.
 *
 * A sala é 1:1 com a noite. Sem sala, enfileirar responde 404 pedindo para criar a music room antes.
 *
 * Posição: a nova entrada entra no fim, `max(position) + 1` de todas as linhas da sala
 * (QUEUED, PLAYING, DONE, SKIPPED e REMOVED). A primeira fica na posição 1.
 * Remover não apaga a linha: o status vira [QueueStatus.REMOVED] e a posição permanece.
 *
 * Tocar marca a entrada como [QueueStatus.PLAYING] e a que estava PLAYING passa a [QueueStatus.DONE].
 * Pular marca [QueueStatus.SKIPPED] e não avança a próxima sozinho.
 * Cada membro tem um voto por entrada; repetir troca o tipo.
 */
@Service
class MusicService(
    val musicRoomRepository: MusicRoomRepository,
    val queueEntryRepository: QueueEntryRepository,
    val voteRepository: VoteRepository,
    val trackRepository: TrackRepository,
    val gameNightRepository: GameNightRepository,
    val memberRepository: MemberRepository,
) {
    /**
     * @return a sala e `true` se ela foi criada agora. `false` devolve a sala que já existia,
     * sem alterar inviteCode, mood ou active.
     */
    @Transactional
    fun open(nightId: Long, request: CreateMusicRoomRequest?): OpenedRoom {
        val night = requireNight(nightId)
        val existing = musicRoomRepository.findByNightId(nightId)
        if (existing != null) {
            log.info("Music room ${existing.id} already exists for game night $nightId")
            return OpenedRoom(existing, created = false)
        }
        val code = request?.inviteCode?.trim()?.ifBlank { null } ?: generateInviteCode()
        if (musicRoomRepository.findByInviteCode(code) != null) {
            throw BadRequestException("inviteCode already exists")
        }
        val saved = musicRoomRepository.save(
            MusicRoom(
                gameNight = night,
                inviteCode = code,
                mood = request?.mood?.trim()?.ifBlank { null },
                active = request?.active ?: true,
            )
        )
        log.info("Music room ${saved.id} created for game night $nightId")
        return OpenedRoom(saved, created = true)
    }

    fun findRoom(nightId: Long): MusicRoom {
        requireNight(nightId)
        return requireRoom(nightId)
    }

    /**
     * Apaga votos, entradas e a sala. HTTP 204.
     */
    @Transactional
    fun delete(nightId: Long) {
        requireNight(nightId)
        val room = requireRoom(nightId)
        discardRoom(room)
        log.warn("Music room ${room.id} deleted from game night $nightId")
    }

    /**
     * Limpa a sala da noite, se existir. Chamado ao apagar a noite,
     * para a chave da sala não impedir o delete.
     */
    @Transactional
    fun deleteForNight(nightId: Long) {
        val room = musicRoomRepository.findByNightId(nightId) ?: return
        discardRoom(room)
        log.warn("Music room ${room.id} deleted with game night $nightId")
    }

    fun list(nightId: Long, status: QueueStatus?, sort: String?): List<QueueEntry> {
        requireNight(nightId)
        val room = requireRoom(nightId)
        val entries = if (status == null) {
            queueEntryRepository.findDetailed(room.id!!)
        } else {
            queueEntryRepository.findDetailedByStatus(room.id!!, status)
        }
        return sortEntries(entries, sort)
    }

    fun votesOf(entryIds: Collection<Long>): Map<Long, List<Vote>> {
        if (entryIds.isEmpty()) return emptyMap()
        return voteRepository.findByEntryIds(entryIds).groupBy { it.queueEntry.id!! }
    }

    @Transactional
    fun enqueue(nightId: Long, request: EnqueueRequest): QueueEntry {
        requireNight(nightId)
        val room = requireRoom(nightId)
        val trackId = request.trackId ?: throw BadRequestException("trackId is required")
        val memberId = request.memberId ?: throw BadRequestException("memberId is required")
        val track = trackRepository.findByIdOrNull(trackId)
            ?: throw NotFoundException("Track not found. id=$trackId")
        val member = memberRepository.findByIdOrNull(memberId)
            ?: throw NotFoundException("Member not found. id=$memberId")
        val position = ((queueEntryRepository.maxPosition(room.id!!) ?: 0L) + 1L).toInt()
        val saved = queueEntryRepository.save(
            QueueEntry(
                musicRoom = room,
                track = track,
                requestedBy = member,
                position = position,
                status = QueueStatus.QUEUED,
            )
        )
        log.info("Queue entry ${saved.id} added at position $position on game night $nightId")
        return saved
    }

    /**
     * Não apaga a linha. O status vira [QueueStatus.REMOVED] e a posição fica.
     */
    @Transactional
    fun remove(nightId: Long, entryId: Long) {
        val entry = requireEntry(nightId, entryId)
        if (entry.status != QueueStatus.REMOVED) {
            entry.status = QueueStatus.REMOVED
            queueEntryRepository.save(entry)
            log.warn("Queue entry $entryId marked REMOVED on game night $nightId")
        }
    }

    @Transactional
    fun play(nightId: Long, entryId: Long): QueueEntry {
        val entry = requireEntry(nightId, entryId)
        if (entry.status == QueueStatus.REMOVED) {
            throw BadRequestException("Cannot play a removed queue entry")
        }
        queueEntryRepository.findByRoomAndStatus(entry.musicRoom.id!!, QueueStatus.PLAYING)
            .filter { it.id != entry.id }
            .forEach {
                it.status = QueueStatus.DONE
                queueEntryRepository.save(it)
            }
        entry.status = QueueStatus.PLAYING
        val saved = queueEntryRepository.save(entry)
        log.info("Queue entry $entryId is now playing on game night $nightId")
        return saved
    }

    @Transactional
    fun skip(nightId: Long, entryId: Long): QueueEntry {
        val entry = requireEntry(nightId, entryId)
        if (entry.status == QueueStatus.REMOVED) {
            throw BadRequestException("Cannot skip a removed queue entry")
        }
        entry.status = QueueStatus.SKIPPED
        val saved = queueEntryRepository.save(entry)
        log.info("Queue entry $entryId skipped on game night $nightId")
        return saved
    }

    /**
     * @return o voto e `true` se a linha foi criada. `false` se o membro já tinha votado
     * e só o tipo foi atualizado.
     */
    @Transactional
    fun vote(nightId: Long, entryId: Long, request: CastVoteRequest): CastVote {
        val entry = requireEntry(nightId, entryId)
        val memberId = request.memberId ?: throw BadRequestException("memberId is required")
        val type = request.type ?: throw BadRequestException("type is required")
        val member = memberRepository.findByIdOrNull(memberId)
            ?: throw NotFoundException("Member not found. id=$memberId")
        val existing = voteRepository.findByEntryAndMember(entry.id!!, memberId)
        if (existing != null) {
            existing.type = type
            val saved = voteRepository.save(existing)
            log.info("Vote ${saved.id} updated to $type on queue entry $entryId")
            return CastVote(saved, created = false)
        }
        val saved = voteRepository.save(
            Vote(
                queueEntry = entry,
                member = member,
                type = type,
            )
        )
        log.info("Vote ${saved.id} created as $type on queue entry $entryId")
        return CastVote(saved, created = true)
    }

    private fun discardRoom(room: MusicRoom) {
        val entries = queueEntryRepository.findByRoomId(room.id!!)
        val entryIds = entries.mapNotNull { it.id }
        if (entryIds.isNotEmpty()) {
            voteRepository.deleteAll(voteRepository.findAllByEntryIds(entryIds))
            voteRepository.flush()
        }
        queueEntryRepository.deleteAll(entries)
        queueEntryRepository.flush()
        musicRoomRepository.delete(room)
        musicRoomRepository.flush()
    }

    private fun requireNight(nightId: Long) =
        gameNightRepository.findByIdOrNull(nightId) ?: throw NotFoundException(nightId)

    private fun requireRoom(nightId: Long) =
        musicRoomRepository.findByNightId(nightId)
            ?: throw NotFoundException("Music room not found. Create the music room before using the queue")

    private fun requireEntry(nightId: Long, entryId: Long): QueueEntry {
        requireNight(nightId)
        requireRoom(nightId)
        return queueEntryRepository.findByNightIdAndId(nightId, entryId) ?: throw NotFoundException(entryId)
    }

    private fun sortEntries(entries: List<QueueEntry>, sort: String?): List<QueueEntry> {
        val spec = parseSort(sort)
        val comparator = when (spec.field) {
            "position" -> compareBy<QueueEntry> { it.position }
            "status" -> compareBy<QueueEntry> { it.status.name }
            else -> throw BadRequestException("Invalid sort field")
        }
        val ordered = if (spec.descending) comparator.reversed() else comparator
        return entries.sortedWith(ordered.thenBy { it.id ?: 0L })
    }

    private fun parseSort(sort: String?): QueueSort {
        val raw = sort?.trim()?.ifBlank { null } ?: return QueueSort("position", descending = false)
        val parts = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty() || parts.size > 2) {
            throw BadRequestException("Invalid sort")
        }
        val field = parts[0]
        if (field !in SORT_FIELDS) {
            throw BadRequestException("Invalid sort field")
        }
        val direction = if (parts.size == 2) SortDir.find(parts[1]) else SortDir.ASC
        return QueueSort(field, descending = direction == SortDir.DESC)
    }

    private fun generateInviteCode(): String {
        repeat(12) {
            val code = buildString(6) {
                repeat(6) { append(INVITE_ALPHABET[random.nextInt(INVITE_ALPHABET.length)]) }
            }
            if (musicRoomRepository.findByInviteCode(code) == null) return code
        }
        throw BadRequestException("Could not generate invite code")
    }

    companion object {
        private val SORT_FIELDS = setOf("position", "status")
        private const val INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private val random = Random.Default
        private val log = LoggerFactory.getLogger(MusicService::class.java)
    }
}

data class OpenedRoom(
    val room: MusicRoom,
    val created: Boolean,
)

data class CastVote(
    val vote: Vote,
    val created: Boolean,
)

private data class QueueSort(
    val field: String,
    val descending: Boolean,
)
