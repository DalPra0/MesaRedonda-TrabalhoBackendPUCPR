package br.pucpr.authserver.boardgames.requests

import br.pucpr.authserver.boardgames.BoardGame
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class UpdateBoardGameRequest(
    @NotBlank
    val name: String?,

    @NotNull
    @Min(1)
    val minPlayers: Int?,

    @NotNull
    @Min(1)
    val maxPlayers: Int?,

    @NotNull
    @Min(1)
    val durationMinutes: Int?,

    @NotNull
    @Min(1)
    @Max(5)
    val weight: Int?,

    val description: String? = null,
) {
    fun toBoardGame() = BoardGame(
        name = name!!.trim(),
        minPlayers = minPlayers!!,
        maxPlayers = maxPlayers!!,
        durationMinutes = durationMinutes!!,
        weight = weight!!,
        description = description?.trim().orEmpty(),
    )
}
