package br.pucpr.authserver.nights.requests

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class CreateScoreRequest(
    @NotNull
    val memberId: Long?,

    @NotNull
    val boardGameId: Long?,

    @NotNull
    @Min(1)
    val placement: Int?,

    @Min(0)
    val bonusPoints: Int? = null,

    val notes: String? = null,
)
