package br.pucpr.authserver.boardgames

import br.pucpr.authserver.exception.NotFoundException
import br.pucpr.authserver.exceptions.BadRequestException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TagService(
    val repository: TagRepository,
    val boardGameRepository: BoardGameRepository,
) {
    fun findAll() = repository.findAll(Sort.by("slug").ascending())

    fun findById(id: Long) = repository.findByIdOrNull(id) ?: throw NotFoundException(id)

    fun insert(tag: Tag): Tag {
        validate(tag)
        if (repository.findBySlug(tag.slug) != null) {
            throw BadRequestException("Tag already exists")
        }
        val saved = repository.save(tag)
        log.info("Tag ${saved.id} created")
        return saved
    }

    fun update(id: Long, updated: Tag): Tag {
        validate(updated)
        val tag = findById(id)
        val sameSlug = repository.findBySlug(updated.slug)
        if (sameSlug != null && sameSlug.id != id) {
            throw BadRequestException("Tag already exists")
        }
        tag.slug = updated.slug
        tag.label = updated.label
        val saved = repository.save(tag)
        log.info("Tag ${saved.id} updated")
        return saved
    }

    // Soltar a tag dos jogos antes: a tabela BoardGameTag tem chave estrangeira para ela.
    @Transactional
    fun delete(id: Long) {
        val tag = findById(id)
        boardGameRepository.findByTagId(id).forEach { game ->
            game.tags.removeIf { it.id == id }
            boardGameRepository.save(game)
        }
        repository.delete(tag)
        log.warn("Tag $id deleted")
    }

    private fun validate(tag: Tag) {
        if (tag.slug.isBlank() || tag.label.isBlank()) {
            throw BadRequestException("slug and label are required")
        }
    }

    companion object {
        val log = LoggerFactory.getLogger(TagService::class.java)
    }
}
