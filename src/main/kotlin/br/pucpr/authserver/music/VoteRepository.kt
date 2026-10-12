package br.pucpr.authserver.music

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface VoteRepository : JpaRepository<Vote, Long> {
    @Query(
        """
            select v from Vote v
            join fetch v.queueEntry
            join fetch v.member
            where v.queueEntry.id = :entryId and v.member.id = :memberId
        """
    )
    fun findByEntryAndMember(entryId: Long, memberId: Long): Vote?

    @Query(
        """
            select v from Vote v
            join fetch v.queueEntry
            where v.queueEntry.id in :entryIds
        """
    )
    fun findByEntryIds(entryIds: Collection<Long>): List<Vote>

    @Query(
        """
            select v from Vote v
            where v.queueEntry.id in :entryIds
        """
    )
    fun findAllByEntryIds(entryIds: Collection<Long>): List<Vote>
}
