package br.pucpr.authserver.boardgames.responses

import br.pucpr.authserver.boardgames.Tag

data class TagResponse(
    val id: Long,
    val slug: String,
    val label: String,
) {
    constructor(tag: Tag) : this(tag.id!!, tag.slug, tag.label)
}
