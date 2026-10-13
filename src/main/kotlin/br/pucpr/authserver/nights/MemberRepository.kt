package br.pucpr.authserver.nights

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, Long> {
    fun findByDisplayName(displayName: String): Member?
}
