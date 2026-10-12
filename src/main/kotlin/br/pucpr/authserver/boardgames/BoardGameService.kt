package br.pucpr.authserver.boardgames

import br.pucpr.authserver.exception.NotFoundException
import br.pucpr.authserver.exceptions.BadRequestException
import br.pucpr.authserver.users.SortDir
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class BoardGameService(
    val repository: BoardGameRepository,
    val tagRepository: TagRepository,
) {
    fun insert(game: BoardGame): BoardGame {
        validate(game)
        if (repository.findByNameIgnoreCase(game.name) != null) {
            throw BadRequestException("Board game already exists")
        }
        val saved = repository.save(game)
        log.info("Board game ${saved.id} created")
        return saved
    }

    fun search(
        tag: String?,
        minPlayers: Int?,
        maxPlayers: Int?,
        name: String?,
        sort: String?,
    ): List<BoardGame> {
        if (minPlayers != null && minPlayers < 1) {
            throw BadRequestException("minPlayers must be at least 1")
        }
        if (maxPlayers != null && maxPlayers < 1) {
            throw BadRequestException("maxPlayers must be at least 1")
        }
        return repository.search(
            name = name.blankToNull(),
            tag = tag.blankToNull(),
            minPlayers = minPlayers,
            maxPlayers = maxPlayers,
            sort = parseSort(sort),
        )
    }

    fun findById(id: Long) = repository.findByIdOrNull(id) ?: throw NotFoundException(id)

    fun update(id: Long, updated: BoardGame): BoardGame {
        validate(updated)
        val game = findById(id)
        val sameName = repository.findByNameIgnoreCase(updated.name)
        if (sameName != null && sameName.id != id) {
            throw BadRequestException("Board game already exists")
        }
        game.name = updated.name
        game.minPlayers = updated.minPlayers
        game.maxPlayers = updated.maxPlayers
        game.durationMinutes = updated.durationMinutes
        game.weight = updated.weight
        game.description = updated.description
        val saved = repository.save(game)
        log.info("Board game ${saved.id} updated")
        return saved
    }

    fun delete(id: Long) {
        val game = findById(id)
        game.tags.clear()
        repository.save(game)
        repository.delete(game)
        log.warn("Board game $id deleted")
    }

    fun addTag(gameId: Long, tagId: Long): Boolean {
        val game = findById(gameId)
        val tag = tagRepository.findByIdOrNull(tagId) ?: throw NotFoundException("Tag not found. id=$tagId")
        if (game.tags.any { it.id == tag.id }) return false
        game.tags.add(tag)
        repository.save(game)
        log.info("Tag $tagId added to board game $gameId")
        return true
    }

    fun removeTag(gameId: Long, tagId: Long): Boolean {
        val game = findById(gameId)
        if (tagRepository.findByIdOrNull(tagId) == null) {
            throw NotFoundException("Tag not found. id=$tagId")
        }
        val removed = game.tags.removeIf { it.id == tagId }
        if (!removed) return false
        repository.save(game)
        log.warn("Tag $tagId removed from board game $gameId")
        return true
    }

    private fun validate(game: BoardGame) {
        if (game.name.isBlank()) {
            throw BadRequestException("name is required")
        }
        if (game.minPlayers < 1 || game.maxPlayers < 1 || game.durationMinutes < 1) {
            throw BadRequestException("players and duration must be at least 1")
        }
        if (game.minPlayers > game.maxPlayers) {
            throw BadRequestException("minPlayers cannot be greater than maxPlayers")
        }
        if (game.weight !in 1..5) {
            throw BadRequestException("weight must be between 1 and 5")
        }
    }

    private fun parseSort(sort: String?): Sort {
        val raw = sort.blankToNull() ?: return Sort.by(Sort.Direction.ASC, "name")
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
        private val SORT_FIELDS = setOf("name", "minPlayers", "maxPlayers", "durationMinutes", "weight")
        val log = LoggerFactory.getLogger(BoardGameService::class.java)
    }
}
