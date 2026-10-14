package br.pucpr.authserver.music.requests

import jakarta.validation.constraints.NotNull

data class EnqueueRequest(
    @NotNull
    val trackId: Long?,

    @NotNull
    val memberId: Long?,
)
