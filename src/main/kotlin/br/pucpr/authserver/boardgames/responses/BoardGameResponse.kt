package br.pucpr.authserver.boardgames.responses

import br.pucpr.authserver.boardgames.BoardGame

data class BoardGameResponse(
    val id: Long,
    val name: String,
    val minPlayers: Int,
    val maxPlayers: Int,
    val durationMinutes: Int,
    val weight: Int,
    val description: String,
    val tags: List<TagResponse>,
) {
    constructor(game: BoardGame) : this(
        id = game.id!!,
        name = game.name,
        minPlayers = game.minPlayers,
        maxPlayers = game.maxPlayers,
        durationMinutes = game.durationMinutes,
        weight = game.weight,
        description = game.description,
        tags = game.tags.map { TagResponse(it) }.sortedBy { it.slug },
    )
}
