package br.pucpr.authserver.music

import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface TrackRepository : JpaRepository<Track, Long>, JpaSpecificationExecutor<Track>

// Filtro dinâmico: só os parâmetros informados viram condição.
fun trackSearch(artist: String? = null, title: String? = null): Specification<Track> =
    Specification { root, _, cb ->
        val predicates = mutableListOf<Predicate>()
        if (artist != null) {
            predicates += cb.like(cb.lower(root.get("artist")), "%${artist.lowercase()}%")
        }
        if (title != null) {
            predicates += cb.like(cb.lower(root.get("title")), "%${title.lowercase()}%")
        }
        cb.and(*predicates.toTypedArray())
    }
