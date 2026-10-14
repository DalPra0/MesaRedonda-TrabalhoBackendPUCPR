package br.pucpr.authserver.nights.requests

import br.pucpr.authserver.nights.NightRole

data class RegisterMemberRequest(
    val role: NightRole? = null,
)
