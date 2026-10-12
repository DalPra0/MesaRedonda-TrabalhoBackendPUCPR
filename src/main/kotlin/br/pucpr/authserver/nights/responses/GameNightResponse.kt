package br.pucpr.authserver.nights.responses

import br.pucpr.authserver.nights.GameNight
import br.pucpr.authserver.nights.GameNightStatus
import java.time.Instant

data class GameNightResponse(
    val id: Long,
    val title: String,
    val scheduledAt: Instant,
    val location: String,
    val status: GameNightStatus,
) {
    constructor(night: GameNight) : this(
        id = night.id!!,
        title = night.title,
        scheduledAt = night.scheduledAt,
        location = night.location,
        status = night.status,
    )
}
