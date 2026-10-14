package br.pucpr.authserver.nights

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface NightRegistrationRepository : JpaRepository<NightRegistration, Long> {
    @Query(
        """
            select r from NightRegistration r
            join fetch r.member
            where r.night.id = :nightId
            order by r.member.displayName asc
        """
    )
    fun findByNightId(nightId: Long): List<NightRegistration>

    @Query(
        """
            select r from NightRegistration r
            where r.night.id = :nightId and r.member.id = :memberId
        """
    )
    fun findByNightIdAndMemberId(nightId: Long, memberId: Long): NightRegistration?
}
