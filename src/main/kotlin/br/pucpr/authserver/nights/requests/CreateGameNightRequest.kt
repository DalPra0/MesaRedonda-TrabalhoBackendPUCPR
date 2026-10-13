package br.pucpr.authserver.nights.requests

import br.pucpr.authserver.nights.GameNight
import br.pucpr.authserver.nights.GameNightStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant

data class CreateGameNightRequest(
    @NotBlank
    val title: String?,

    @NotNull
    val scheduledAt: Instant?,

    @NotBlank
    val location: String?,

    val status: GameNightStatus? = null,
) {
    fun toGameNight() = GameNight(
        title = title!!.trim(),
        scheduledAt = scheduledAt!!,
        location = location!!.trim(),
        status = status ?: GameNightStatus.DRAFT,
    )
}
