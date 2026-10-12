package br.pucpr.authserver.boardgames

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Entity
class Tag(
    @Id @GeneratedValue
    var id: Long? = null,

    @Column(unique = true, nullable = false)
    var slug: String,

    @Column(nullable = false)
    var label: String,
)
