package br.pucpr.authserver.nights

import br.pucpr.authserver.boardgames.BoardGame
import br.pucpr.authserver.boardgames.BoardGameRepository
import br.pucpr.authserver.exception.NotFoundException
import br.pucpr.authserver.exceptions.BadRequestException
import br.pucpr.authserver.nights.requests.CreateScoreRequest
import br.pucpr.authserver.nights.requests.UpdateScoreRequest
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Placar da noite.
 *
 * Cada linha vale [scorePoints]: `max(0, 11 - placement) + bonusPoints`.
 * Criar, atualizar ou apagar uma linha recalcula [Member.seasonPoints]
 * como a soma dessa conta em todos os placares do membro na temporada.
 */
@Service
class ScoreService(
    val repository: ScoreEntryRepository,
    val gameNightRepository: GameNightRepository,
    val memberRepository: MemberRepository,
    val boardGameRepository: BoardGameRepository,
    val nightGameRepository: NightGameRepository,
    val registrationRepository: NightRegistrationRepository,
) {
    fun list(nightId: Long): List<ScoreEntry> {
        requireNight(nightId)
        return repository.findByNightId(nightId)
    }

    @Transactional
    fun insert(nightId: Long, request: CreateScoreRequest): ScoreEntry {
        val draft = resolve(
            nightId = nightId,
            memberId = request.memberId,
            boardGameId = request.boardGameId,
            placement = request.placement,
            bonusPoints = request.bonusPoints,
            notes = request.notes,
            current = null,
        )
        val saved = repository.save(
            ScoreEntry(
                gameNight = draft.night,
                member = draft.member,
                boardGame = draft.game,
                placement = draft.placement,
                bonusPoints = draft.bonusPoints,
                notes = draft.notes,
            )
        )
        repository.flush()
        recalculate(draft.member)
        log.info("Score ${saved.id} created on game night $nightId")
        return saved
    }

    @Transactional
    fun update(nightId: Long, scoreId: Long, request: UpdateScoreRequest): ScoreEntry {
        val score = requireScore(nightId, scoreId)
        val previousMember = score.member
        val draft = resolve(
            nightId = nightId,
            memberId = request.memberId,
            boardGameId = request.boardGameId,
            placement = request.placement,
            bonusPoints = request.bonusPoints,
            notes = request.notes,
            current = score,
        )
        score.member = draft.member
        score.boardGame = draft.game
        score.placement = draft.placement
        score.bonusPoints = draft.bonusPoints
        score.notes = draft.notes
        val saved = repository.save(score)
        repository.flush()
        recalculate(previousMember)
        if (draft.member.id != previousMember.id) recalculate(draft.member)
        log.info("Score ${saved.id} updated on game night $nightId")
        return saved
    }

    @Transactional
    fun delete(nightId: Long, scoreId: Long) {
        val score = requireScore(nightId, scoreId)
        val member = score.member
        repository.delete(score)
        repository.flush()
        recalculate(member)
        log.warn("Score $scoreId deleted from game night $nightId")
    }

    /**
     * Apaga os placares da noite e recalcula [Member.seasonPoints] de quem pontuou nela.
     * Chamado ao apagar a noite, para o total da temporada não ficar mentindo.
     */
    @Transactional
    fun deleteScoresForNight(nightId: Long) {
        discard(repository.findByNightId(nightId), "Scores of game night $nightId deleted")
    }

    /**
     * Apaga os placares do jogo e recalcula [Member.seasonPoints] de quem pontuou nele.
     * Chamado ao apagar o board game.
     */
    @Transactional
    fun deleteScoresForBoardGame(boardGameId: Long) {
        discard(repository.findByBoardGameId(boardGameId), "Scores of board game $boardGameId deleted")
    }

    private fun discard(scores: List<ScoreEntry>, warning: String) {
        if (scores.isEmpty()) return
        val members = scores.map { it.member }.distinctBy { it.id }
        repository.deleteAll(scores)
        repository.flush()
        members.forEach { recalculate(it) }
        log.warn(warning)
    }

    private fun recalculate(member: Member) {
        val total = repository.findByMemberId(member.id!!).sumOf { scorePoints(it.placement, it.bonusPoints) }
        member.seasonPoints = total
        memberRepository.save(member)
    }

    private fun requireNight(nightId: Long) =
        gameNightRepository.findByIdOrNull(nightId) ?: throw NotFoundException(nightId)

    private fun requireScore(nightId: Long, scoreId: Long): ScoreEntry {
        requireNight(nightId)
        return repository.findByNightIdAndId(nightId, scoreId) ?: throw NotFoundException(scoreId)
    }

    private fun resolve(
        nightId: Long,
        memberId: Long?,
        boardGameId: Long?,
        placement: Int?,
        bonusPoints: Int?,
        notes: String?,
        current: ScoreEntry?,
    ): ScoreDraft {
        val place = placement ?: throw BadRequestException("placement must be at least 1")
        if (place < 1) throw BadRequestException("placement must be at least 1")
        val bonus = bonusPoints ?: 0
        if (bonus < 0) throw BadRequestException("bonusPoints cannot be negative")
        val memberKey = memberId ?: throw BadRequestException("memberId is required")
        val gameKey = boardGameId ?: throw BadRequestException("boardGameId is required")

        val night = requireNight(nightId)
        val member = memberRepository.findByIdOrNull(memberKey)
            ?: throw NotFoundException("Member not found. id=$memberKey")
        if (registrationRepository.findByNightIdAndMemberId(nightId, memberKey) == null) {
            throw BadRequestException("Member is not registered on this night")
        }
        val game = boardGameRepository.findByIdOrNull(gameKey)
            ?: throw NotFoundException("Board game not found. id=$gameKey")
        if (nightGameRepository.findByNightIdAndGameId(nightId, gameKey) == null) {
            throw BadRequestException("Board game is not on this night")
        }
        val duplicate = repository.findByNightMemberAndGame(nightId, memberKey, gameKey)
        if (duplicate != null && duplicate.id != current?.id) {
            throw BadRequestException("Score already exists for this member and board game")
        }
        return ScoreDraft(
            night = night,
            member = member,
            game = game,
            placement = place,
            bonusPoints = bonus,
            notes = notes?.trim()?.ifBlank { null },
        )
    }

    companion object {
        val log = LoggerFactory.getLogger(ScoreService::class.java)
    }
}

private data class ScoreDraft(
    val night: GameNight,
    val member: Member,
    val game: BoardGame,
    val placement: Int,
    val bonusPoints: Int,
    val notes: String?,
)
