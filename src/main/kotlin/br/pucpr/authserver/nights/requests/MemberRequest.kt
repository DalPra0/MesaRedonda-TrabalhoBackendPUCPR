package br.pucpr.authserver.nights.requests

import br.pucpr.authserver.nights.Member
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

// seasonPoints fica de fora de propósito: é calculado pelo ScoreService.
data class MemberRequest(
    @NotBlank
    val displayName: String?,

    @Email
    val email: String? = null,
) {
    fun toMember() = Member(
        displayName = displayName!!.trim(),
        email = email?.trim()?.ifBlank { null },
    )
}
