package br.pucpr.authserver.music.requests

import br.pucpr.authserver.music.VoteType
import jakarta.validation.constraints.NotNull

data class CastVoteRequest(
    @NotNull
    val type: VoteType?,

    @NotNull
    val memberId: Long?,
)
