package br.pucpr.authserver.nights.responses

import br.pucpr.authserver.nights.Member

data class MemberResponse(
    val id: Long,
    val displayName: String,
    val email: String?,
    val seasonPoints: Int,
) {
    constructor(member: Member) : this(
        member.id!!,
        member.displayName,
        member.email,
        member.seasonPoints,
    )
}
