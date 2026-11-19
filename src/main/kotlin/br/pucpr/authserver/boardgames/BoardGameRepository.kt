package br.pucpr.authserver.boardgames

import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BoardGameRepository : JpaRepository<BoardGame, Long>, JpaSpecificationExecutor<BoardGame> {
    fun findByNameIgnoreCase(name: String): BoardGame?

    @Query("select g from BoardGame g join g.tags t where t.id = :tagId")
    fun findByTagId(tagId: Long): List<BoardGame>
}

/**
 * Filtro dinâmico: cada parâmetro nulo é ignorado, só os informados viram condição (AND).
 * Quem chama passa o resultado para `findAll(spec, sort)`.
 */
fun boardGameSearch(
    name: String? = null,
    tag: String? = null,
    minPlayers: Int? = null,
    maxPlayers: Int? = null,
    maxDuration: Int? = null,
    weight: Int? = null,
): Specification<BoardGame> = Specification { root, _, cb ->
    val predicates = mutableListOf<Predicate>()
    if (name != null) {
        predicates += cb.like(cb.lower(root.get("name")), "%${name.lowercase()}%")
    }
    if (tag != null) {
        val tags = root.join<BoardGame, Tag>("tags")
        predicates += cb.equal(cb.lower(tags.get("slug")), tag.lowercase())
    }
    if (minPlayers != null) {
        predicates += cb.greaterThanOrEqualTo(root.get("minPlayers"), minPlayers)
    }
    if (maxPlayers != null) {
        predicates += cb.lessThanOrEqualTo(root.get("maxPlayers"), maxPlayers)
    }
    if (maxDuration != null) {
        predicates += cb.lessThanOrEqualTo(root.get("durationMinutes"), maxDuration)
    }
    if (weight != null) {
        predicates += cb.equal(root.get<Int>("weight"), weight)
    }
    cb.and(*predicates.toTypedArray())
}
