package br.pucpr.authserver.nights

import br.pucpr.authserver.boardgames.BoardGameRepository
import br.pucpr.authserver.exception.NotFoundException
import br.pucpr.authserver.exceptions.BadRequestException
import br.pucpr.authserver.users.SortDir
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class GameNightService(
    val repository: GameNightRepository,
    val nightGameRepository: NightGameRepository,
    val registrationRepository: NightRegistrationRepository,
    val memberRepository: MemberRepository,
    val boardGameRepository: BoardGameRepository,
<<<<<<< HEAD
    val scoreService: ScoreService,
=======
>>>>>>> origin/main
) {
    fun insert(night: GameNight): GameNight {
        validate(night)
        val saved = repository.save(night)
        log.info("Game night ${saved.id} created")
        return saved
    }

    fun search(
        status: GameNightStatus?,
        from: Instant?,
        to: Instant?,
        sort: String?,
    ): List<GameNight> {
        if (from != null && to != null && from.isAfter(to)) {
            throw BadRequestException("from cannot be after to")
        }
        return repository.findAll(gameNightSearch(status, from, to), parseSort(sort)).toList()
    }

    fun findById(id: Long) = repository.findByIdOrNull(id) ?: throw NotFoundException(id)

    fun update(id: Long, updated: GameNight): GameNight {
        validate(updated)
        val night = findById(id)
        night.title = updated.title
        night.scheduledAt = updated.scheduledAt
        night.location = updated.location
        night.status = updated.status
        val saved = repository.save(night)
        log.info("Game night ${saved.id} updated")
        return saved
    }

    @Transactional
    fun delete(id: Long) {
        val night = findById(id)
<<<<<<< HEAD
        scoreService.deleteScoresForNight(id)
=======
>>>>>>> origin/main
        nightGameRepository.deleteAll(nightGameRepository.findByNightId(id))
        registrationRepository.deleteAll(registrationRepository.findByNightId(id))
        repository.delete(night)
        log.warn("Game night $id deleted")
    }

    fun listGames(nightId: Long): List<NightGame> {
        findById(nightId)
        return nightGameRepository.findByNightId(nightId)
    }

    /**
     * Associa um jogo ao draft.
     * A ordem é o próximo inteiro livre (`max(draftOrder) + 1`).
     *
     * @return true se a associação foi criada (HTTP 200); false se o jogo já estava na noite (HTTP 204).
     */
    @Transactional
    fun addGame(nightId: Long, gameId: Long): Boolean {
        val night = findById(nightId)
        val game = boardGameRepository.findByIdOrNull(gameId)
            ?: throw NotFoundException("Board game not found. id=$gameId")
        if (nightGameRepository.findByNightIdAndGameId(nightId, gameId) != null) return false
        val nextOrder = nightGameRepository.maxDraftOrder(nightId) + 1
        nightGameRepository.save(
            NightGame(
                night = night,
                game = game,
                draftOrder = nextOrder,
                confirmed = false,
            )
        )
        log.info("Board game $gameId added to game night $nightId")
        return true
    }

    @Transactional
    fun removeGame(nightId: Long, gameId: Long): Boolean {
        findById(nightId)
        if (boardGameRepository.findByIdOrNull(gameId) == null) {
            throw NotFoundException("Board game not found. id=$gameId")
        }
        val nightGame = nightGameRepository.findByNightIdAndGameId(nightId, gameId) ?: return false
        nightGameRepository.delete(nightGame)
        log.warn("Board game $gameId removed from game night $nightId")
        return true
    }

    fun listMembers(nightId: Long): List<NightRegistration> {
        findById(nightId)
        return registrationRepository.findByNightId(nightId)
    }

    /**
     * Inscreve um membro na noite.
     * Sem role no body, o papel é [NightRole.PLAYER].
     *
     * @return true se a inscrição foi criada (HTTP 200); false se o membro já estava na noite (HTTP 204).
     * Reinscrever não troca o papel.
     */
    @Transactional
    fun addMember(nightId: Long, memberId: Long, role: NightRole?): Boolean {
        val night = findById(nightId)
        val member = memberRepository.findByIdOrNull(memberId)
            ?: throw NotFoundException("Member not found. id=$memberId")
        if (registrationRepository.findByNightIdAndMemberId(nightId, memberId) != null) return false
        val assigned = role ?: NightRole.PLAYER
        registrationRepository.save(
            NightRegistration(
                night = night,
                member = member,
                role = assigned,
            )
        )
        log.info("Member $memberId joined game night $nightId as $assigned")
        return true
    }

    @Transactional
    fun removeMember(nightId: Long, memberId: Long): Boolean {
        findById(nightId)
        if (memberRepository.findByIdOrNull(memberId) == null) {
            throw NotFoundException("Member not found. id=$memberId")
        }
        val registration = registrationRepository.findByNightIdAndMemberId(nightId, memberId) ?: return false
        registrationRepository.delete(registration)
        log.warn("Member $memberId removed from game night $nightId")
        return true
    }

    /**
     * Confirma o draft da noite.
     *
     * Marca todos os [NightGame] da noite com `confirmed = true`, inclusive quando a noite não tem jogos.
     *
     * Transição de status:
     * - [GameNightStatus.DRAFT] passa a [GameNightStatus.OPEN]
     * - [GameNightStatus.OPEN] passa a [GameNightStatus.LIVE]
     * - [GameNightStatus.LIVE] permanece [GameNightStatus.LIVE] (confirmar de novo não rebaixa a noite)
     * - [GameNightStatus.SCORED] e [GameNightStatus.CANCELLED] são recusados com [BadRequestException]
     */
    @Transactional
    fun confirmDraft(nightId: Long): GameNight {
        val night = findById(nightId)
        val next = nextStatusAfterConfirm(night.status)
        val games = nightGameRepository.findByNightId(nightId)
        games.forEach { it.confirmed = true }
        nightGameRepository.saveAll(games)
        night.status = next
        val saved = repository.save(night)
        log.info("Game night $nightId draft confirmed with status ${saved.status}")
        return saved
    }

    private fun nextStatusAfterConfirm(status: GameNightStatus): GameNightStatus = when (status) {
        GameNightStatus.DRAFT -> GameNightStatus.OPEN
        GameNightStatus.OPEN -> GameNightStatus.LIVE
        GameNightStatus.LIVE -> GameNightStatus.LIVE
        GameNightStatus.SCORED, GameNightStatus.CANCELLED ->
            throw BadRequestException("Cannot confirm a night with status $status")
    }

    private fun validate(night: GameNight) {
        if (night.title.isBlank()) {
            throw BadRequestException("title is required")
        }
        if (night.location.isBlank()) {
            throw BadRequestException("location is required")
        }
    }

    private fun parseSort(sort: String?): Sort {
        val raw = sort.blankToNull() ?: return Sort.by(Sort.Direction.DESC, "scheduledAt")
        val parts = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty() || parts.size > 2) {
            throw BadRequestException("Invalid sort")
        }
        val field = parts[0]
        if (field !in SORT_FIELDS) {
            throw BadRequestException("Invalid sort field")
        }
        val direction = if (parts.size == 2) SortDir.find(parts[1]) else SortDir.ASC
        val springDirection = when (direction) {
            SortDir.ASC -> Sort.Direction.ASC
            SortDir.DESC -> Sort.Direction.DESC
        }
        return Sort.by(springDirection, field)
    }

    private fun String?.blankToNull() = this?.trim()?.ifBlank { null }

    companion object {
        private val SORT_FIELDS = setOf("scheduledAt", "title", "status", "location")
        val log = LoggerFactory.getLogger(GameNightService::class.java)
    }
}
