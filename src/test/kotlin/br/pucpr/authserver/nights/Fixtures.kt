package br.pucpr.authserver.nights

import br.pucpr.authserver.boardgames.BoardGame
import br.pucpr.authserver.boardgames.oneBoardGame
import java.time.Instant

fun oneMember(
    id: Long? = 1L,
    displayName: String = "Lucas",
    email: String? = "lucas@pucpr.edu.br",
    seasonPoints: Int = 0,
): Member = Member(
    id = id,
    displayName = displayName,
    email = email,
    seasonPoints = seasonPoints,
)

fun oneGameNight(
    id: Long? = 1L,
    title: String = "Liga SI",
    scheduledAt: Instant = Instant.parse("2026-10-16T22:00:00Z"),
    location: String = "Sala 1",
    status: GameNightStatus = GameNightStatus.DRAFT,
): GameNight = GameNight(
    id = id,
    title = title,
    scheduledAt = scheduledAt,
    location = location,
    status = status,
)

fun oneNightGame(
    id: Long? = 1L,
    night: GameNight = oneGameNight(),
    game: BoardGame = oneBoardGame(),
    draftOrder: Int = 1,
    confirmed: Boolean = false,
): NightGame = NightGame(
    id = id,
    night = night,
    game = game,
    draftOrder = draftOrder,
    confirmed = confirmed,
)

fun oneScore(
    id: Long? = 10L,
    night: GameNight = oneGameNight(),
    member: Member = oneMember(),
    game: BoardGame = oneBoardGame(),
    placement: Int = 1,
    bonusPoints: Int = 0,
    notes: String? = null,
): ScoreEntry = ScoreEntry(
    id = id,
    gameNight = night,
    member = member,
    boardGame = game,
    placement = placement,
    bonusPoints = bonusPoints,
    notes = notes,
)
fun oneRegistration(
    id: Long? = 1L,
    night: GameNight = oneGameNight(),
    member: Member = oneMember(),
    role: NightRole = NightRole.PLAYER,
): NightRegistration = NightRegistration(
    id = id,
    night = night,
    member = member,
    role = role,
)
