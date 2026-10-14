package br.pucpr.authserver.music.responses

import br.pucpr.authserver.music.Track

data class TrackResponse(
    val id: Long,
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val externalId: String?,
) {
    constructor(track: Track) : this(
        id = track.id!!,
        title = track.title,
        artist = track.artist,
        durationSeconds = track.durationSeconds,
        externalId = track.externalId,
    )
}
