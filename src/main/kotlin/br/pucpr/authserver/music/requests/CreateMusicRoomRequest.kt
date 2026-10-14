package br.pucpr.authserver.music.requests

data class CreateMusicRoomRequest(
    val inviteCode: String? = null,
    val mood: String? = null,
    val active: Boolean? = null,
)
