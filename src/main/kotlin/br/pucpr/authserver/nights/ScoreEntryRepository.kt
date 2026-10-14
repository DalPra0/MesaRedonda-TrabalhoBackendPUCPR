package br.pucpr.authserver.nights

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ScoreEntryRepository : JpaRepository<ScoreEntry, Long> {
    @Query(
        """
            select s from ScoreEntry s
            join fetch s.member
            join fetch s.boardGame
            where s.gameNight.id = :nightId
            order by s.boardGame.name asc, s.placement asc, s.member.displayName asc
        """
    )
    fun findByNightId(nightId: Long): List<ScoreEntry>

    @Query(
        """
            select s from ScoreEntry s
            join fetch s.member
            join fetch s.boardGame
            join fetch s.gameNight
            where s.gameNight.id = :nightId and s.id = :scoreId
        """
    )
    fun findByNightIdAndId(nightId: Long, scoreId: Long): ScoreEntry?

    @Query(
        """
            select s from ScoreEntry s
            where s.gameNight.id = :nightId
              and s.member.id = :memberId
              and s.boardGame.id = :boardGameId
        """
    )
    fun findByNightMemberAndGame(nightId: Long, memberId: Long, boardGameId: Long): ScoreEntry?

    @Query(
        """
            select s from ScoreEntry s
            where s.member.id = :memberId
        """
    )
    fun findByMemberId(memberId: Long): List<ScoreEntry>

    @Query(
        """
            select s from ScoreEntry s
            join fetch s.member
            where s.boardGame.id = :boardGameId
        """
    )
    fun findByBoardGameId(boardGameId: Long): List<ScoreEntry>
}
