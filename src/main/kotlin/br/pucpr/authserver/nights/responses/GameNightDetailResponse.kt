package br.pucpr.authserver.nights.responses

import br.pucpr.authserver.nights.GameNight
import br.pucpr.authserver.nights.GameNightStatus
import br.pucpr.authserver.nights.NightGame
import br.pucpr.authserver.nights.NightRegistration
import java.time.Instant

data class GameNightDetailResponse(
    val id: Long,
    val title: String,
    val scheduledAt: Instant,
    val location: String,
    val status: GameNightStatus,
    val games: List<NightGameResponse>,
    val members: List<NightMemberResponse>,
) {
    constructor(
        night: GameNight,
        games: List<NightGame>,
        members: List<NightRegistration>,
    ) : this(
        id = night.id!!,
        title = night.title,
        scheduledAt = night.scheduledAt,
        location = night.location,
        status = night.status,
        games = games.map { NightGameResponse(it) },
        members = members.map { NightMemberResponse(it) },
    )
}
