package br.pucpr.authserver.boardgames.requests

import br.pucpr.authserver.boardgames.Tag
import jakarta.validation.constraints.NotBlank

data class TagRequest(
    @NotBlank
    val slug: String?,

    @NotBlank
    val label: String?,
) {
    fun toTag() = Tag(
        slug = slug!!.trim().lowercase(),
        label = label!!.trim(),
    )
}
