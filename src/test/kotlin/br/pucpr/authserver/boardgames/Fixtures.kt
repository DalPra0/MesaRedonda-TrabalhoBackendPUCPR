package br.pucpr.authserver.boardgames

fun oneTag(
    id: Long? = 1L,
    slug: String = "party",
    label: String = "Party",
): Tag = Tag(id = id, slug = slug, label = label)

fun oneBoardGame(
    id: Long? = 1L,
    name: String = "Catan",
    minPlayers: Int = 3,
    maxPlayers: Int = 4,
    durationMinutes: Int = 90,
    weight: Int = 2,
    description: String = "Colonos negociam recursos",
    tags: MutableSet<Tag> = mutableSetOf(),
): BoardGame = BoardGame(
    id = id,
    name = name,
    minPlayers = minPlayers,
    maxPlayers = maxPlayers,
    durationMinutes = durationMinutes,
    weight = weight,
    description = description,
    tags = tags,
)
