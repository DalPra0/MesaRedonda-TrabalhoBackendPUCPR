package br.pucpr.authserver.music.requests

import br.pucpr.authserver.music.Track
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateTrackRequest(
    @NotBlank
    val title: String?,

    @NotBlank
    val artist: String?,

    @NotNull
    @Min(1)
    val durationSeconds: Int?,

    val externalId: String? = null,
) {
    fun toTrack() = Track(
        title = title!!.trim(),
        artist = artist!!.trim(),
        durationSeconds = durationSeconds!!,
        externalId = externalId?.trim()?.ifBlank { null },
    )
}
