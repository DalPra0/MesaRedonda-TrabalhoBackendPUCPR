package br.pucpr.authserver.music.responses

import br.pucpr.authserver.music.QueueEntry
import br.pucpr.authserver.music.QueueStatus
import br.pucpr.authserver.music.Vote
import br.pucpr.authserver.music.VoteType

data class QueueEntryResponse(
    val id: Long,
    val nightId: Long,
    val musicRoomId: Long,
    val trackId: Long,
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val memberId: Long,
    val displayName: String,
    val position: Int,
    val status: QueueStatus,
    val upVotes: Int,
    val downVotes: Int,
    val skipRequests: Int,
) {
    constructor(entry: QueueEntry, votes: List<Vote> = emptyList()) : this(
        id = entry.id!!,
        nightId = entry.musicRoom.gameNight.id!!,
        musicRoomId = entry.musicRoom.id!!,
        trackId = entry.track.id!!,
        title = entry.track.title,
        artist = entry.track.artist,
        durationSeconds = entry.track.durationSeconds,
        memberId = entry.requestedBy.id!!,
        displayName = entry.requestedBy.displayName,
        position = entry.position,
        status = entry.status,
        upVotes = votes.count { it.type == VoteType.UP },
        downVotes = votes.count { it.type == VoteType.DOWN },
        skipRequests = votes.count { it.type == VoteType.SKIP_REQUEST },
    )
}
