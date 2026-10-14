package br.pucpr.authserver.music.responses

import br.pucpr.authserver.music.MusicRoom

data class MusicRoomResponse(
    val id: Long,
    val nightId: Long,
    val inviteCode: String,
    val mood: String?,
    val active: Boolean,
) {
    constructor(room: MusicRoom) : this(
        id = room.id!!,
        nightId = room.gameNight.id!!,
        inviteCode = room.inviteCode,
        mood = room.mood,
        active = room.active,
    )
}
