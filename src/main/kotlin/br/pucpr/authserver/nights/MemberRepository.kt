package br.pucpr.authserver.nights

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {
    fun findByDisplayName(displayName: String): Member?
}
