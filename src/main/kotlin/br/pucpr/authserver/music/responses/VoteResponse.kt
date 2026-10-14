package br.pucpr.authserver.music.responses

import br.pucpr.authserver.music.Vote
import br.pucpr.authserver.music.VoteType

data class VoteResponse(
    val id: Long,
    val queueEntryId: Long,
    val memberId: Long,
    val displayName: String,
    val type: VoteType,
) {
    constructor(vote: Vote) : this(
        id = vote.id!!,
        queueEntryId = vote.queueEntry.id!!,
        memberId = vote.member.id!!,
        displayName = vote.member.displayName,
        type = vote.type,
    )
}
