package br.pucpr.authserver.boardgames

import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BoardGameRepository : JpaRepository<BoardGame, Long> {
    fun findByNameIgnoreCase(name: String): BoardGame?

    @Query(
        """
            select g from BoardGame g
            where (:name is null or lower(g.name) like lower(concat('%', :name, '%')))
              and (:minPlayers is null or g.minPlayers >= :minPlayers)
              and (:maxPlayers is null or g.maxPlayers <= :maxPlayers)
              and (
                    :tag is null or exists (
                        select t.id from Tag t
                        where t member of g.tags
                          and lower(t.slug) = lower(:tag)
                    )
                  )
        """
    )
    fun search(
        name: String?,
        tag: String?,
        minPlayers: Int?,
        maxPlayers: Int?,
        sort: Sort,
    ): List<BoardGame>
}
