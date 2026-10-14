package br.pucpr.authserver.nights.responses

import br.pucpr.authserver.nights.ScoreEntry
import br.pucpr.authserver.nights.scorePoints

data class ScoreResponse(
    val id: Long,
    val nightId: Long,
    val memberId: Long,
    val displayName: String,
    val boardGameId: Long,
    val boardGameName: String,
    val placement: Int,
    val bonusPoints: Int,
    val points: Int,
    val notes: String?,
    val seasonPoints: Int,
) {
    constructor(score: ScoreEntry) : this(
        id = score.id!!,
        nightId = score.gameNight.id!!,
        memberId = score.member.id!!,
        displayName = score.member.displayName,
        boardGameId = score.boardGame.id!!,
        boardGameName = score.boardGame.name,
        placement = score.placement,
        bonusPoints = score.bonusPoints,
        points = scorePoints(score.placement, score.bonusPoints),
        notes = score.notes,
        seasonPoints = score.member.seasonPoints,
    )
}
