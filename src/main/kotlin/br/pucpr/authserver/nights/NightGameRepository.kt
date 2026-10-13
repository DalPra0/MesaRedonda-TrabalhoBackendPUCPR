package br.pucpr.authserver.nights

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface NightGameRepository : JpaRepository<NightGame, Long> {
    @Query(
        """
            select ng from NightGame ng
            join fetch ng.game
            where ng.night.id = :nightId
            order by ng.draftOrder asc
        """
    )
    fun findByNightId(nightId: Long): List<NightGame>

    @Query(
        """
            select ng from NightGame ng
            where ng.night.id = :nightId and ng.game.id = :gameId
        """
    )
    fun findByNightIdAndGameId(nightId: Long, gameId: Long): NightGame?

    @Query(
        """
            select coalesce(max(ng.draftOrder), 0) from NightGame ng
            where ng.night.id = :nightId
        """
    )
    fun maxDraftOrder(nightId: Long): Int
}
