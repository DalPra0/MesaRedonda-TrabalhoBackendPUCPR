package br.pucpr.authserver.music

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface QueueEntryRepository : JpaRepository<QueueEntry, Long> {
    @Query(
        """
            select distinct e from QueueEntry e
            join fetch e.track
            join fetch e.requestedBy
            join fetch e.musicRoom r
            join fetch r.gameNight
            where e.musicRoom.id = :roomId
        """
    )
    fun findDetailed(roomId: Long): List<QueueEntry>

    @Query(
        """
            select distinct e from QueueEntry e
            join fetch e.track
            join fetch e.requestedBy
            join fetch e.musicRoom r
            join fetch r.gameNight
            where e.musicRoom.id = :roomId
              and e.status = :status
        """
    )
    fun findDetailedByStatus(roomId: Long, status: QueueStatus): List<QueueEntry>

    @Query(
        """
            select e from QueueEntry e
            join fetch e.track
            join fetch e.requestedBy
            join fetch e.musicRoom r
            join fetch r.gameNight
            where r.gameNight.id = :nightId and e.id = :entryId
        """
    )
    fun findByNightIdAndId(nightId: Long, entryId: Long): QueueEntry?

    @Query(
        """
            select e from QueueEntry e
            where e.musicRoom.id = :roomId and e.status = :status
        """
    )
    fun findByRoomAndStatus(roomId: Long, status: QueueStatus): List<QueueEntry>

    @Query(
        """
            select e from QueueEntry e
            where e.musicRoom.id = :roomId
        """
    )
    fun findByRoomId(roomId: Long): List<QueueEntry>

    @Query("select max(e.position) from QueueEntry e where e.musicRoom.id = :roomId")
    fun maxPosition(roomId: Long): Long?

    @Query("select count(e) from QueueEntry e where e.track.id = :trackId")
    fun countByTrackId(trackId: Long): Long
}
