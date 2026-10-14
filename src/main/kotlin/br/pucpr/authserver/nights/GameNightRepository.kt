package br.pucpr.authserver.nights

import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface GameNightRepository : JpaRepository<GameNight, Long>, JpaSpecificationExecutor<GameNight>

fun gameNightSearch(
    status: GameNightStatus?,
    from: Instant?,
    to: Instant?,
): Specification<GameNight> = Specification { root, _, cb ->
    val predicates = mutableListOf<Predicate>()
    if (status != null) {
        predicates += cb.equal(root.get<GameNightStatus>("status"), status)
    }
    if (from != null) {
        predicates += cb.greaterThanOrEqualTo(root.get("scheduledAt"), from)
    }
    if (to != null) {
        predicates += cb.lessThanOrEqualTo(root.get("scheduledAt"), to)
    }
    if (predicates.isEmpty()) cb.conjunction() else cb.and(*predicates.toTypedArray())
}
