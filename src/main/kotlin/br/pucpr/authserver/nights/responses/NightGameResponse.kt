package br.pucpr.authserver.nights.responses

import br.pucpr.authserver.nights.NightGame

data class NightGameResponse(
    val id: Long,
    val gameId: Long,
    val name: String,
    val draftOrder: Int,
    val confirmed: Boolean,
) {
    constructor(nightGame: NightGame) : this(
        id = nightGame.id!!,
        gameId = nightGame.game.id!!,
        name = nightGame.game.name,
        draftOrder = nightGame.draftOrder,
        confirmed = nightGame.confirmed,
    )
}
