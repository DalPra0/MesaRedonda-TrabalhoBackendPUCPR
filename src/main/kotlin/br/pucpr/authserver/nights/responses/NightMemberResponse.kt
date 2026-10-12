package br.pucpr.authserver.nights.responses

import br.pucpr.authserver.nights.NightRegistration
import br.pucpr.authserver.nights.NightRole

data class NightMemberResponse(
    val id: Long,
    val memberId: Long,
    val displayName: String,
    val email: String?,
    val seasonPoints: Int,
    val role: NightRole,
) {
    constructor(registration: NightRegistration) : this(
        id = registration.id!!,
        memberId = registration.member.id!!,
        displayName = registration.member.displayName,
        email = registration.member.email,
        seasonPoints = registration.member.seasonPoints,
        role = registration.role,
    )
}
