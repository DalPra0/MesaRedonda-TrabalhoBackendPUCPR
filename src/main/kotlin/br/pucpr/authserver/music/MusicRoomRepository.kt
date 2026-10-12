package br.pucpr.authserver.music

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MusicRoomRepository : JpaRepository<MusicRoom, Long> {
    @Query(
        """
            select r from MusicRoom r
            join fetch r.gameNight
            where r.gameNight.id = :nightId
        """
    )
    fun findByNightId(nightId: Long): MusicRoom?

    fun findByInviteCode(inviteCode: String): MusicRoom?
}
